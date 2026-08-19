package com.estatenestora.app.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Free, direct-from-device road-routing for the booking live-tracking map —
 * same "no backend/Telegram hop, free public OSM-ecosystem service, no
 * vendor API key" pattern GeoSearchClient already established for geocoding.
 * OSRM's public demo server: real road geometry + a driving-time estimate,
 * not just a straight-line guess — the difference between "the pin is 2km
 * away" and "arriving in about 6 minutes" that makes a tracking map actually
 * feel live instead of decorative.
 */
object RoutingClient {
    private const val TAG = "RoutingClient"
    private const val OSRM_BASE_URL = "https://router.project-osrm.org"
    private const val USER_AGENT = "Nestora-Android/1.0 (contact: estatenestora@gmail.com)"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    data class Route(
        val distanceMeters: Double,
        val durationSeconds: Double,
        /** [lat, lon] pairs tracing the actual road path, for drawing the route line. */
        val pathLatLon: List<DoubleArray>,
        val isApproximate: Boolean = false,
        /** Direct GPS separation, used only for the 10-metre arrival threshold. */
        val proximityMeters: Double = distanceMeters
    ) {
        val isWithinArrivalRange: Boolean get() = proximityMeters < 10.0
    }

    /**
     * Routes from (fromLat,fromLon) to (toLat,toLon) via OSRM's driving
     * profile — the only profile the public demo server hosts, used here
     * purely as a reasonable local-travel estimate rather than an assertion
     * about the traveler's actual mode of transport. Returns null on any
     * failure (offline, server hiccup, no route found) — callers fall back
     * to a straight-line distance/ETA rather than blocking the map on this.
     */
    suspend fun getRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Route =
        withContext(Dispatchers.IO) {
            val fallback = approximateRoute(fromLat, fromLon, toLat, toLon)
            if (fallback.proximityMeters <= 100.0) return@withContext fallback
            val url = "$OSRM_BASE_URL/route/v1/driving/$fromLon,$fromLat;$toLon,$toLat" +
                "?overview=full&geometries=geojson"
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "OSRM returned HTTP ${response.code}")
                        return@withContext fallback
                    }
                    val body = response.body?.string() ?: return@withContext fallback
                    val parsed = gson.fromJson(body, OsrmResponse::class.java)
                    val route = parsed?.routes?.firstOrNull() ?: return@withContext fallback
                    if (route.distance <= 0.0 || route.duration <= 0.0 || route.geometry.coordinates.size < 2) {
                        return@withContext fallback
                    }
                    Route(
                        distanceMeters = route.distance,
                        durationSeconds = route.duration,
                        pathLatLon = route.geometry.coordinates.map { doubleArrayOf(it[1], it[0]) },
                        isApproximate = false,
                        proximityMeters = straightLineDistanceMeters(fromLat, fromLon, toLat, toLon)
                    )
                }
            } catch (e: Throwable) {
                Log.w(TAG, "OSRM request failed", e)
                fallback
            }
        }

    private fun approximateRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Route {
        val straightLineMeters = straightLineDistanceMeters(fromLat, fromLon, toLat, toLon)

        // Road routers can return a surprisingly long loop for two GPS fixes
        // that are already beside one another (for example, opposite sides of
        // a divided road). For arrival/nearby distances, the GPS separation is
        // the useful and least surprising value.
        if (straightLineMeters <= 100.0) {
            return Route(
                distanceMeters = straightLineMeters,
                durationSeconds = (straightLineMeters / 7.0).coerceAtLeast(1.0),
                pathLatLon = listOf(doubleArrayOf(fromLat, fromLon), doubleArrayOf(toLat, toLon)),
                isApproximate = true,
                proximityMeters = straightLineMeters
            )
        }

        val estimatedRoadMeters = straightLineMeters * 1.25
        val urbanMetersPerSecond = 25_000.0 / 3_600.0
        return Route(
            distanceMeters = estimatedRoadMeters,
            durationSeconds = (estimatedRoadMeters / urbanMetersPerSecond).coerceAtLeast(60.0),
            pathLatLon = listOf(doubleArrayOf(fromLat, fromLon), doubleArrayOf(toLat, toLon)),
            isApproximate = true,
            proximityMeters = straightLineMeters
        )
    }

    private fun straightLineDistanceMeters(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Double {
        val earthRadiusMeters = 6_371_000.0
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val deltaLat = Math.toRadians(toLat - fromLat)
        val deltaLon = Math.toRadians(toLon - fromLon)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        return earthRadiusMeters * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    private data class OsrmResponse(@SerializedName("routes") val routes: List<OsrmRoute> = emptyList())
    private data class OsrmRoute(
        @SerializedName("distance") val distance: Double = 0.0,
        @SerializedName("duration") val duration: Double = 0.0,
        @SerializedName("geometry") val geometry: OsrmGeometry = OsrmGeometry()
    )
    private data class OsrmGeometry(@SerializedName("coordinates") val coordinates: List<List<Double>> = emptyList())
}
