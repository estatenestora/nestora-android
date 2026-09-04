package com.estatenestora.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.data.model.BookingSummary

/** Customer-only booking history reached from HIRE Profile, never the provider workspace. */
@Composable
fun CustomerBookingsScreen(
    bookings: List<BookingSummary>,
    customerUserId: String?,
    onBack: () -> Unit,
    onExploreServices: () -> Unit,
    onBookingClick: (BookingSummary) -> Unit,
    onHelpClick: () -> Unit = {}
) {
    val customerBookings = remember(bookings, customerUserId) {
        bookings.filter { booking ->
            !customerUserId.isNullOrBlank() && booking.customerUserId == customerUserId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF121212))
            }
            Text(
                text = "My bookings",
                modifier = Modifier.weight(1f),
                color = Color(0xFF121212),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = onHelpClick,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE3E1E8))
            ) {
                Text("Help", color = Color(0xFF6D3AE6), fontWeight = FontWeight.Medium)
            }
        }
        HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)

        if (customerBookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.padding(horizontal = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No bookings yet.",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF141414)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Looks like you haven’t experienced quality services at home.",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = Color(0xFF616161),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.clickable { onExploreServices() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Explore our services", color = Color(0xFF6D3AE6), fontSize = 15.sp)
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF6D3AE6)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(customerBookings, key = BookingSummary::id) { booking ->
                    CustomerBookingRow(booking = booking, onClick = { onBookingClick(booking) })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun CustomerBookingRow(booking: BookingSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEDEDED))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = booking.listingTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF171717)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = booking.stageLabel.ifBlank { booking.status },
                    fontSize = 13.sp,
                    color = Color(0xFF6B6B6B)
                )
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF9A9A9A))
        }
    }
}
