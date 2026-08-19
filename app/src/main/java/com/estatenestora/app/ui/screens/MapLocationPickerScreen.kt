package com.estatenestora.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.estatenestora.app.data.model.GeocodePlace
import com.estatenestora.app.ui.components.MapLibreView
import com.estatenestora.app.ui.theme.*
import com.estatenestora.app.util.findActivity
import com.estatenestora.app.util.getCurrentLocation
import com.estatenestora.app.util.hasLocationPermission
import com.estatenestora.app.util.isSystemLocationEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

// Fallback center: Kolkata
private val DEFAULT_CENTER = LatLng(22.5726, 88.3639)
private const val DEFAULT_ZOOM = 14.0

// Photon (unlike Nominatim) handles short prefixes well — verified directly
// ("st" alone returns real matches) — so 2 characters is enough to start
// searching instead of waiting for 3, without flooding the dropdown with
// noise a single character would.
private const val MIN_SEARCH_QUERY_LEN = 2

@Composable
fun MapLocationPickerScreen(
    initialAddress: String?,
    onSearchAddress: suspend (String, Double?, Double?) -> List<GeocodePlace>,
    onReverseGeocode: suspend (Double, Double) -> GeocodePlace?,
    onLocationConfirmed: (String, String, Double?, Double?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }

    fun hasLocationPermission(): Boolean = hasLocationPermission(context)

    // Synchronously check cached location on startup to bypass Bowbazar fallback lag
    val initialCenter = remember {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val cachedLoc = if (locationManager != null && hasLocationPermission()) {
            locationManager.getAllProviders().mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }.maxByOrNull { it.time }
        } else {
            null
        }
        if (cachedLoc != null) LatLng(cachedLoc.latitude, cachedLoc.longitude) else DEFAULT_CENTER
    }

    var searchQuery by remember { mutableStateOf("") }
    var suppressNextSearchEffect by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<GeocodePlace>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    var selectedPlace by remember { mutableStateOf<GeocodePlace?>(null) }
    var isResolvingAddress by remember { mutableStateOf(false) }
    var isLocationOn by remember { mutableStateOf(hasLocationPermission() && isSystemLocationEnabled(context)) }
    var isLocating by remember { mutableStateOf(false) }

    var settledTarget by remember { mutableStateOf<LatLng?>(null) }
    var suppressNextSettleReverseGeocode by remember { mutableStateOf(false) }

    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }

    // Map dragging state to toggle full-screen map view
    var isMapDragging by remember { mutableStateOf(false) }

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showEnableLocationDialog by remember { mutableStateOf(false) }
    var permissionPermanentlyDenied by remember { mutableStateOf(false) }

    // Loop prevention state flags
    var isRequestingPermission by remember { mutableStateOf(false) }
    var hasAskedPermissionInThisVisit by remember { mutableStateOf(false) }
    var userExplicitlyTurnedOff by remember { mutableStateOf(false) }

    fun flyTo(latLng: LatLng, zoom: Double = DEFAULT_ZOOM) {
        mapRef.value?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), 600)
    }

    fun locateMe() {
        if (!hasLocationPermission()) return
        isLocating = true
        scope.launch {
            val loc = getCurrentLocation(context)
            isLocating = false
            if (loc != null) {
                isLocationOn = true
                flyTo(LatLng(loc.latitude, loc.longitude), 16.0)
            } else {
                Toast.makeText(
                    context,
                    "Location temporarily unavailable — drag map or search to select location.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        isRequestingPermission = false
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            permissionPermanentlyDenied = false
            isLocationOn = isSystemLocationEnabled(context)
            if (isSystemLocationEnabled(context)) locateMe() else showEnableLocationDialog = true
        } else {
            isLocationOn = false
            val canAskAgain = activity != null && (
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            permissionPermanentlyDenied = !canAskAgain
            if (!canAskAgain) {
                showPermissionDeniedDialog = true
            } else {
                Toast.makeText(context, "Location permission is needed to auto-detect your position — tap \"Current location\" to try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun requestLocationOn(isExplicit: Boolean = false) {
        when {
            !hasLocationPermission() -> {
                if (permissionPermanentlyDenied) {
                    if (isExplicit) {
                        showPermissionDeniedDialog = true
                    }
                } else {
                    if (isExplicit || !hasAskedPermissionInThisVisit) {
                        hasAskedPermissionInThisVisit = true
                        isRequestingPermission = true
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                }
            }
            !isSystemLocationEnabled(context) -> {
                isLocationOn = false
                if (isExplicit) {
                    showEnableLocationDialog = true
                }
            }
            else -> {
                locateMe()
            }
        }
    }

    // ON_RESUME lifecycle observer to monitor permission changes silently
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPerm = hasLocationPermission()
                val systemEnabled = isSystemLocationEnabled(context)
                
                if (hasPerm && systemEnabled) {
                    isLocationOn = true
                    if (!userExplicitlyTurnedOff) {
                        locateMe()
                    }
                } else {
                    isLocationOn = false
                    if (!userExplicitlyTurnedOff && !isRequestingPermission) {
                        requestLocationOn(isExplicit = false)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Debounced reverse-geocode whenever the pin settles somewhere new
    LaunchedEffect(settledTarget) {
        val target = settledTarget ?: return@LaunchedEffect
        if (suppressNextSettleReverseGeocode) {
            suppressNextSettleReverseGeocode = false
            return@LaunchedEffect
        }
        delay(600)
        isResolvingAddress = true
        val result = onReverseGeocode(target.latitude, target.longitude)
        isResolvingAddress = false
        if (result != null) {
            selectedPlace = result
        }
    }

    // Debounced search-as-you-type
    LaunchedEffect(searchQuery) {
        if (suppressNextSearchEffect) {
            suppressNextSearchEffect = false
            return@LaunchedEffect
        }
        if (searchQuery.trim().length < MIN_SEARCH_QUERY_LEN) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(450)
        // Bias by wherever the pin/camera currently is (falling back to the
        // startup center) so results rank by proximity — the same "near me"
        // behavior that makes Google's search feel accurate, not just a
        // global text match.
        val biasPoint = mapRef.value?.cameraPosition?.target ?: initialCenter
        searchResults = onSearchAddress(searchQuery.trim(), biasPoint.latitude, biasPoint.longitude)
        isSearching = false
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Location Permission Required", fontWeight = FontWeight.Bold) },
            text = { Text("Nestora needs location access to find nearby services. Please grant location permission in Settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        runCatching { context.startActivity(intent) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                ) { Text("Open Settings", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPermissionDeniedDialog = false }) { Text("Not now", color = NestoraTextMuted) }
            }
        )
    }

    if (showEnableLocationDialog) {
        AlertDialog(
            onDismissRequest = { showEnableLocationDialog = false },
            title = { Text("Enable Device Location", fontWeight = FontWeight.Bold) },
            text = { Text("Device location services are required to find services in your area. Please enable Location in your device settings to proceed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEnableLocationDialog = false
                        runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                ) { Text("Open Location Settings", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEnableLocationDialog = false }) { Text("Not now", color = NestoraTextMuted) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ─── MAP LAYER ───
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    showSuggestions = false
                }
        ) {
            MapLibreView(
                onMapReady = { map ->
                    mapRef.value = map
                    map.addOnCameraMoveStartedListener { reason ->
                        isMapDragging = true
                    }
                    map.addOnCameraIdleListener {
                        settledTarget = map.cameraPosition.target
                        isMapDragging = false
                    }
                    map.addOnMapClickListener { latLng ->
                        flyTo(latLng)
                        true
                    }
                    val camera = org.maplibre.android.camera.CameraPosition.Builder()
                        .target(initialCenter)
                        .zoom(DEFAULT_ZOOM)
                        .build()
                    map.cameraPosition = camera
                },
                modifier = Modifier.fillMaxSize()
            )

            // Center Pin Marker (Small Pointer size)
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp) // Smaller shadow circle size
                        .clip(CircleShape)
                        .background(NestoraMint.copy(alpha = 0.25f))
                )
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Map Pin",
                    tint = NestoraError,
                    modifier = Modifier
                        .size(36.dp) // Smaller pin size (from 54.dp to 36.dp)
                        .offset(y = (-14).dp) // Adjusted offset for smaller pin tip
                )
            }

            // Map zoom controls
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MapControlButton(label = "+", contentDescription = "Zoom in") {
                    mapRef.value?.animateCamera(CameraUpdateFactory.zoomIn())
                }
                MapControlButton(label = "−", contentDescription = "Zoom out") {
                    mapRef.value?.animateCamera(CameraUpdateFactory.zoomOut())
                }
            }
        }

        // ─── FULL SCREEN SUGGESTIONS OVERLAY ───
        AnimatedVisibility(
            visible = !isMapDragging && showSuggestions && searchQuery.trim().length >= MIN_SEARCH_QUERY_LEN,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // ─── FLOATING TOP HEADER (Search & Back button) ───
        // Row alignment set to Alignment.Top so back button stays locked at the top
        AnimatedVisibility(
            visible = !isMapDragging,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Back button on white circle with shadow (exact 46.dp size matching search box)
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .clickable { onBack() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0D1A13)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Search field column
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp), // Compact height (46.dp)
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        // Custom Row inside Surface with BasicTextField for pixel-perfect vertical centering and no cutoff
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search an area or address",
                                        color = NestoraTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        showSuggestions = true
                                    },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.sp,
                                        color = Color(0xFF0D1A13)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = NestoraMint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Suggestions list flat layout rendered over the full-screen suggestions overlay
                    if (showSuggestions && searchQuery.trim().length >= MIN_SEARCH_QUERY_LEN) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            if (isSearching) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = NestoraMint, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            } else if (searchResults.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No matches found",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D1A13)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Try typing a different street or neighborhood name",
                                        fontSize = 13.sp,
                                        color = NestoraTextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                searchResults.forEach { place ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedPlace = place
                                                settledTarget = null
                                                suppressNextSettleReverseGeocode = true
                                                suppressNextSearchEffect = true
                                                showSuggestions = false
                                                searchQuery = place.title
                                                flyTo(LatLng(place.latitude, place.longitude), 16.0)
                                            }
                                            .padding(vertical = 14.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = NestoraMint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(place.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1A13))
                                            Text(
                                                text = place.subtitle,
                                                fontSize = 12.sp,
                                                color = NestoraTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFF2F4F5), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── FLOATING BOTTOM CONTAINER (Current Location pill & Confirm Card) ───
        // Only visible when suggestions are NOT active
        AnimatedVisibility(
            visible = !isMapDragging && !(showSuggestions && searchQuery.trim().length >= MIN_SEARCH_QUERY_LEN),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pill button "Current location"
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .height(44.dp)
                        .clickable {
                            userExplicitlyTurnedOff = false
                            requestLocationOn(isExplicit = true)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(color = NestoraMint, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Current location",
                                tint = NestoraMintDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isLocating) "Locating…" else "Current location",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NestoraMintDark
                        )
                    }
                }

                // Bottom confirm card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Place the pin at exact service location",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NestoraTextMuted,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            when {
                                isLocating -> {
                                    CircularProgressIndicator(color = NestoraMint, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Getting your current location…", fontSize = 13.sp, color = NestoraTextMuted)
                                }
                                isResolvingAddress -> {
                                    CircularProgressIndicator(color = NestoraMint, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Resolving address…", fontSize = 13.sp, color = NestoraTextMuted)
                                }
                                selectedPlace != null -> {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = NestoraMint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedPlace!!.title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0D1A13)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = selectedPlace!!.subtitle,
                                            fontSize = 13.sp,
                                            color = NestoraTextMuted,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                                else -> Text(
                                    "Drag the map or search to pick a location",
                                    fontSize = 13.sp,
                                    color = NestoraTextMuted
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                selectedPlace?.let { onLocationConfirmed(it.title, it.subtitle, it.latitude, it.longitude) }
                            },
                            enabled = selectedPlace != null && !isResolvingAddress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint, disabledContainerColor = NestoraBorderLight)
                        ) {
                            Text(
                                text = "Confirm & proceed",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Tip: Tap top search bar or drag map to change address.",
                            fontSize = 11.sp,
                            color = NestoraTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapControlButton(label: String, contentDescription: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Black, color = NestoraMint)
        }
    }
}

// MapLibreView / rememberMapViewWithLifecycle now live in
// com.estatenestora.app.ui.components.MapLibreShared — shared with
// BookingDetailScreen's live tracking map, one map stack for the app instead
// of two drifting copies.

// findActivity / isSystemLocationEnabled / getCurrentLocation now live in
// com.estatenestora.app.util.LocationUtils — shared with AutoRegisterScreen's
// "Share My Location" flow, which used to carry its own drifted, less
// robust copy of this exact logic (no permanently-denied detection, no
// Settings-redirect dialog), causing it to behave worse for the identical
// underlying problem this screen already handles.
