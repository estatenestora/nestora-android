package com.estatenestora.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

// =============================================================================
// MapLibreShared.kt
//
// The MapLibre + free OpenFreeMap vector-tile setup, extracted out of
// MapLocationPickerScreen (its original, single owner) so BookingDetailScreen's
// live tracking map can reuse the exact same map engine and style — one map
// stack for the whole app, not two drifting copies. No API key needed, same
// "free OSM ecosystem, no vendor key" pattern GeoSearchClient already uses
// for geocoding.
// =============================================================================

/** Free vector tile style — shared by every map surface in the app. */
const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun MapLibreView(
    onMapReady: (MapLibreMap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember { MapLibre.getInstance(context) }

    val mapView = rememberMapViewWithLifecycle()

    // Run getMapAsync exactly once inside a LaunchedEffect to prevent recomposition loops.
    // onMapReady fires from the style-loaded callback, not right after setStyle()
    // returns — setStyle() only *starts* the async style fetch/parse, so a caller
    // that adds sources/layers/images (as the live-tracking map does) needs the
    // style to genuinely be ready first. Confirmed live: without this, map.style
    // was still null when onMapReady ran, so those calls silently no-op'd and the
    // map fell back to its default whole-world camera instead of the one we set.
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) {
                onMapReady(map)
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = {}
    )
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    return mapView
}
