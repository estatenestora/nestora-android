package com.estatenestora.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import com.estatenestora.app.data.model.BookingDetail
import com.estatenestora.app.ui.components.LiveTrackingMap
import com.estatenestora.app.ui.theme.NestoraMint
import com.estatenestora.app.ui.theme.NestoraTextMuted
import com.estatenestora.app.util.getCurrentLocation
import com.estatenestora.app.util.continuousLocationUpdates
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.estatenestora.app.util.hasLocationPermission
import com.estatenestora.app.util.isSystemLocationEnabled
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("d MMM, h:mm a")

private fun formatIso(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        java.time.Instant.parse(iso).atZone(ZoneId.systemDefault()).format(timeFormatter)
    } catch (e: Exception) {
        iso
    }
}

private fun hasUsableCoordinates(lat: Double?, lon: Double?): Boolean =
    lat != null && lon != null && lat.isFinite() && lon.isFinite() &&
        lat in -90.0..90.0 && lon in -180.0..180.0 &&
        !(lat == 0.0 && lon == 0.0)

@Composable
fun BookingDetailScreen(
    detail: BookingDetail?,
    viewerUserId: String?,
    onBack: () -> Unit,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    onStartTravel: (Double, Double) -> Unit = { _, _ -> },
    onMarkArrived: () -> Unit = {},
    onVerifyOtp: (String, (String?) -> Unit) -> Unit = { _, _ -> },
    onStartService: () -> Unit = {},
    onCompleteService: () -> Unit = {},
    onSubmitReview: suspend (Double, String) -> Boolean = { _, _ -> false },
    onGetCancellationPreview: suspend (String) -> com.estatenestora.app.data.model.CancelPreview? = { null },
    onCancelBooking: suspend (String) -> com.estatenestora.app.data.model.AndroidBridgeResponse? = { null },
    onGetPaymentInfo: suspend (String) -> com.estatenestora.app.data.model.PaymentInfo? = { null },
    onConfirmPayment: suspend (String) -> com.estatenestora.app.data.model.AndroidBridgeResponse? = { null },
    onPushLiveLocation: suspend (String, Double, Double) -> Boolean = { _, _, _ -> false },
    onPushCustomerLiveLocation: suspend (String, Double, Double) -> Boolean = { _, _, _ -> false },
    onRepairCustomerLocation: suspend (String, Double, Double, String) -> Boolean = { _, _, _, _ -> false }
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelPreview by remember { mutableStateOf<com.estatenestora.app.data.model.CancelPreview?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }
    var isSubmittingCancel by remember { mutableStateOf(false) }
    var isSubmittingPayment by remember { mutableStateOf(false) }
    var cancelError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var liveLocationPermissionGranted by remember {
        mutableStateOf(hasLocationPermission(context))
    }

    val liveTrackingPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        liveLocationPermissionGranted = grants[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission(context)
    }

    var showAcceptDialog by remember { mutableStateOf(false) }

    // Rating & Feedback States (Swiggy Rate Screen)
    var rating by remember { mutableStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    var feedbackSubmitted by remember { mutableStateOf(false) }
    var activeRoute by remember(detail?.id) { mutableStateOf<com.estatenestora.app.data.remote.RoutingClient.Route?>(null) }
    val trackingDestinationLat = detail?.let {
        it.destinationLatitude ?: it.customerLatitude.takeIf { _ -> it.isHomeService }
    }
    val trackingDestinationLon = detail?.let {
        it.destinationLongitude ?: it.customerLongitude.takeIf { _ -> it.isHomeService }
    }
    val hasTrackingDestination = hasUsableCoordinates(trackingDestinationLat, trackingDestinationLon)

    val isCancellable = detail != null && detail.status.uppercase() !in listOf("PAID", "CLOSED", "COMPLETED", "CANCELLED")

    // The current traveler always publishes their own position. During
    // provider travel, the customer also publishes a separate, temporary
    // destination fix. The backend retains the booking-time pin as fallback.
    val isViewerTraveler = detail != null && viewerUserId != null && (
        (detail.status.equals("PROVIDER_EN_ROUTE", ignoreCase = true) && detail.providerUserId == viewerUserId) ||
        (detail.status.equals("CUSTOMER_EN_ROUTE", ignoreCase = true) && detail.customerUserId == viewerUserId)
    )
    val isViewerLiveCustomerTarget = detail != null && viewerUserId != null && detail.isHomeService &&
        detail.status.equals("PROVIDER_EN_ROUTE", ignoreCase = true) && detail.customerUserId == viewerUserId
    val shouldShareLiveLocation = isViewerTraveler || isViewerLiveCustomerTarget
    LaunchedEffect(detail?.id, detail?.status, shouldShareLiveLocation, isViewerTraveler, liveLocationPermissionGranted) {
        if (!shouldShareLiveLocation || detail == null) return@LaunchedEffect

        if (!liveLocationPermissionGranted) {
            liveTrackingPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return@LaunchedEffect
        }

        var lastPushAt = 0L
        while (isActive) {
            if (!hasLocationPermission(context) || !isSystemLocationEnabled(context)) {
                delay(1_000L)
                continue
            }

            continuousLocationUpdates(context, intervalMillis = 3_000L)
                .conflate()
                .collect { location ->
                    // Do not publish a coarse/stale fix as live movement. It
                    // can make a nearby provider jump hundreds of metres and
                    // produces a misleading ETA until the next good fix.
                    if (location.hasAccuracy() && location.accuracy > 100f) return@collect
                    if (location.time > 0L && System.currentTimeMillis() - location.time > 30_000L) return@collect
                    val waitMillis = 8_000L - (System.currentTimeMillis() - lastPushAt)
                    if (waitMillis > 0) delay(waitMillis)
                    lastPushAt = System.currentTimeMillis()
                    if (isViewerTraveler) {
                        onPushLiveLocation(detail.id, location.latitude, location.longitude)
                    } else {
                        onPushCustomerLiveLocation(detail.id, location.latitude, location.longitude)
                    }
                }
            delay(1_000L)
        }
    }

    // Older bookings could be created while the address was known but the GPS
    // fix was still 0,0. When the customer opens such a home-service booking,
    // repair its fixed destination once so both maps receive the missing pin.
    val needsCustomerLocationRepair = detail != null && detail.isHomeService &&
        detail.customerUserId == viewerUserId &&
        !hasUsableCoordinates(detail.customerLatitude, detail.customerLongitude)
    LaunchedEffect(detail?.id, needsCustomerLocationRepair) {
        val currentDetail = detail ?: return@LaunchedEffect
        if (!needsCustomerLocationRepair || !hasLocationPermission(context) || !isSystemLocationEnabled(context)) {
            return@LaunchedEffect
        }
        val location = getCurrentLocation(context) ?: return@LaunchedEffect
        repeat(3) { attempt ->
            if (onRepairCustomerLocation(
                    currentDetail.id,
                    location.latitude,
                    location.longitude,
                    currentDetail.customerAddress
                )
            ) {
                return@LaunchedEffect
            }
            if (attempt < 2) delay(1_000L * (attempt + 1))
        }
    }


            // Cancellation Alert Dialog
            if (showCancelDialog && detail != null) {
                LaunchedEffect(detail.id) {
                    isLoadingPreview = true
                    cancelError = null
                    cancelPreview = null
                    try {
                        val resp = onGetCancellationPreview(detail.id)
                        if (resp != null) {
                            cancelPreview = resp
                        } else {
                            cancelError = "Could not fetch cancellation details."
                        }
                    } catch (e: Exception) {
                        cancelError = e.message ?: "An error occurred."
                    } finally {
                        isLoadingPreview = false
                    }
                }

                AlertDialog(
                    onDismissRequest = { if (!isSubmittingCancel) showCancelDialog = false },
                    title = { Text("Cancel Booking?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1A13)) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isLoadingPreview) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = NestoraMint, modifier = Modifier.size(28.dp))
                                }
                            } else if (cancelError != null) {
                                Text(cancelError!!, color = Color(0xFFAB3B3B), fontSize = 13.sp)
                            } else if (cancelPreview != null) {
                                val preview = cancelPreview!!
                                if (preview.isFree) {
                                    Text("Free Cancellation", fontWeight = FontWeight.Bold, color = NestoraMint, fontSize = 15.sp)
                                    Text("Your cancellation is free. No charges will apply to your account.", fontSize = 13.sp, color = Color(0xFF333333))
                                } else {
                                    Text("Chargeable Cancellation", fontWeight = FontWeight.Bold, color = Color(0xFFAB3B3B), fontSize = 15.sp)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Cancellation Fee: ₹${preview.feeAmount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0D1A13))
                                        Text("Fee percentage: ${(preview.feePct * 100).toInt()}% of service fee", fontSize = 12.sp, color = NestoraTextMuted)
                                    }
                                    Text(preview.policySummary ?: "A cancellation fee will be charged to your wallet per policy.", fontSize = 12.sp, color = Color(0xFF555555))
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSubmittingCancel = true
                                    try {
                                        val resp = onCancelBooking(detail.id)
                                        if (resp?.ok == true) {
                                            showCancelDialog = false
                                        } else {
                                            cancelError = resp?.reply ?: "Failed to cancel booking."
                                        }
                                    } catch (e: Exception) {
                                        cancelError = e.message ?: "An error occurred."
                                    } finally {
                                        isSubmittingCancel = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAB3B3B)),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoadingPreview && !isSubmittingCancel
                        ) {
                            if (isSubmittingCancel) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Cancel Booking", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelDialog = false }, enabled = !isSubmittingCancel) {
                            Text("Keep Booking", color = NestoraTextMuted, fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White
                )
            }

            if (showAcceptDialog && detail != null) {
                AlertDialog(
                    onDismissRequest = { showAcceptDialog = false },
                    title = { Text("Accept Booking Request", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0D1A13)) },
                    text = {
                        Text(
                            "Accept this request at the listed price of ₹${detail.serviceFee.toInt()}? The customer will then be asked to pay Provider's advance before you can start.",
                            fontSize = 13.sp,
                            color = Color(0xFF4A5568)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showAcceptDialog = false
                                onAccept()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Accept Request", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAcceptDialog = false }) {
                            Text("Cancel", color = NestoraTextMuted)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White
                )
            }


    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7FDFA))) {
            if (detail == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NestoraMint)
                }
            } else {
                val isViewerProvider = viewerUserId != null && detail.providerUserId == viewerUserId
                val counterpartName = if (isViewerProvider) detail.customerName else detail.providerName

                val statusUpper = detail.status.uppercase()

                if (statusUpper == "PAID") {
                    // ==========================================
                    // 2A. Swiggy RATE & REVIEW FULL SCREEN UI (Figma Page 4)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF7FDFA))
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Delivery Header Banner Box
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF005E46), Color(0xFF004332))
                                        )
                                    )
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Service Completed",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isViewerProvider) "Service completed for ${detail.customerName}" else "Service completed by $counterpartName",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            // Back button overlaid on top-left of the green header banner
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(16.dp)
                                    .align(Alignment.TopStart)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Final settlement summary — the remaining amount is paid
                        // directly customer-to-provider, off-platform; this is just
                        // the breakdown so both sides see the same numbers.
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEDF2F7)),
                            shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Final Summary", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1A13))
                                val effectiveFee = detail.agreedPrice ?: detail.serviceFee
                                DetailMetadataRow("Service Fee", "₹${effectiveFee.toInt()}")
                                DetailMetadataRow("Advance Paid ", "₹${(detail.advanceAmount ?: 0.0).toInt()}")
                                if (detail.commutingFee > 0) {
                                    DetailMetadataRow("Commuting Fee", "₹${detail.commutingFee.toInt()}")
                                }
                                DashedDivider()
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        if (isViewerProvider) "Collect directly from customer" else "Pay directly to $counterpartName",
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1A13)
                                    )
                                    Text("₹${detail.remainingAmount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = NestoraMint)
                                }
                                Text(
                                    "This amount is settled directly between you two — Nestora does not collect it.",
                                    fontSize = 11.sp, color = NestoraTextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Advertising Nestora Carousel
                        FeedbackAdCarousel()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Floating Review Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEDF2F7)),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (!detail.hasReviewed && !feedbackSubmitted) {
                                    Text(
                                        text = if (isViewerProvider) "Rate customer behaviour" else "Rate your service",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2D3748)
                                    )

                                    // Interactive Stars
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        (1..5).forEach { star ->
                                            val isSelected = star <= rating
                                            Text(
                                                text = if (isSelected) "★" else "☆",
                                                fontSize = 36.sp,
                                                color = if (isSelected) Color(0xFFFFB300) else Color(0xFFCBD5E0),
                                                modifier = Modifier.clickable { rating = star }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = reviewText,
                                        onValueChange = { reviewText = it },
                                        placeholder = { Text(if (isViewerProvider) "Write a customer review (optional)" else "Write a quick feedback (optional)", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    var isSubmittingReview by remember { mutableStateOf(false) }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isSubmittingReview = true
                                                val ok = onSubmitReview(rating.toDouble(), reviewText)
                                                isSubmittingReview = false
                                                if (ok) {
                                                    feedbackSubmitted = true
                                                    Toast.makeText(context, "Thank you for your rating!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Could not submit your rating. Please try again.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        enabled = rating > 0 && !isSubmittingReview,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isSubmittingReview) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Submit Feedback", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    // Success Screen
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE6F4EA)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Success", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF137333))
                                    }
                                    Text(
                                        text = "Feedback Submitted Successfully!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF137333)
                                    )
                                    Text(
                                        text = "We appreciate your response.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF718096)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Contact Support footer
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEDF2F7)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:+917076783428")
                                        }
                                        context.startActivity(intent)
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Not satisfied?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                                    Text("Call support partner", fontSize = 11.sp, color = Color(0xFF718096))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFF5F5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call support",
                                        tint = Color(0xFFE53E3E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // 2B. Swiggy MAP & TRACKING STATUS VIEW
                    // ==========================================
                    var statusHeader: String
                    var etaMinutes: String
                    var statusSubtext: String

                    when (detail.status.uppercase()) {
                        "REQUESTED" -> {
                            statusHeader = "Order Received!"
                            statusSubtext = "$counterpartName is reviewing your request."
                            etaMinutes = "25"
                        }
                        "PAYMENT_PENDING" -> {
                            statusHeader = if (isViewerProvider) "Request Accepted!" else "Pay Advance to Confirm"
                            statusSubtext = if (isViewerProvider) "Waiting for the customer to pay the advance commission." else "Pay Provider's advance to confirm this booking."
                            etaMinutes = "Pay"
                        }
                        "PAYMENT_UPLOADED" -> {
                            statusHeader = "Advance Submitted!"
                            statusSubtext = "Nestora is verifying the advance payment."
                            etaMinutes = "Verify"
                        }
                        "CONFIRMED" -> {
                            statusHeader = "Booking Confirmed!"
                            statusSubtext = if (isViewerProvider) "Head over when you're ready to start." else "$counterpartName has confirmed booking."
                            etaMinutes = "15"
                        }
                        "PROVIDER_EN_ROUTE" -> {
                            statusHeader = if (isViewerProvider) "You are traveling" else "Out for service"
                            val routeEta = activeRoute?.let { r ->
                                if (r.isWithinArrivalRange) {
                                    " · Arrived"
                                } else {
                                    val minutes = (r.durationSeconds / 60.0).let { if (it < 1) 1 else Math.round(it) }.toInt()
                                    val km = r.distanceMeters / 1000.0
                                    val distanceText = if (km < 1) "${r.distanceMeters.toInt()} m" else "%.1f km".format(km)
                                    val qualifier = if (r.isApproximate) "Estimated: " else ""
                                    " · ${qualifier}arriving in ~$minutes min ($distanceText away)"
                                }
                            } ?: ""
                            statusSubtext = (if (isViewerProvider) "Heading to customer's location." else "$counterpartName is on the way.") + routeEta
                            val minutesVal = activeRoute?.let { r ->
                                (r.durationSeconds / 60.0).let { if (it < 1) 1 else Math.round(it) }.toInt()
                            }
                            etaMinutes = when {
                                !hasTrackingDestination -> "Waiting for location"
                                activeRoute?.isWithinArrivalRange == true -> "Arrived"
                                minutesVal != null -> minutesVal.toString()
                                else -> "Calculating ETA…"
                            }
                        }
                        "CUSTOMER_EN_ROUTE" -> {
                            statusHeader = if (isViewerProvider) "Customer is traveling" else "You are traveling"
                            val routeEta = activeRoute?.let { r ->
                                if (r.isWithinArrivalRange) {
                                    " · Arrived"
                                } else {
                                    val minutes = (r.durationSeconds / 60.0).let { if (it < 1) 1 else Math.round(it) }.toInt()
                                    val km = r.distanceMeters / 1000.0
                                    val distanceText = if (km < 1) "${r.distanceMeters.toInt()} m" else "%.1f km".format(km)
                                    val qualifier = if (r.isApproximate) "Estimated: " else ""
                                    " · ${qualifier}arriving in ~$minutes min ($distanceText away)"
                                }
                            } ?: ""
                            statusSubtext = (if (isViewerProvider) "Customer is heading to your center." else "Heading to $counterpartName's center location.") + routeEta
                            val minutesVal = activeRoute?.let { r ->
                                (r.durationSeconds / 60.0).let { if (it < 1) 1 else Math.round(it) }.toInt()
                            }
                            etaMinutes = when {
                                !hasTrackingDestination -> "Waiting for location"
                                activeRoute?.isWithinArrivalRange == true -> "Arrived"
                                minutesVal != null -> minutesVal.toString()
                                else -> "Calculating ETA…"
                            }
                        }
                        "PROVIDER_ARRIVED", "CUSTOMER_ARRIVED" -> {
                            statusHeader = "Arrived at location!"
                            statusSubtext = "$counterpartName has arrived!"
                            etaMinutes = "Arrived"
                        }
                        "OTP_VERIFIED" -> {
                            statusHeader = "OTP Verified!"
                            statusSubtext = "$counterpartName verified OTP."
                            etaMinutes = "1"
                        }
                        "SERVICE_STARTED" -> {
                            statusHeader = "Service in progress!"
                            statusSubtext = if (isViewerProvider) "Tap Service Completed once you're done." else "$counterpartName is performing the service."
                            etaMinutes = "Active"
                        }
                        "PAID", "CLOSED", "COMPLETED" -> {
                            statusHeader = "Service Completed!"
                            statusSubtext = "Pay the remaining amount directly to $counterpartName."
                            etaMinutes = "Done"
                        }
                        else -> {
                            statusHeader = "Booking Status"
                            statusSubtext = "Booking is inactive or cancelled."
                            etaMinutes = "-"
                        }
                    }

                    // 50% Screen height dynamic scaling (ss1 / user requested)
                    val configuration = LocalConfiguration.current
                    val mapHeight = (configuration.screenHeightDp / 2).dp

                    // 1. Map Card Backdrop with rounded corners at bottom corner (ss1 & ss4)
                    Card(
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(mapHeight)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val statusU = detail.status.uppercase()
                            // Home-service destination is the customer's
                            // saved booking location. Keep this client-side
                            // fallback for older/server responses that omit
                            // destination_latitude but still include the
                            // underlying customer coordinates.
                            val mapDestinationLat = trackingDestinationLat
                            val mapDestinationLon = trackingDestinationLon
                            // Pre-approval: neither map data nor addresses are relevant
                            // yet on either flow — nothing has been approved/paid.
                            val advanceApproved = statusU !in listOf("REQUESTED", "PAYMENT_PENDING")
                            val activeStatuses = listOf("CONFIRMED", "PROVIDER_EN_ROUTE", "PROVIDER_ARRIVED", "OTP_VERIFIED", "SERVICE_STARTED")
                            // Home-service: the SAME map component covers both states —
                            // a real map with just the customer pinned (traveler=null)
                            // before "GPS Tracking" is tapped, and live route/ETA once a
                            // traveler position exists — never a fake placeholder box.
                            val showLiveMap = detail.isHomeService && advanceApproved && statusU in activeStatuses
                            // Appointment: once the customer's advance is at least
                            // submitted (PAYMENT_UPLOADED+), show both fixed positions
                            // on a static map — neither party has live GPS tracking here.
                            val showStaticDualMap = !detail.isHomeService && advanceApproved && statusU in activeStatuses &&
                                detail.customerLatitude != null && detail.customerLongitude != null &&
                                detail.destinationLatitude != null && detail.destinationLongitude != null

                            if (!advanceApproved) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0xFFEFF6F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = NestoraTextMuted, modifier = Modifier.size(36.dp))
                                        Text(
                                            if (detail.isHomeService) "Address & map unlock once Nestora approves the advance" else "Location & map unlock once the advance is paid",
                                            fontSize = 13.sp, color = Color(0xFF33443C), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)
                                        )
                                    }
                                }
                            } else if (showLiveMap) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LiveTrackingMap(
                                        travelerLat = detail.travelerLatitude,
                                        travelerLon = detail.travelerLongitude,
                                        destinationLat = mapDestinationLat,
                                        destinationLon = mapDestinationLon,
                                        travelerHeadline = if (isViewerProvider) "You're on the way to $counterpartName" else "$counterpartName is on the way to you",
                                        lastUpdatedIso = detail.travelerLocationUpdatedAt,
                                        modifier = Modifier.fillMaxSize(),
                                        showEtaBadge = false,
                                        showRoute = statusU in listOf("PROVIDER_EN_ROUTE", "CUSTOMER_EN_ROUTE"),
                                        onRouteLoaded = { activeRoute = it }
                                    )
                                    if (detail.travelerLatitude == null && hasTrackingDestination) {
                                        // Pre-tracking: real map, destination pinned, no
                                        // "waiting" overlay — this IS the intended static state.
                                        Surface(
                                            shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 4.dp,
                                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                                        ) {
                                            Text(
                                                if (isViewerProvider) "Tap GPS Tracking below to share your live location" else "Live tracking starts once $counterpartName taps GPS Tracking",
                                                fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1A13),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                    if (!hasTrackingDestination) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 4.dp,
                                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                                        ) {
                                            Text(
                                                "Waiting for the customer's GPS location",
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF0D1A13)
                                            )
                                        }
                                    }
                                }
                            } else if (showStaticDualMap) {
                                com.estatenestora.app.ui.components.StaticDualPositionMap(
                                    customerLat = detail.customerLatitude!!,
                                    customerLon = detail.customerLongitude!!,
                                    providerLat = detail.destinationLatitude!!,
                                    providerLon = detail.destinationLongitude!!,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Fallback only — reached for closed/cancelled bookings
                                // past the map-relevant statuses, or if location data is
                                // unexpectedly missing for an otherwise-active booking.
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0xFFEFF6F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = NestoraTextMuted, modifier = Modifier.size(36.dp))
                                        val addressLabel = if (detail.isHomeService) {
                                            detail.customerAddress.ifBlank { "Address shared at booking time" }
                                        } else {
                                            if (isViewerProvider) { "You have an appointment with $counterpartName" } else { "Appointment at $counterpartName's location" }
                                        }
                                        Text(addressLabel, fontSize = 13.sp, color = Color(0xFF33443C), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                                    }
                                }
                            }

                            // Top header overlaid floating (ss1 -> ss2 clean layout box containing aligned items)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .align(Alignment.CenterStart)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .shadow(2.dp, CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1A202C))
                                }

                                // Clean centered alignment without white background box (ss2 style)
                                // Standard padding 56.dp keeps text 1cm away from the left/right buttons.
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 56.dp)
                                ) {
                                    Text(
                                        text = detail.listingTitle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A202C),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    val timeText = if (detail.createdAt.isNotBlank()) formatIso(detail.createdAt) else ""
                                    val subtitleText = buildString {
                                        if (timeText.isNotBlank()) {
                                            append(timeText)
                                            append(" · ")
                                        }
                                        append("ORDER #${detail.referenceCode.ifBlank { detail.id.take(8) }}")
                                    }
                                    
                                    Text(
                                        text = subtitleText,
                                        fontSize = 11.sp,
                                        color = Color(0xFF718096),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .shadow(2.dp, CircleShape)
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color(0xFF1A202C))
                                    }
                                }
                            }

                            // Compact, little rounded status card overlay at the bottom showing provider details (ss4)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.95f),
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = statusHeader,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF1A202C)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = statusSubtext, // Dynamic John is on the way status text
                                            fontSize = 11.sp,
                                            color = Color(0xFF4A5568)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Compact ETA indicator (only shown when etaMinutes is valid)
                                    if (etaMinutes.isNotBlank() && etaMinutes != "-") {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF137333),
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            Text(
                                                text = if (etaMinutes.all { it.isDigit() }) "$etaMinutes MINS" else etaMinutes,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Scrollable details area (Weight 1f)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }

                        // Upfront Advance Payment / Actions / OTP Card (Unified Action & Status Panel - before While you wait, after Map)
                        val showActionsTopCard = detail.status.uppercase() in listOf(
                            "REQUESTED", "PAYMENT_PENDING", "PAYMENT_UPLOADED", "CONFIRMED",
                            "PROVIDER_EN_ROUTE", "PROVIDER_ARRIVED", "CUSTOMER_ARRIVED", "OTP_VERIFIED", "SERVICE_STARTED"
                        )
                        if (showActionsTopCard) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (detail.status == "PROVIDER_ARRIVED" || detail.status == "CUSTOMER_ARRIVED") {
                                            // Case 1: OTP Gated verification
                                            if (isViewerProvider) {
                                                Text("Enter OTP code from customer to start service:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1A13))
                                                
                                                var otpInput by remember { mutableStateOf("") }
                                                var otpError by remember { mutableStateOf<String?>(null) }
                                                var isVerifyingOtp by remember { mutableStateOf(false) }

                                                val focusRequester = remember { FocusRequester() }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp)
                                                        .clickable { focusRequester.requestFocus() },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    BasicTextField(
                                                        value = otpInput,
                                                        onValueChange = { newValue ->
                                                            val cleanValue = newValue.filter { it.isDigit() }
                                                            if (cleanValue.length <= 4) {
                                                                otpInput = cleanValue
                                                            }
                                                        },
                                                        keyboardOptions = KeyboardOptions(
                                                            keyboardType = KeyboardType.Number,
                                                            imeAction = ImeAction.Done
                                                        ),
                                                        modifier = Modifier
                                                            .size(1.dp)
                                                            .focusRequester(focusRequester),
                                                        decorationBox = { it() }
                                                    )

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        for (i in 0 until 4) {
                                                            val char = otpInput.getOrNull(i)?.toString() ?: ""
                                                            val isFocused = i == otpInput.length

                                                            Box(
                                                                modifier = Modifier
                                                                    .size(54.dp)
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(Color(0xFFF7FDFA))
                                                                    .border(
                                                                        width = if (isFocused) 2.dp else 1.dp,
                                                                        color = if (isFocused) NestoraMint else Color(0xFFD0DFD9),
                                                                        shape = RoundedCornerShape(12.dp)
                                                                    )
                                                                    .clickable { focusRequester.requestFocus() },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = char,
                                                                    fontSize = 22.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = Color(0xFF004332)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                LaunchedEffect(Unit) {
                                                    focusRequester.requestFocus()
                                                }

                                                if (otpError != null) {
                                                    Text(otpError!!, color = Color(0xFFAB3B3B), fontSize = 12.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        isVerifyingOtp = true
                                                        otpError = null
                                                        onVerifyOtp(otpInput) { err ->
                                                            if (err != null) {
                                                                isVerifyingOtp = false
                                                                otpError = err
                                                            } else {
                                                                onStartService()
                                                                isVerifyingOtp = false
                                                            }
                                                        }
                                                    },
                                                    enabled = otpInput.length == 4 && !isVerifyingOtp,
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                                                ) {
                                                    if (isVerifyingOtp) {
                                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                    } else {
                                                        Text("Verify & Start Service", color = Color.White)
                                                    }
                                                }
                                            } else {
                                                Text("Share this OTP code with provider to start service:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1A13))
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFFF0FDF4),
                                                    border = BorderStroke(1.5.dp, NestoraMint),
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        detail.otpCode ?: "----",
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF005E46),
                                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                                    )
                                                }
                                                Text("Only share this once your provider has arrived at the location.", fontSize = 11.sp, color = NestoraTextMuted, textAlign = TextAlign.Center)
                                            }
                                        } else if (detail.status == "PAYMENT_PENDING" || detail.status == "PAYMENT_UPLOADED") {
                                            // Case 2: PAYMENT_PENDING or PAYMENT_UPLOADED
                                            if (isViewerProvider) {
                                                var billing by remember(detail.id, detail.status) { mutableStateOf<com.estatenestora.app.data.model.PaymentInfo?>(null) }
                                                LaunchedEffect(detail.id, detail.status) { billing = onGetPaymentInfo(detail.id) }
                                                val advanceAmt = billing?.amount ?: detail.advanceAmount ?: 0.0
                                                
                                                Text(
                                                    text = if (detail.status == "PAYMENT_UPLOADED") "Advance submitted — awaiting Nestora verification" else "Waiting for customer to pay the advance",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0D1A13),
                                                    textAlign = TextAlign.Center
                                                )
                                                Text("Advance : ₹${advanceAmt.toInt()}", fontSize = 12.sp, color = NestoraTextMuted)
                                                if (detail.commutingFee > 0) {
                                                    Text("Commuting fee (added to remaining amount): ₹${detail.commutingFee.toInt()}", fontSize = 12.sp, color = NestoraTextMuted)
                                                }
                                                Text("Remaining amount owed to you after service: ₹${detail.remainingAmount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NestoraMint)
                                            } else {
                                                val effectiveFee = detail.agreedPrice ?: detail.serviceFee
                                                val commissionPct = detail.advanceCommissionPct ?: 20.0
                                                val advanceAmt = detail.advanceAmount ?: Math.round(effectiveFee * commissionPct / 100.0).toDouble()

                                                Text(
                                                    text = "Upfront Advance Payment is due: ₹${advanceAmt.toInt()} (${commissionPct.toInt()}% Commission)",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFAB3B3B),
                                                    textAlign = TextAlign.Center
                                                )

                                                if (detail.status == "PAYMENT_PENDING") {
                                                     if (detail.paymentRejected) {
                                                         Surface(
                                                             shape = RoundedCornerShape(12.dp),
                                                             color = Color(0xFFFFF5F5),
                                                             border = BorderStroke(1.dp, Color(0xFFFEB2B2)),
                                                             modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                                         ) {
                                                             Column(modifier = Modifier.padding(14.dp)) {
                                                                 Text(
                                                                     text = "⚠️ Last Payment Rejected",
                                                                     fontSize = 12.sp,
                                                                     fontWeight = FontWeight.Bold,
                                                                     color = Color(0xFFC53030)
                                                                 )
                                                                 Spacer(modifier = Modifier.height(4.dp))
                                                                 Text(
                                                                     text = "Your payment was rejected. Please check your last transaction with Nestora.",
                                                                     fontSize = 11.sp,
                                                                     color = Color(0xFF742A2A),
                                                                     lineHeight = 15.sp
                                                                 )
                                                             }
                                                         }
                                                     }
                                                    Text(
                                                        text = "Please pay the upfront commission advance using any UPI app on your device, and tap 'Confirm Payment' once done.",
                                                        fontSize = 11.sp,
                                                        color = NestoraTextMuted,
                                                        textAlign = TextAlign.Center,
                                                        lineHeight = 15.sp
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                scope.launch {
                                                                    val payInfo = onGetPaymentInfo(detail.id)
                                                                    if (payInfo != null) {
                                                                        val upiUriString = "upi://pay" +
                                                                                "?pa=${payInfo.upiId}" +
                                                                                "&pn=${Uri.encode(payInfo.payeeName)}" +
                                                                                "&am=${payInfo.amount}" +
                                                                                "&cu=${payInfo.currency}" +
                                                                                "&tn=${Uri.encode(payInfo.note)}" +
                                                                                "&tr=${payInfo.txnRef}"
                                                                        try {
                                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUriString))
                                                                            val chooser = Intent.createChooser(intent, "Pay via UPI App")
                                                                            context.startActivity(chooser)
                                                                        } catch (e: Exception) {
                                                                            Toast.makeText(context, "No UPI app found on this device.", Toast.LENGTH_LONG).show()
                                                                        }
                                                                    } else {
                                                                        Toast.makeText(context, "Failed to load payment details.", Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.weight(1f).height(48.dp),
                                                            shape = RoundedCornerShape(12.dp),
                                                            contentPadding = PaddingValues(0.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                                                        ) {
                                                            Text(
                                                                text = "Pay via UPI",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }

                                                        Button(
                                                            onClick = {
                                                                scope.launch {
                                                                    isSubmittingPayment = true
                                                                    val resp = onConfirmPayment(detail.id)
                                                                    isSubmittingPayment = false
                                                                    if (resp?.ok == true) {
                                                                        Toast.makeText(context, "Payment confirmed to admin!", Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        Toast.makeText(context, resp?.reply ?: "Failed to confirm payment.", Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.weight(1f).height(48.dp),
                                                            shape = RoundedCornerShape(12.dp),
                                                            contentPadding = PaddingValues(0.dp),
                                                            enabled = !isSubmittingPayment,
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004332))
                                                        ) {
                                                            if (isSubmittingPayment) {
                                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                            } else {
                                                                Text(
                                                                    text = "Confirm Payment",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = Color(0xFFFFF9E6),
                                                        border = BorderStroke(1.dp, Color(0xFFFFE0B2))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Spacer(Modifier.width(8.dp))
                                                            Text(
                                                                text = "Verification Pending: The admin is currently verifying your payment settlement.",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = Color(0xFFE65100),
                                                                lineHeight = 16.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Case 3: Other Active statuses
                                            if (isViewerProvider) {
                                                when (detail.status) {
                                                    "REQUESTED" -> {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            Button(
                                                                onClick = { android.util.Log.d("BookingDetailScreen", "Accept button clicked, setting showAcceptDialog = true"); showAcceptDialog = true },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(12.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                                                            ) {
                                                                Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                                                            }
                                                            Button(
                                                                onClick = { onReject() },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(12.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAB3B3B))
                                                            ) {
                                                                Text("Reject", color = Color.White, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                    "CONFIRMED" -> {
                                                        if (detail.isHomeService) {
                                                            var isStartingTravel by remember { mutableStateOf(false) }
                                                            var gpsError by remember { mutableStateOf<String?>(null) }

                                                            // Android does not allow an app to silently switch on the device-wide
                                                            // location toggle. Play Services provides the supported system
                                                            // resolution dialog; the full Settings screen is the fallback.
                                                            fun startTravelWithCurrentLocation() {
                                                                scope.launch {
                                                                    isStartingTravel = true
                                                                    gpsError = null
                                                                    val loc = getCurrentLocation(context)
                                                                    if (loc != null) {
                                                                        onStartTravel(loc.latitude, loc.longitude)
                                                                    } else {
                                                                        gpsError = "Could not get your GPS location. Please check location permissions and try again."
                                                                    }
                                                                    isStartingTravel = false
                                                                }
                                                            }

                                                            fun continueAfterLocationSettings() {
                                                                if (isSystemLocationEnabled(context)) {
                                                                    if (hasLocationPermission(context)) {
                                                                        startTravelWithCurrentLocation()
                                                                    }
                                                                } else {
                                                                    gpsError = "Please enable device location and try again."
                                                                }
                                                            }

                                                            val permissionLauncher = rememberLauncherForActivityResult(
                                                                ActivityResultContracts.RequestMultiplePermissions()
                                                            ) { grants ->
                                                                val granted = grants[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                                                    grants[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
                                                                if (granted && isSystemLocationEnabled(context)) {
                                                                    startTravelWithCurrentLocation()
                                                                } else if (!granted) {
                                                                    gpsError = "Location permission is required for GPS tracking."
                                                                } else {
                                                                    gpsError = "Please enable device location and try again."
                                                                }
                                                            }

                                                            val fallbackLocationSettingsLauncher = rememberLauncherForActivityResult(
                                                                ActivityResultContracts.StartActivityForResult()
                                                            ) {
                                                                // Re-check after Settings closes; the result code is not reliable
                                                                // across Android versions and device manufacturers.
                                                                continueAfterLocationSettings()
                                                                if (isSystemLocationEnabled(context) && !hasLocationPermission(context)) {
                                                                    permissionLauncher.launch(
                                                                        arrayOf(
                                                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                                        )
                                                                    )
                                                                }
                                                            }

                                                            val locationResolutionLauncher = rememberLauncherForActivityResult(
                                                                ActivityResultContracts.StartIntentSenderForResult()
                                                            ) {
                                                                continueAfterLocationSettings()
                                                                if (isSystemLocationEnabled(context) && !hasLocationPermission(context)) {
                                                                    permissionLauncher.launch(
                                                                        arrayOf(
                                                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                                        )
                                                                    )
                                                                }
                                                            }

                                                            fun requestLocationSettings() {
                                                                val locationRequest = LocationRequest.Builder(
                                                                    Priority.PRIORITY_HIGH_ACCURACY,
                                                                    1_000L
                                                                ).setMinUpdateIntervalMillis(500L).build()
                                                                val settingsRequest = LocationSettingsRequest.Builder()
                                                                    .addLocationRequest(locationRequest)
                                                                    .setAlwaysShow(true)
                                                                    .build()

                                                                runCatching {
                                                                    LocationServices.getSettingsClient(context)
                                                                        .checkLocationSettings(settingsRequest)
                                                                        .addOnSuccessListener {
                                                                            continueAfterLocationSettings()
                                                                            if (!hasLocationPermission(context)) {
                                                                                permissionLauncher.launch(
                                                                                    arrayOf(
                                                                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                                                    )
                                                                                )
                                                                            }
                                                                        }
                                                                        .addOnFailureListener { error ->
                                                                            if (error is ResolvableApiException) {
                                                                                locationResolutionLauncher.launch(
                                                                                    IntentSenderRequest.Builder(error.resolution).build()
                                                                                )
                                                                            } else {
                                                                                fallbackLocationSettingsLauncher.launch(
                                                                                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                                                                )
                                                                            }
                                                                        }
                                                                }.onFailure {
                                                                    fallbackLocationSettingsLauncher.launch(
                                                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                                                    )
                                                                }
                                                            }

                                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                Text("Advance approved. Tap to start sharing your live location:", fontSize = 12.sp, color = NestoraTextMuted, textAlign = TextAlign.Center)
                                                                // GPS only — no map-picker alternative. Once commuting starts,
                                                                // it's the provider's real, continuously-updating position
                                                                // that matters, not a one-off manually chosen point.
                                                                Button(
                                                                    onClick = {
                                                                        when {
                                                                            !isSystemLocationEnabled(context) -> {
                                                                                gpsError = null
                                                                                requestLocationSettings()
                                                                            }
                                                                            !hasLocationPermission(context) -> {
                                                                                gpsError = null
                                                                                permissionLauncher.launch(
                                                                                    arrayOf(
                                                                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                                                    )
                                                                                )
                                                                            }
                                                                            else -> {
                                                                                startTravelWithCurrentLocation()
                                                                            }
                                                                        }
                                                                    },
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    enabled = !isStartingTravel,
                                                                    shape = RoundedCornerShape(12.dp),
                                                                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                                                                ) {
                                                                    if (isStartingTravel) {
                                                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                                    } else {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.Center
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Default.LocationOn,
                                                                                contentDescription = null,
                                                                                tint = Color.White,
                                                                                modifier = Modifier.size(16.dp)
                                                                            )
                                                                            Spacer(Modifier.width(6.dp))
                                                                            Text("GPS Tracking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                                        }
                                                                    }
                                                                }
                                                                if (gpsError != null) {
                                                                    Text(gpsError!!, fontSize = 11.sp, color = Color(0xFFAB3B3B), textAlign = TextAlign.Center)
                                                                }
                                                            }
                                                        } else {
                                                            Text(
                                                                text = "Waiting for customer to arrive at your location...",
                                                                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NestoraTextMuted, textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                    "PROVIDER_EN_ROUTE" -> {
                                                        val canConfirmArrival = activeRoute?.proximityMeters?.let { it <= 75.0 } == true
                                                        Button(
                                                            onClick = { onMarkArrived() },
                                                            enabled = canConfirmArrival,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                                                        ) {
                                                            Text(if (canConfirmArrival) "I Have Arrived" else "Approaching customer", color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                        if (!canConfirmArrival) {
                                                            Text(
                                                                "Arrival unlocks when you are within 75 m of the customer.",
                                                                fontSize = 11.sp,
                                                                color = NestoraTextMuted,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                    "OTP_VERIFIED" -> {
                                                        Button(
                                                            onClick = { onStartService() },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                                                        ) {
                                                            Text("Start Service", color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    "SERVICE_STARTED" -> {
                                                        Button(
                                                            onClick = { onCompleteService() },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                                                        ) {
                                                            Text("Service Completed", color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    else -> {
                                                        Text(
                                                            text = "Waiting for customer actions...",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = NestoraTextMuted
                                                        )
                                                    }
                                                }
                                            } else {
                                                when (detail.status) {
                                                    "REQUESTED" -> {
                                                        Text(
                                                            text = "Waiting for provider to accept your request...",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = NestoraTextMuted,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                    "CONFIRMED" -> {
                                                        if (detail.isHomeService) {
                                                            Text(
                                                                text = "Advance approved! Waiting for $counterpartName to start commuting.",
                                                                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NestoraTextMuted, textAlign = TextAlign.Center
                                                            )
                                                        } else {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                Text("Head over to $counterpartName's location, then tap when you arrive:", fontSize = 12.sp, color = NestoraTextMuted, textAlign = TextAlign.Center)
                                                                Button(
                                                                    onClick = { onMarkArrived() },
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    shape = RoundedCornerShape(12.dp),
                                                                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                                                                ) {
                                                                    Text("I Have Arrived", color = Color.White, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    "PROVIDER_EN_ROUTE" -> {
                                                        Text(
                                                            text = "Provider is traveling to your location...",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = NestoraTextMuted,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                    "OTP_VERIFIED" -> {
                                                        Text(
                                                            text = "Provider is preparing / service is about to start...",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = NestoraTextMuted,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                    "SERVICE_STARTED" -> {
                                                        Text(
                                                            text = "⚙️ Service is currently in progress...",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = NestoraMint,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // While you wait single-page carousel (ss3/ss5, sliding enabled via detectDragGestures)
                        item {
                            WhileYouWaitCarousel()
                        }

                        // ==========================================
                        // 3. Swiggy ORDER DETAILS Card (Figma Page 3)
                        // ==========================================
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally // Centered layout header (ss3)
                            ) {
                                Text(
                                    text = "ORDER DETAILS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold, // Bold
                                    color = Color(0xFF2D3748),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .width(42.dp)
                                        .height(2.5.dp)
                                        .background(Color(0xFFE53E3E)) // Red Swiggy accent underline
                                )
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFEDF2F7)),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Row 1: Provider Details (Uses Person Material Icon instead of emoji)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEDF2F7)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Provider",
                                                tint = Color(0xFF718096),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = counterpartName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1A202C)
                                            )
                                            Text(
                                                text = detail.listingTitle,
                                                fontSize = 12.sp,
                                                color = Color(0xFF718096)
                                            )
                                        }
                                        val counterpartPhone = if (isViewerProvider) detail.customerPhone else detail.providerPhone
                                        if (counterpartPhone.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFFF5F5))
                                                    .clickable {
                                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                                            data = Uri.parse("tel:$counterpartPhone")
                                                        }
                                                        context.startActivity(intent)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = "Call $counterpartName",
                                                    tint = Color(0xFFE53E3E),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Dashed Divider
                                    DashedDivider(modifier = Modifier.padding(vertical = 14.dp))

                                    // Row 2: Customer Address Details (Uses Location Material Icon instead of emoji)
                                    // Displays Appointment when customer travels to center, else Home Service.
                                    val labelText = if (detail.isHomeService) "Home Service" else "Appointment"
                                    val addressText = if (detail.isHomeService) {
                                        detail.customerAddress.ifBlank { "Address not specified" }
                                    } else {
                                        // Customer sees provider's center address, provider sees their own center.
                                        if (isViewerProvider) "Your Center Location" else "Provider's Center Location"
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEDF2F7)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Location",
                                                tint = Color(0xFFE53E3E), // Red Swiggy pin
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = labelText,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1A202C)
                                            )
                                            Text(
                                                text = addressText,
                                                fontSize = 12.sp,
                                                color = Color(0xFF718096)
                                            )
                                        }
                                    }

                                    // Dashed Divider
                                    DashedDivider(modifier = Modifier.padding(vertical = 14.dp))

                                    // Real details block showing price, booking status, created times, and IDs
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val effectiveFee = detail.agreedPrice ?: detail.serviceFee
                                        val totalPrice = effectiveFee + detail.commutingFee
                                        val remainingAmt = detail.remainingAmount

                                        DetailMetadataRow("Booking Reference", "#${detail.referenceCode.ifBlank { detail.id.take(8) }}")
                                        if (detail.createdAt.isNotBlank()) {
                                            DetailMetadataRow("Booked Date & Time", formatIso(detail.createdAt))
                                        }
                                        if (detail.agreedPrice != null) {
                                            DetailMetadataRow("Agreed Price", "₹${detail.agreedPrice.toInt()}")
                                        } else {
                                            DetailMetadataRow("Service Fee", "₹${detail.serviceFee.toInt()}")
                                        }
                                        if (detail.commutingFee > 0.0) {
                                            DetailMetadataRow("Commuting Fee", "₹${detail.commutingFee.toInt()}")
                                        }
                                        if (detail.advanceAmount != null && detail.advanceAmount > 0.0) {
                                            DetailMetadataRow("Advance Paid", "-₹${detail.advanceAmount.toInt()}")
                                            DetailMetadataRow("Total", "₹${remainingAmt.toInt()}")
                                        } else {
                                            DetailMetadataRow("Total", "₹${totalPrice.toInt()}")
                                        }
                                    }
                                }
                            }
                        }

                        // Problem / Listing Description Card (ss2 -> problemDescription/listing description)
                        if (detail.problemDescription.isNotBlank()) {
                            item {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Listing Description", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NestoraTextMuted)
                                        Text(detail.problemDescription, fontSize = 13.sp, color = Color(0xFF2D3748))
                                    }
                                }
                            }
                        }



                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }

        // Invisible click interceptor to close top-right overlay options menu when clicking anywhere else
        if (showMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showMenu = false }
            )
        }

        // Custom Dropdown Card drawn on top of the click interceptor (ss3 style)
        if (showMenu && detail != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 46.dp, end = 16.dp)
                        .width(IntrinsicSize.Max),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181C24)),
                    border = BorderStroke(1.dp, Color(0xFF2C3545)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        if (isCancellable) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMenu = false
                                        showCancelDialog = true
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = "Cancel Booking",
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            }
                            HorizontalDivider(color = Color(0xFF2C3545), thickness = 0.8.dp)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showMenu = false
                                    Toast.makeText(context, "Redirecting to support chat...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "Help & Support",
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashedDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawLine(
            color = Color(0xFFCBD5E0),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = pathEffect
        )
    }
}

@Composable
private fun DetailMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF718096))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
    }
}

@Composable
private fun WhileYouWaitCarousel() {
    val slides = listOf(
        SlideItem("IKEA Nagasandra turns 3!", "You can stand a chance to win a home makeover worth ₹10 Lacs*.", "VISIT THE STORE", "", Color(0xFFF3F4F6)),
        SlideItem("AC Service Special", "Get absolute cooling with 20% flat discount on professional washing.", "BOOK NOW", "", Color(0xFFEBF8FF)),
        SlideItem("Deep Home Cleaning", "Let professionals make your home sparkle. Safe escrow payments.", "EXPLORE", "", Color(0xFFE6FFFA)),
        SlideItem("Electrical Safety Check", "Prevent short circuits. Free diagnostic check by certified technicians.", "GET CHECK", "", Color(0xFFFFFAF0))
    )
    
    var currentPage by remember { mutableStateOf(0) }
    val slide = slides[currentPage]
    var offsetX by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Centered Heading with red underline squiggle (ss3)
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WHILE YOU WAIT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold, // Bold
                color = Color(0xFF2D3748),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(2.5.dp)
                    .background(Color(0xFFE53E3E))
            )
        }

        // Slide card (finger sliding left/right support + tapping to cycle fallback)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = slide.bgColor,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX > 80f) {
                                // Swipe right -> Previous slide
                                currentPage = if (currentPage > 0) currentPage - 1 else slides.size - 1
                            } else if (offsetX < -80f) {
                                // Swipe left -> Next slide
                                currentPage = (currentPage + 1) % slides.size
                            }
                            offsetX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                        }
                    )
                }
                .clickable {
                    // Fallback click handler to cycle
                    currentPage = (currentPage + 1) % slides.size
                }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slide.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A202C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = slide.description,
                        fontSize = 11.sp,
                        color = Color(0xFF4A5568),
                        lineHeight = 15.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Button indicator
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFD69E2E), // Yellow button
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = slide.buttonText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Illustration side
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(slide.emoji, fontSize = 42.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Page indicator dots centered with customizable fading dots (ss1 layout)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Very tiny dot (far left)
            Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFE2E8F0)))
            Spacer(modifier = Modifier.width(6.dp))
            // Medium dot (inner left)
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFCBD5E0)))
            Spacer(modifier = Modifier.width(8.dp))

            // Main understandable index pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2D3748),
                shadowElevation = 2.dp,
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = "${currentPage + 1}/${slides.size}",
                    fontSize = 11.sp, // Standard understandable size
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            // Medium dot (inner right)
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFCBD5E0)))
            Spacer(modifier = Modifier.width(6.dp))
            // Very tiny dot (far right)
            Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFE2E8F0)))
        }
    }
}

private data class SlideItem(
    val title: String,
    val description: String,
    val buttonText: String,
    val emoji: String,
    val bgColor: Color
)

@Composable
private fun FeedbackAdCarousel() {
    val slides = listOf(
        SlideItem("Nestora Trust Badge", "Providers undergo strict KYC and face verification checks before taking on any booking.", "LEARN MORE", "🛡️", Color(0xFFF0F4FF)),
        SlideItem("Direct Settlements", "No middleman fees on Nestora! Pay remaining amount directly offline to provider.", "EXPLORE", "🤝", Color(0xFFE6FFFA)),
        SlideItem("Secure Escrow", "Your advance payment is held securely in escrow until service completion.", "HOW IT WORKS", "🔒", Color(0xFFEBF8FF)),
        SlideItem("Instant Matching", "Explain your problem, and let our advanced AI matching connect you to top pros.", "TRY FINDER", "⚡", Color(0xFFFFF5F5))
    )
    
    var currentPage by remember { mutableStateOf(0) }
    val slide = slides[currentPage]
    var offsetX by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Slide card (finger sliding left/right support + tapping to cycle fallback)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = slide.bgColor,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX > 80f) {
                                currentPage = if (currentPage > 0) currentPage - 1 else slides.size - 1
                            } else if (offsetX < -80f) {
                                currentPage = (currentPage + 1) % slides.size
                            }
                            offsetX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                        }
                    )
                }
                .clickable {
                    currentPage = (currentPage + 1) % slides.size
                }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Tag label
                    Text(
                        text = "NESTORA ADVANTAGE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NestoraMint,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = slide.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A202C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = slide.description,
                        fontSize = 11.sp,
                        color = Color(0xFF4A5568),
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Button indicator
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NestoraMint,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = slide.buttonText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Illustration side
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(slide.emoji, fontSize = 38.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Page indicator dots centered with index pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Very tiny dot (far left)
            Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFE2E8F0)))
            Spacer(modifier = Modifier.width(6.dp))
            // Medium dot (inner left)
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFCBD5E0)))
            Spacer(modifier = Modifier.width(8.dp))

            // Main understandable index pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2D3748),
                shadowElevation = 2.dp,
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = "${currentPage + 1}/${slides.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            // Medium dot (inner right)
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFCBD5E0)))
            Spacer(modifier = Modifier.width(6.dp))
            // Very tiny dot (far right)
            Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFE2E8F0)))
        }
    }
}

