package com.estatenestora.app.data.remote

import android.os.SystemClock
import android.util.Log
import com.estatenestora.app.data.model.GeocodePlace
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Free, direct-from-device address search & reverse geocoding for the map
 * picker (see MapLocationPickerScreen) — no backend/Telegram hop.
 *
 * Two different free OSM-backed services, each used for what it's actually
 * good at — this split is *why* search accuracy was poor before:
 *
 *  - Search (forward/autocomplete): Photon (https://photon.komoot.io), an
 *    Elasticsearch-based geocoder built specifically for as-you-type search.
 *    It does typo-tolerant, prefix, and POI/business-name matching (e.g.
 *    "starbu" -> "Starbucks") and supports ranking results by proximity to
 *    a given lat/lon. Plain Nominatim search — what this used to call — is a
 *    structured full-text geocoder, not an autocomplete engine: it wants a
 *    fairly complete, well-formed address, has weak-to-no business-name
 *    matching, and doesn't rank by "near me" the way Google's search does.
 *    That mismatch was the actual cause of "doesn't work like Google Maps".
 *  - Reverse (coordinate -> address): also Photon first, since it resolves
 *    to the nearest named place/POI — much closer to what Google Maps shows
 *    when you drop a pin — rather than just the containing street the way
 *    Nominatim's reverse does. Falls back to Nominatim if Photon has
 *    nothing for that exact point (its POI coverage, while good, is thinner
 *    than Nominatim's in some areas).
 *
 * Neither will fully match Google Maps — that's Google's own proprietary
 * business/POI database and per-user ranking, not something any free
 * OSM-based service can replicate — but this is a meaningfully closer,
 * still 100%-free approximation of it than raw Nominatim was.
 *
 * The same per-device rate guard applies to every call, regardless of which
 * of the two providers ends up serving it: neither publishes a hard SLA —
 * Photon in particular documents no fixed rate limit — but both are shared
 * community demo instances, not paid infrastructure, so this stays a
 * polite, bounded caller either way rather than assuming "no published
 * limit" means "no limit".
 */
object GeoSearchClient {
    private const val TAG = "GeoSearchClient"
    private const val PHOTON_BASE_URL = "https://photon.komoot.io"
    private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org"

    // Required by Nominatim's usage policy, and good practice for Photon too
    // — identifies the app + a contact point instead of an anonymous caller.
    private const val USER_AGENT = "Nestora-Android/1.0 (contact: estatenestora@gmail.com)"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * @param biasLat/biasLon Optional "search near here" point — pass the
     * map's current camera position (or last resolved location). Without
     * this, "starbucks" ranks by Photon's global relevance instead of by
     * what's actually near the user (verified directly: the same query
     * without a bias point returned Wisconsin/Colorado/Japan branches ahead
     * of the one 2km away — *this* is most of what makes Google's search
     * feel accurate, not the underlying data source).
     *
     * Fetches a slightly larger pool than [limit] from whichever provider
     * answers, then re-ranks it client-side (see [rerank]) before trimming
     * to [limit] — Photon's own relevance score is text-similarity-first
     * and only loosely distance-weighted, so without this a strong text
     * match on the other side of the world can still outrank a weaker one
     * two streets away.
     */
    suspend fun searchAddress(query: String, biasLat: Double? = null, biasLon: Double? = null, limit: Int = 6): List<GeocodePlace> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isEmpty()) return@withContext emptyList()
            val fetchLimit = (limit * 2).coerceAtLeast(10)

            val photonUrl = buildString {
                append("$PHOTON_BASE_URL/api/?q=${URLEncoder.encode(q, "UTF-8")}&limit=$fetchLimit&lang=en")
                if (biasLat != null && biasLon != null) append("&lat=$biasLat&lon=$biasLon")
            }
            val photonResults = GeoRateGuard.throttled { runRequest(photonUrl) }
                ?.let { runCatching { parsePhotonFeatureCollection(it) }.getOrNull() }
            if (!photonResults.isNullOrEmpty()) {
                return@withContext rerank(dedupe(photonResults), q, biasLat, biasLon).take(limit)
            }

            // Photon came back empty (happens for sparse/unusual queries) —
            // fall back to Nominatim's structured search instead of showing
            // the user nothing.
            val nominatimUrl = "$NOMINATIM_BASE_URL/search?q=${URLEncoder.encode(q, "UTF-8")}&format=json&addressdetails=1&limit=$fetchLimit"
            val body = GeoRateGuard.throttled { runRequest(nominatimUrl) } ?: return@withContext emptyList()
            val nominatimResults = runCatching {
                gson.fromJson(body, Array<NominatimResult>::class.java).map { it.toGeocodePlace() }
            }.getOrElse {
                Log.e(TAG, "[Search] Nominatim fallback parse error", it)
                emptyList()
            }
            rerank(dedupe(nominatimResults), q, biasLat, biasLon).take(limit)
        }

    suspend fun reverseGeocode(lat: Double, lon: Double): GeocodePlace? = withContext(Dispatchers.IO) {
        val photonUrl = "$PHOTON_BASE_URL/reverse?lat=$lat&lon=$lon"
        val fromPhoton = GeoRateGuard.throttled { runRequest(photonUrl) }
            ?.let { runCatching { parsePhotonFeatureCollection(it) }.getOrNull() }
            ?.firstOrNull()
        if (fromPhoton != null) return@withContext fromPhoton

        val nominatimUrl = "$NOMINATIM_BASE_URL/reverse?lat=$lat&lon=$lon&format=json"
        val body = GeoRateGuard.throttled { runRequest(nominatimUrl) } ?: return@withContext null
        runCatching {
            gson.fromJson(body, NominatimResult::class.java).toGeocodePlace(fallbackLat = lat, fallbackLon = lon)
        }.getOrElse {
            Log.e(TAG, "[Reverse] Nominatim fallback parse error", it)
            null
        }
    }

    private fun runRequest(url: String): String? {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Request returned HTTP ${response.code} for $url")
                    return null
                }
                response.body?.string()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Request failed for $url", e)
            null
        }
    }
}

/**
 * Enforces a polite, bounded request rate underneath every call this device
 * makes to either provider — independent of the UI's own debounce timers. A
 * mutex around the "how long since the last request started" check
 * serialises the bookkeeping — not the network call itself — so concurrent
 * callers queue up waiting their turn to *start*, at least [MIN_INTERVAL_MS]
 * apart, rather than racing through if a UI debounce ever fires two calls
 * close together (e.g. search and reverse-geocode triggered almost at once).
 */
private object GeoRateGuard {
    private const val MIN_INTERVAL_MS = 1_100L
    private val mutex = Mutex()
    private var lastRequestAtMs = 0L

    suspend fun <T> throttled(block: () -> T): T {
        mutex.withLock {
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - lastRequestAtMs
            if (lastRequestAtMs != 0L && elapsed < MIN_INTERVAL_MS) {
                delay(MIN_INTERVAL_MS - elapsed)
            }
            lastRequestAtMs = SystemClock.elapsedRealtime()
        }
        return block()
    }
}

// ── Result post-processing: dedupe + relevance/proximity re-rank ───────────
// Both providers return raw results ranked purely by their own internal
// text-relevance score, which is only loosely (Photon) or not at all
// (Nominatim) proximity-aware, and OSM frequently has several near-duplicate
// nodes for the same real place (e.g. a station's platform, entrance, and
// stop all separately tagged a few metres apart). Neither is fixable by
// tuning request parameters alone — verified against Photon's own
// documented location_bias_scale knob, which made results *worse* in
// testing, not better — so this re-ranks what comes back instead of trying
// to coax a better raw order out of either API.

/** Collapses near-identical entries: same name, coordinates within ~11m
 * (4 decimal places) of each other — almost always the same real place
 * tagged multiple times in OSM, not genuinely different options worth
 * showing separately. */
private fun dedupe(results: List<GeocodePlace>): List<GeocodePlace> {
    val seen = mutableSetOf<String>()
    return results.filter { place ->
        val key = "${place.title.trim().lowercase()}|${"%.4f".format(place.latitude)}|${"%.4f".format(place.longitude)}"
        seen.add(key)
    }
}

/** Re-scores each result by how well its title matches the query text,
 * gently penalised by distance from the bias point when one was given, then
 * sorts by that score — ties broken by each provider's own original order.
 * The distance penalty is capped and modest by design: it nudges close
 * matches ahead of equally-good far ones, it does not let "nearby" beat a
 * genuinely stronger text match, since sometimes the user really is
 * searching for somewhere outside the current map view. */
private fun rerank(results: List<GeocodePlace>, query: String, biasLat: Double?, biasLon: Double?): List<GeocodePlace> {
    val q = query.trim().lowercase()

    fun nameScore(title: String): Double {
        val t = title.trim().lowercase()
        return when {
            t == q -> 100.0
            t.startsWith(q) -> 70.0
            t.split(Regex("\\s+")).any { it.startsWith(q) } -> 50.0
            t.contains(q) -> 30.0
            else -> 0.0
        }
    }

    fun combinedScore(place: GeocodePlace): Double {
        val nameComponent = nameScore(place.title)
        val distancePenalty = if (biasLat != null && biasLon != null) {
            val km = haversineKm(biasLat, biasLon, place.latitude, place.longitude)
            minOf(km, 400.0) * 0.15
        } else 0.0
        return nameComponent - distancePenalty
    }

    return results.withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<GeocodePlace>> { combinedScore(it.value) }
                .thenBy { it.index }
        )
        .map { it.value }
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2).let { it * it }
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return earthRadiusKm * c
}

// ── Photon (GeoJSON FeatureCollection) parsing ──────────────────────────────

private data class PhotonFeatureCollection(
    @SerializedName("features") val features: List<PhotonFeature> = emptyList()
)

private data class PhotonFeature(
    @SerializedName("properties") val properties: PhotonProperties = PhotonProperties(),
    @SerializedName("geometry") val geometry: PhotonGeometry? = null
)

private data class PhotonProperties(
    @SerializedName("name") val name: String? = null,
    @SerializedName("housenumber") val housenumber: String? = null,
    @SerializedName("street") val street: String? = null,
    @SerializedName("district") val district: String? = null,
    @SerializedName("locality") val locality: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("county") val county: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("postcode") val postcode: String? = null,
    @SerializedName("country") val country: String? = null
)

private data class PhotonGeometry(
    // GeoJSON order is [lon, lat], not [lat, lon].
    @SerializedName("coordinates") val coordinates: List<Double> = emptyList()
)

private fun parsePhotonFeatureCollection(json: String): List<GeocodePlace> {
    val gson = Gson()
    val collection = gson.fromJson(json, PhotonFeatureCollection::class.java) ?: return emptyList()
    return collection.features.mapNotNull { it.toGeocodePlaceOrNull() }
}

private fun PhotonFeature.toGeocodePlaceOrNull(): GeocodePlace? {
    val coords = geometry?.coordinates
    if (coords == null || coords.size < 2) return null
    val lon = coords[0]
    val lat = coords[1]
    val p = properties

    val streetLine = listOfNotNull(p.housenumber, p.street).joinToString(" ").trim()
    val title = p.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: streetLine.takeIf { it.isNotEmpty() }
        ?: p.city?.trim()
        ?: return null

    val subtitle = listOfNotNull(
        streetLine.takeIf { it.isNotEmpty() && it != title },
        p.district,
        p.locality,
        p.city,
        p.state,
        p.postcode,
        p.country
    )
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != title }
        .distinct()
        .joinToString(", ")
        .ifEmpty { title }

    return GeocodePlace(title = title, subtitle = subtitle, latitude = lat, longitude = lon)
}

// ── Nominatim (fallback) parsing — same key-preference order the backend's
// geocoding_client.go uses, so a picked point reads the same title whichever
// side resolved it. ──

private data class NominatimResult(
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("lat") val lat: String = "",
    @SerializedName("lon") val lon: String = "",
    @SerializedName("address") val address: Map<String, String>? = null
) {
    fun toGeocodePlace(fallbackLat: Double = 0.0, fallbackLon: Double = 0.0): GeocodePlace {
        val (title, subtitle) = splitDisplayName(address, displayName)
        return GeocodePlace(
            title = title,
            subtitle = subtitle,
            latitude = lat.toDoubleOrNull() ?: fallbackLat,
            longitude = lon.toDoubleOrNull() ?: fallbackLon
        )
    }
}

private val AREA_KEYS = listOf(
    "sublocality", "sublocality_level_1", "neighbourhood", "neighborhood",
    "suburb", "city_district", "locality", "quarter", "ward", "residential", "road"
)

private fun splitDisplayName(address: Map<String, String>?, displayName: String): Pair<String, String> {
    val full = displayName.trim()
    val areaTitle = AREA_KEYS.firstNotNullOfOrNull { key -> address?.get(key)?.trim()?.takeIf { it.isNotEmpty() } }

    if (areaTitle.isNullOrEmpty()) {
        val parts = full.split(",", limit = 2)
        val title = parts.getOrElse(0) { full }.trim()
        val subtitle = parts.getOrNull(1)?.trim().orEmpty().ifEmpty { full }
        return title to subtitle
    }

    var subtitle = full
    if (full.startsWith(areaTitle)) {
        subtitle = full.removePrefix(areaTitle).trim().removePrefix(",").trim()
    }
    if (subtitle.isEmpty()) subtitle = full
    return areaTitle to subtitle
}
