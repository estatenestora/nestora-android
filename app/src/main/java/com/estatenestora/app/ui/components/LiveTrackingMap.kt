package com.estatenestora.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.estatenestora.app.R
import com.estatenestora.app.data.remote.RoutingClient
import com.estatenestora.app.ui.theme.NestoraMint
import com.estatenestora.app.ui.theme.NestoraTextMuted
import kotlinx.coroutines.delay
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property.LINE_CAP_ROUND
import org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val SRC_TRAVELER = "nestora-traveler-src"
private const val SRC_DESTINATION = "nestora-destination-src"
private const val SRC_ROUTE = "nestora-route-src"
private const val LAYER_ROUTE = "nestora-route-layer"
private const val LAYER_TRAVELER = "nestora-traveler-layer"
private const val LAYER_DESTINATION = "nestora-destination-layer"
private const val IMG_TRAVELER = "nestora-traveler-icon"
private const val IMG_DESTINATION = "nestora-destination-icon"

/**
 * Live GPS tracking map for home-service bookings, shown once the provider
 * has tapped "GPS Tracking". Before that (travelerLat/Lon == null) this
 * still renders a real map — just the destination avatar pinned, no
 * traveler marker yet — rather than a fake placeholder box, so "static map
 * with pointing" and "live tracking" are the same component, just one state
 * further along.
 *
 * The traveler is always the provider (tagged "P") and the destination is
 * always the customer (tagged "C") — appointment bookings never reach this
 * component at all, they only ever use [StaticDualPositionMap] below.
 */
@Composable
fun LiveTrackingMap(
    travelerLat: Double?,
    travelerLon: Double?,
    destinationLat: Double?,
    destinationLon: Double?,
    travelerHeadline: String,
    lastUpdatedIso: String?,
    modifier: Modifier = Modifier,
    showEtaBadge: Boolean = true,
    showRoute: Boolean = true,
    onRouteLoaded: (RoutingClient.Route?) -> Unit = {}
) {
    val context = LocalContext.current
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var lastFittedDestination by remember { mutableStateOf<String?>(null) }
    var route by remember { mutableStateOf<RoutingClient.Route?>(null) }
    val latestTravelerLat by rememberUpdatedState(travelerLat)
    val latestTravelerLon by rememberUpdatedState(travelerLon)
    val latestOnRouteLoaded by rememberUpdatedState(onRouteLoaded)

    // A single loop survives provider GPS updates. Restarting the request for
    // every fix can repeatedly cancel a slow route call before it completes.
    LaunchedEffect(destinationLat, destinationLon, showRoute) {
        if (!showRoute) {
            route = null
            latestOnRouteLoaded(null)
            return@LaunchedEffect
        }
        while (true) {
            val fromLat = latestTravelerLat
            val fromLon = latestTravelerLon
            if (fromLat != null && fromLon != null && destinationLat != null && destinationLon != null) {
                val nextRoute = RoutingClient.getRoute(fromLat, fromLon, destinationLat, destinationLon)
                route = nextRoute
                latestOnRouteLoaded(nextRoute)
            } else {
                route = null
                latestOnRouteLoaded(null)
            }
            delay(5_000)
        }
    }

    // The detail response can gain destination coordinates after the map has
    // already initialized (the first poll and the address/location write are
    // independent). Keep the customer marker in sync whenever that happens;
    // adding it only in onMapReady loses the marker permanently.
    LaunchedEffect(mapRef, destinationLat, destinationLon) {
        val map = mapRef ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        if (destinationLat != null && destinationLon != null) {
            ensureDestinationMarker(style, context, destinationLat, destinationLon)
            if (travelerLat == null || travelerLon == null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(destinationLat, destinationLon), 13.0))
            }
        }
    }

    LaunchedEffect(mapRef, travelerLat, travelerLon, destinationLat, destinationLon, route) {
        val map = mapRef ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        if (travelerLat != null && travelerLon != null) {
            (style.getSource(SRC_TRAVELER) as? GeoJsonSource)
                ?.setGeoJson(Point.fromLngLat(travelerLon, travelerLat))

            val rVal = route
            val visibleRoutePoints = rVal?.let {
                remainingRoutePath(it, travelerLat, travelerLon, destinationLat, destinationLon)
            }.orEmpty()
            val nextRoutePoint = visibleRoutePoints.firstOrNull {
                distanceMeters(travelerLat, travelerLon, it[0], it[1]) > 2.0
            }
            val bearing = if (nextRoutePoint != null) {
                // The marker must follow the first segment actually drawn on
                // screen, rather than the first segment of an older route.
                calculateBearing(travelerLat, travelerLon, nextRoutePoint[0], nextRoutePoint[1])
            } else if (destinationLat != null && destinationLon != null) {
                calculateBearing(travelerLat, travelerLon, destinationLat, destinationLon)
            } else {
                0f
            }
            // The live marker is a top-down direction arrow whose zero angle
            // already faces north. Do not rotate the old side-profile bike
            // image: a real-world photo becomes head-down/legs-up at 90°.
            style.getLayer(LAYER_TRAVELER)?.setProperties(iconRotate(providerMarkerRotation(bearing)))
        }

        val r = route
        if (r != null && r.pathLatLon.isNotEmpty()) {
            val points = if (travelerLat != null && travelerLon != null) {
                remainingRoutePath(r, travelerLat, travelerLon, destinationLat, destinationLon)
            } else {
                r.pathLatLon
            }.map { Point.fromLngLat(it[1], it[0]) }
            (style.getSource(SRC_ROUTE) as? GeoJsonSource)?.setGeoJson(LineString.fromLngLats(points))
        } else {
            (style.getSource(SRC_ROUTE) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }

        if (travelerLat != null && travelerLon != null) {
            val destination = if (destinationLat != null && destinationLon != null) {
                LatLng(destinationLat, destinationLon)
            } else null
            val destinationKey = destination?.let { "${it.latitude},${it.longitude}" }

            // If the provider arrived before the customer's coordinates, the
            // first pass centers on the provider. Refit once the destination
            // appears so both markers are visible together.
            if (destination != null && destinationKey != null && lastFittedDestination != destinationKey) {
                val bounds = LatLngBounds.Builder()
                    .include(LatLng(travelerLat, travelerLon))
                    .include(destination)
                    .build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 90))
                lastFittedDestination = destinationKey
            } else if (destinationKey == null && lastFittedDestination == null) {
                // No destination — centre on traveler with default zoom
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(travelerLat, travelerLon), 14.0))
            }
        }
    }

    // Both avatars blink always — same gentle icon-opacity tick used by
    // StaticDualPositionMap, so the two map styles read as one family.
    val infinite = rememberInfiniteTransition(label = "live-avatar-blink")
    val blinkAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(700), repeatMode = RepeatMode.Reverse),
        label = "live-avatar-blink-alpha"
    )
    LaunchedEffect(mapRef, blinkAlpha) {
        val style = mapRef?.style ?: return@LaunchedEffect
        style.getLayer(LAYER_TRAVELER)?.setProperties(iconOpacity(blinkAlpha))
        style.getLayer(LAYER_DESTINATION)?.setProperties(iconOpacity(blinkAlpha))
    }

    Box(modifier = modifier) {
        MapLibreView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                mapRef = map
                map.style?.let { style ->
                    style.addImage(IMG_TRAVELER, directionalProviderMarkerBitmap())

                    style.addSource(GeoJsonSource(SRC_ROUTE))
                    style.addLayer(
                        LineLayer(LAYER_ROUTE, SRC_ROUTE).withProperties(
                            lineColor(NestoraMint.toArgbInt()),
                            lineWidth(4f),
                            lineOpacity(0.85f),
                            lineCap(LINE_CAP_ROUND),
                            lineJoin(LINE_JOIN_ROUND)
                        )
                    )

                    // Add the marker immediately when coordinates are already
                    // present; the effect above also handles late-arriving
                    // coordinates from a subsequent detail poll.
                    if (destinationLat != null && destinationLon != null) {
                        ensureDestinationMarker(style, context, destinationLat, destinationLon)
                    }

                    style.addSource(GeoJsonSource(SRC_TRAVELER))
                    style.addLayer(
                        SymbolLayer(LAYER_TRAVELER, SRC_TRAVELER).withProperties(
                            iconImage(IMG_TRAVELER),
                            iconAllowOverlap(true),
                            iconIgnorePlacement(true)
                        )
                    )
                }
            }
        )

        if (travelerLat == null && showEtaBadge) {
            // Only shown when this composable is being used as the "live"
            // view but hasn't received a position yet (e.g. GPS Tracking was
            // just tapped and the first fix hasn't arrived). When it's being
            // used purely as the pre-tracking static preview (showEtaBadge
            // = false), the caller supplies its own copy instead.
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = NestoraMint, modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                    Text("Waiting for live location…", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1A13))
                }
            }
        }

        if (showEtaBadge) {
            EtaBadge(
                headline = travelerHeadline,
                route = route,
                travelerKnown = travelerLat != null,
                destinationKnown = destinationLat != null && destinationLon != null,
                lastUpdatedIso = lastUpdatedIso,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            )
        }
    }
}

/** Adds or updates the customer destination marker without duplicating map
 * sources/layers when a later booking poll supplies the coordinates. */
private fun ensureDestinationMarker(style: Style, context: android.content.Context, lat: Double, lon: Double) {
    val point = Point.fromLngLat(lon, lat)
    val source = style.getSource(SRC_DESTINATION) as? GeoJsonSource
    if (source != null) {
        source.setGeoJson(point)
        return
    }

    style.addImage(IMG_DESTINATION, customMarkerBitmap(context, R.drawable.ic_location_pin))
    style.addSource(GeoJsonSource(SRC_DESTINATION, Feature.fromGeometry(point)))
    style.addLayer(
        SymbolLayer(LAYER_DESTINATION, SRC_DESTINATION).withProperties(
            iconImage(IMG_DESTINATION),
            iconAllowOverlap(true),
            iconIgnorePlacement(true)
        )
    )
}

@Composable
private fun EtaBadge(
    headline: String,
    route: RoutingClient.Route?,
    travelerKnown: Boolean,
    destinationKnown: Boolean,
    lastUpdatedIso: String?,
    modifier: Modifier = Modifier
) {
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    val secondsAgo = remember(lastUpdatedIso, tick) { secondsSinceIso(lastUpdatedIso) }

    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 4.dp, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot()
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        !travelerKnown -> "Waiting for live location…"
                        !destinationKnown -> "Waiting for customer location…"
                        else -> etaText(route)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1A13)
                )
            }
            Text(
                text = if (travelerKnown) "$headline${secondsAgo?.let { " · updated ${it}s ago" } ?: ""}" else headline,
                fontSize = 11.sp,
                color = NestoraTextMuted
            )
        }
    }
}

@Composable
private fun PulsingDot() {
    val infinite = rememberInfiniteTransition(label = "live-dot")
    val alpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
        label = "live-dot-alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(NestoraMint.copy(alpha = alpha))
    )
}

private fun etaText(route: RoutingClient.Route?): String {
    if (route == null) return "Calculating ETA…"
    if (route.isWithinArrivalRange) return "Arrived"
    val minutes = (route.durationSeconds / 60.0).let { if (it < 1) 1 else Math.round(it) }
    val km = route.distanceMeters / 1000.0
    val distanceText = if (km < 1) "${route.distanceMeters.toInt()} m" else "%.1f km".format(km)
    val prefix = if (route.isApproximate) "Estimated: " else ""
    return "${prefix}arriving in ~$minutes min · $distanceText away"
}

private fun secondsSinceIso(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return try {
        val instant = java.time.Instant.parse(iso)
        val diff = java.time.Duration.between(instant, java.time.Instant.now()).seconds
        if (diff < 0) 0 else diff
    } catch (e: Exception) {
        null
    }
}

private fun Color.toArgbInt(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Static dual-position map — appointment bookings never travel, so instead
// of live GPS tracking + OSRM route this just places both parties' fixed
// positions as small avatar markers with a gentle blink, no network calls
// beyond the one map-tile load. Only ever mounted while the composable
// hosting it (BookingDetailScreen) is on screen, so the blink loop and any
// future data refresh naturally stop the moment the user navigates away.
// ─────────────────────────────────────────────────────────────────────────

private const val SRC_CUSTOMER_AVATAR = "nestora-customer-avatar-src"
private const val SRC_PROVIDER_AVATAR = "nestora-provider-avatar-src"
private const val LAYER_CUSTOMER_AVATAR = "nestora-customer-avatar-layer"
private const val LAYER_PROVIDER_AVATAR = "nestora-provider-avatar-layer"
private const val IMG_CUSTOMER_AVATAR = "nestora-customer-avatar-icon"
private const val IMG_PROVIDER_AVATAR = "nestora-provider-avatar-icon"

@Composable
fun StaticDualPositionMap(
    customerLat: Double,
    customerLon: Double,
    providerLat: Double,
    providerLon: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }

    // Gentle blink via the symbol layer's own icon-opacity property — no
    // bitmap regeneration, just a cheap style property tick every 700ms.
    val infinite = rememberInfiniteTransition(label = "avatar-blink")
    val blinkAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(700), repeatMode = RepeatMode.Reverse),
        label = "avatar-blink-alpha"
    )
    LaunchedEffect(mapRef, blinkAlpha) {
        val style = mapRef?.style ?: return@LaunchedEffect
        style.getLayer(LAYER_CUSTOMER_AVATAR)?.setProperties(iconOpacity(blinkAlpha))
        style.getLayer(LAYER_PROVIDER_AVATAR)?.setProperties(iconOpacity(blinkAlpha))
    }

    Box(modifier = modifier) {
        MapLibreView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                mapRef = map
                map.style?.let { style ->
                    style.addImage(IMG_CUSTOMER_AVATAR, customMarkerBitmap(context, R.drawable.ic_location_pin))
                    style.addImage(IMG_PROVIDER_AVATAR, customMarkerBitmap(context, R.drawable.provider_bike_marker))

                    style.addSource(GeoJsonSource(SRC_CUSTOMER_AVATAR, Feature.fromGeometry(Point.fromLngLat(customerLon, customerLat))))
                    style.addLayer(
                        SymbolLayer(LAYER_CUSTOMER_AVATAR, SRC_CUSTOMER_AVATAR).withProperties(
                            iconImage(IMG_CUSTOMER_AVATAR),
                            iconAllowOverlap(true),
                            iconIgnorePlacement(true)
                        )
                    )

                    style.addSource(GeoJsonSource(SRC_PROVIDER_AVATAR, Feature.fromGeometry(Point.fromLngLat(providerLon, providerLat))))
                    style.addLayer(
                        SymbolLayer(LAYER_PROVIDER_AVATAR, SRC_PROVIDER_AVATAR).withProperties(
                            iconImage(IMG_PROVIDER_AVATAR),
                            iconAllowOverlap(true),
                            iconIgnorePlacement(true)
                        )
                    )

                    val bounds = LatLngBounds.Builder()
                        .include(LatLng(customerLat, customerLon))
                        .include(LatLng(providerLat, providerLon))
                        .build()
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 90))
                }
            }
        )
    }
}

private const val NestoraOrangeArgb = 0xFFE68A2E.toInt()
private val NestoraOrange = Color(NestoraOrangeArgb)

/**
 * Shared marker style for both maps above: the real Material "person" glyph
 * (just the body silhouette — no circular backdrop disc), tinted per role,
 * with a small bold "C"/"P" letter underneath so each party is unambiguous
 * at a glance. Used both for the live traveler/destination pair and the
 * appointment static pair, so the two map styles read as one family.
 */
private fun personTagBitmap(context: android.content.Context, colorArgb: Int, tag: String, sizePx: Int = 96): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val iconSize = (sizePx * 0.62f).toInt()
    val left = (sizePx - iconSize) / 2
    val top = (sizePx * 0.06f).toInt()
    ContextCompat.getDrawable(context, R.drawable.ic_person_filled)?.mutate()?.apply {
        setTint(colorArgb)
        setBounds(left, top, left + iconSize, top + iconSize)
        draw(canvas)
    }

    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx * 0.24f
        textAlign = Paint.Align.CENTER
        color = colorArgb
        isFakeBoldText = true
    }
    val tagY = sizePx - sizePx * 0.04f - (tagPaint.descent() + tagPaint.ascent()) / 2f
    canvas.drawText(tag, sizePx / 2f, tagY, tagPaint)

    return bmp
}

private fun customMarkerBitmap(context: android.content.Context, drawableId: Int, sizePx: Int = 120): Bitmap {
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, sizePx, sizePx)
    drawable.draw(canvas)
    return bmp
}

/** A rotation-safe, top-down provider direction marker for the live map. */
private fun directionalProviderMarkerBitmap(sizePx: Int = 96): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val radius = sizePx * 0.42f
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(0, 103, 81) }
    val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }

    canvas.drawCircle(center, center, radius, outline)
    canvas.drawCircle(center, center, radius - sizePx * 0.045f, fill)
    val pointer = android.graphics.Path().apply {
        moveTo(center, sizePx * 0.16f)
        lineTo(sizePx * 0.72f, sizePx * 0.72f)
        lineTo(center, sizePx * 0.60f)
        lineTo(sizePx * 0.28f, sizePx * 0.72f)
        close()
    }
    canvas.drawPath(pointer, arrow)
    return bitmap
}

/** MapLibre rotates clockwise from a north-facing icon. Keep this normalised
 * so a route bearing can never flip the marker through an invalid angle. */
internal fun providerMarkerRotation(bearing: Float): Float =
    ((bearing % 360f) + 360f) % 360f

private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
	// Location.distanceBetween returns [distance, initialBearing, finalBearing].
	// The old one-element array read distance as a rotation angle, so the bike
	// could point away from the green route even when the route itself was right.
	val results = FloatArray(3)
	try {
		android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
	} catch (e: Exception) {
		return 0f
	}
	return (results[1] + 360f) % 360f
}

/**
 * A route fetch may still describe the provider's previous GPS position for
 * a few seconds. Start the rendered line at the latest position and retain
 * only the forward part of the road path. The marker bearing is derived from
 * this same result, which prevents icon/line disagreement during live moves.
 */
private fun remainingRoutePath(
    route: RoutingClient.Route,
    travelerLat: Double,
    travelerLon: Double,
    destinationLat: Double?,
    destinationLon: Double?
): List<DoubleArray> {
    if (route.isApproximate) {
        return buildList {
            add(doubleArrayOf(travelerLat, travelerLon))
            if (destinationLat != null && destinationLon != null) {
                add(doubleArrayOf(destinationLat, destinationLon))
            }
        }
    }

    val closestIndex = route.pathLatLon.indices.minByOrNull { index ->
        val point = route.pathLatLon[index]
        distanceMeters(travelerLat, travelerLon, point[0], point[1])
    } ?: return route.pathLatLon

    return buildList {
        add(doubleArrayOf(travelerLat, travelerLon))
        addAll(route.pathLatLon.drop(closestIndex))
    }
}

private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0].toDouble()
}
