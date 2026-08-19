package com.estatenestora.app.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Shared GPS/permission-state helpers — plain android.location, no Google
 * A fused-provider request is attempted first for a reliable fix immediately
 * after Location Services is enabled, with android.location retained as a
 * fallback for devices without a usable Google location provider.
 *
 * This used to be duplicated (and drifting) between MapLocationPickerScreen
 * and AutoRegisterScreen's own "Share My Location" flow: the map picker had
 * already been hardened (permanently-denied detection, Settings-redirect
 * dialogs, resume-based re-checks) while AutoRegisterScreen still had an
 * older, simpler copy that just toasted and gave up — which is why "Share
 * My Location" behaved worse there than in the map picker for the exact
 * same underlying problem. One shared copy now backs both.
 */

/** Unwraps a possibly-wrapped Context to find the hosting Activity — needed
 * for shouldShowRequestPermissionRationale, which only exists on Activity,
 * not Context. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

/** Whether the system's Location toggle is on at all — independent of
 * whether this app has permission. No normal app can flip this on/off
 * itself; the best it can do is detect it and send the user to the system
 * screen that can (Settings.ACTION_LOCATION_SOURCE_SETTINGS). */
fun isSystemLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF
        }
    } catch (e: Throwable) {
        false
    }
}

// A cold GPS_PROVIDER lock (no cached fix, no Google-services-backed
// NETWORK_PROVIDER on most non-Play-Services setups) can easily take well
// over 8s outdoors and may never resolve at all indoors — 15s gives it a
// fair shot while still bounded.
private const val FUSED_FIX_TIMEOUT_MS = 20_000L
private const val FRESH_FIX_TIMEOUT_MS = 15_000L

/**
 * Resolves the device's current location: any cached fix is returned
 * immediately (better UX than waiting out a fresh lock when something close
 * is already known), otherwise waits up to [FRESH_FIX_TIMEOUT_MS] for a
 * fresh fix before falling back to whatever's cached (even if stale) rather
 * than hanging forever. Returns null only if permission is missing, no
 * provider is enabled at all, or truly nothing is available in time.
 */
@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null

    // After the user enables Location Services, LocationManager may not have
    // a cached fix yet. Fused location can combine GPS/Wi-Fi/cell signals and
    // is much more reliable for this first fix.
    val fusedLocation = getFusedCurrentLocation(context)
    if (fusedLocation != null) return fusedLocation

    return getPlatformCurrentLocation(context)
}

/**
 * Keeps one high-accuracy fused request alive for live booking tracking.
 * A one-shot getCurrentLocation call is suitable for a form, but repeatedly
 * starting and stopping it can leave a moving provider with a stale server
 * position when a fix takes longer than the polling interval.
 */
@SuppressLint("MissingPermission")
fun continuousLocationUpdates(context: Context, intervalMillis: Long = 3_000L): Flow<Location> = callbackFlow {
    if (!hasLocationPermission(context) || !isSystemLocationEnabled(context)) {
        close()
        return@callbackFlow
    }

    val client = LocationServices.getFusedLocationProviderClient(context)
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
        .setMinUpdateIntervalMillis((intervalMillis / 2).coerceAtLeast(1_000L))
        .setWaitForAccurateLocation(false)
        .build()
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { trySend(it).isSuccess }
        }
    }

    try {
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { close(it) }
    } catch (e: Throwable) {
        close(e)
    }

    awaitClose {
        client.removeLocationUpdates(callback)
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun getFusedCurrentLocation(context: Context): Location? =
    withTimeoutOrNull(FUSED_FIX_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val cancellationSource = CancellationTokenSource()
            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationSource.token)
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location, null)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null, null)
                    }
            } catch (e: Throwable) {
                if (cont.isActive) cont.resume(null, null)
            }

            cont.invokeOnCancellation { cancellationSource.cancel() }
        }
    }

@SuppressLint("MissingPermission")
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun getPlatformCurrentLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val enabledProviders = try {
        locationManager.getProviders(true).filter { it != LocationManager.PASSIVE_PROVIDER }
    } catch (e: Throwable) {
        emptyList()
    }

    fun latestCached(): Location? = try {
        locationManager.getAllProviders().mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    } catch (e: SecurityException) {
        null
    }

    val cached = latestCached()
    if (cached != null) {
        cont.resume(cached, null)
        return@suspendCancellableCoroutine
    }

    if (enabledProviders.isEmpty()) {
        cont.resume(null, null)
        return@suspendCancellableCoroutine
    }

    var resumed = false
    val handler = Handler(Looper.getMainLooper())
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (resumed) return
            resumed = true
            enabledProviders.forEach { runCatching { locationManager.removeUpdates(this) } }
            cont.resume(location, null)
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    enabledProviders.forEach { provider ->
        runCatching { locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()) }
    }

    val timeoutRunnable = Runnable {
        if (resumed) return@Runnable
        resumed = true
        runCatching { locationManager.removeUpdates(listener) }
        cont.resume(latestCached(), null)
    }
    handler.postDelayed(timeoutRunnable, FRESH_FIX_TIMEOUT_MS)

    cont.invokeOnCancellation {
        handler.removeCallbacks(timeoutRunnable)
        runCatching { locationManager.removeUpdates(listener) }
    }
}
