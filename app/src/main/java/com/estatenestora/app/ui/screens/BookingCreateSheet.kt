package com.estatenestora.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.data.model.BookingQuote
import com.estatenestora.app.data.model.ServiceListing
import com.estatenestora.app.data.repository.NestoraRepository
import com.estatenestora.app.ui.theme.NestoraMint
import com.estatenestora.app.ui.theme.NestoraTextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingCreateSheet(
    listing: ServiceListing,
    initialLocationText: String,
    initialLat: Double,
    initialLon: Double,
    onDismiss: () -> Unit,
    onFetchQuote: suspend (String) -> BookingQuote?,
    onConfirmBooking: suspend (String, Boolean, Double, Double, String) -> NestoraRepository.CreateBookingResult,
    onBookingCreated: (String) -> Unit,
    onChangeLocationClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quote by remember { mutableStateOf<BookingQuote?>(null) }
    var isLoadingQuote by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Upfront booking options. The address itself is never typed here — it
    // defaults to the home-page address bar location and can only be changed
    // by redirecting to the map picker (see onChangeLocationClick), which
    // remounts this sheet with a new initialLocationText/Lat/Lon.
    var isHomeService by remember { mutableStateOf(true) }
    val addressText = initialLocationText
    val hasValidCoordinates = initialLat.isFinite() && initialLon.isFinite() &&
        initialLat in -90.0..90.0 && initialLon in -180.0..180.0 &&
        !(initialLat == 0.0 && initialLon == 0.0)

    LaunchedEffect(listing.id) {
        isLoadingQuote = true
        errorText = null
        quote = onFetchQuote(listing.id)
        isLoadingQuote = false
        if (quote == null) {
            errorText = "Could not load pricing for this service. Please try again."
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = listing.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0D1A13)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "by ${listing.providerName}",
                fontSize = 13.sp,
                color = NestoraTextMuted
            )
            Spacer(Modifier.height(20.dp))

            // Service Choice Toggle
            Text(
                text = "Select Service Preference",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF33443C),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(46.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val activeBg = NestoraMint
                val activeText = Color.White
                val inactiveBg = Color(0xFFF2F8F5)
                val inactiveText = Color(0xFF4A5C53)

                Button(
                    onClick = { isHomeService = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHomeService) activeBg else inactiveBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("🏠 Home Service", color = if (isHomeService) activeText else inactiveText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { isHomeService = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isHomeService) activeBg else inactiveBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("📅 Appointment", color = if (!isHomeService) activeText else inactiveText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(18.dp))

            // Address — read-only, defaults to the home-page address bar
            // location. The only way to change it is the map picker.
            Text(
                text = if (isHomeService) "Delivery Address" else "Appointment Address",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF33443C),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF2F8F5),
                border = BorderStroke(1.dp, Color(0xFFD0DFD9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (addressText.isNotBlank()) addressText else "No location selected",
                        fontSize = 14.sp,
                        color = if (addressText.isNotBlank()) Color(0xFF0D1A13) else NestoraTextMuted,
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onChangeLocationClick, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("Change", color = NestoraMint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            if (!hasValidCoordinates) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A GPS position is required so your provider can find you. Tap Change to choose it on the map.",
                    fontSize = 12.sp,
                    color = Color(0xFFB45309),
                    lineHeight = 17.sp
                )
            }
            Spacer(Modifier.height(18.dp))

            if (isLoadingQuote) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NestoraMint)
                }
            } else if (quote != null) {
                val q = quote!!
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF7FDFA),
                    border = BorderStroke(1.dp, Color(0xFFD4EFE6))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Estimated Price", fontSize = 13.sp, color = NestoraTextMuted)
                            Text("₹${q.serviceFee.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1A13))
                        }
                        Text(
                            text = "Final amount may vary slightly based on the actual work done.",
                            fontSize = 11.sp,
                            color = NestoraTextMuted
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = q.cancellationPolicy,
                    fontSize = 12.sp,
                    color = NestoraTextMuted,
                    lineHeight = 18.sp
                )
            }

            if (errorText != null) {
                Spacer(Modifier.height(14.dp))
                Text(errorText!!, fontSize = 13.sp, color = Color(0xFFB71C1C), fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        isCreating = true
                        errorText = null
                        // Determine coordinate values
                        val lat = initialLat
                        val lon = initialLon
                        val addr = addressText

                        if (!hasValidCoordinates) {
                            errorText = "Please choose a valid customer location before booking."
                            isCreating = false
                            return@launch
                        }

                        val result = onConfirmBooking(listing.id, isHomeService, lat, lon, addr)
                        isCreating = false
                        val bookingId = result.bookingId
                        if (bookingId != null) {
                            onBookingCreated(bookingId)
                        } else {
                            errorText = result.errorReply ?: "Could not create booking. Please try again."
                        }
                    }
                },
                enabled = quote != null && !isCreating && addressText.isNotBlank() && hasValidCoordinates,
                colors = ButtonDefaults.buttonColors(containerColor = NestoraMint, disabledContainerColor = Color(0xFFB9E3D3)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text("Confirm Booking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
