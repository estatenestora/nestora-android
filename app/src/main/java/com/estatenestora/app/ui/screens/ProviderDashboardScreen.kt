package com.estatenestora.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.data.model.ProviderDashboardSummary
import com.estatenestora.app.ui.theme.NestoraMint
import java.util.Locale

internal enum class ProviderDashboardDestination { BOOKINGS, LISTINGS, AVAILABILITY, PACKAGES, REGISTER, WALLET, ACCOUNT }

internal data class ProviderDashboardAttention(
    val title: String,
    val message: String,
    val destination: ProviderDashboardDestination
)

internal fun providerDashboardAttention(summary: ProviderDashboardSummary): List<ProviderDashboardAttention> = buildList {
    if (summary.walletBalance <= -50.0) {
        add(
            ProviderDashboardAttention(
                "Booking acceptance is paused",
                "Keep your Nestora Money balance above -₹50 to accept another request.",
                ProviderDashboardDestination.WALLET
            )
        )
    }
    if (summary.unseenRequests > 0) {
        add(
            ProviderDashboardAttention(
                "${summary.unseenRequests} new request${if (summary.unseenRequests == 1) "" else "s"}",
                "Review the requested time and respond before the customer chooses another provider.",
                ProviderDashboardDestination.BOOKINGS
            )
        )
    }
    if (summary.disputedJobs > 0) {
        add(
            ProviderDashboardAttention(
                "${summary.disputedJobs} booking${if (summary.disputedJobs == 1) " needs" else "s need"} attention",
                "Open the booking record and follow the support guidance.",
                ProviderDashboardDestination.BOOKINGS
            )
        )
    }
    if (summary.totalListings == 0) {
        add(
            ProviderDashboardAttention(
                "Publish your first service",
                "Customers can find you only after a service listing is published.",
                ProviderDashboardDestination.REGISTER
            )
        )
    } else if (summary.activeListings == 0) {
        add(
            ProviderDashboardAttention(
                "No listing is visible to customers",
                "Review your inactive listings and publish the services you can currently provide.",
                ProviderDashboardDestination.LISTINGS
            )
        )
    } else if (!summary.isAvailable) {
        add(
            ProviderDashboardAttention(
                "Customer requests are paused",
                "Turn availability on when you are ready to receive new service requests.",
                ProviderDashboardDestination.AVAILABILITY
            )
        )
    }
    if (!summary.verificationStatus.equals("VERIFIED", ignoreCase = true)) {
        add(
            ProviderDashboardAttention(
                "Complete provider verification",
                "A verified profile improves customer confidence and marketplace visibility.",
                ProviderDashboardDestination.ACCOUNT
            )
        )
    }
}

@Composable
fun ProviderDashboardContent(
    summary: ProviderDashboardSummary?,
    isLoading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onOpenBookings: () -> Unit,
    onOpenListings: () -> Unit,
    onOpenAvailability: () -> Unit,
    onOpenPackages: () -> Unit,
    onRegisterService: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F6))
            // The dashboard hero is a fixed, edge-to-edge sibling above this
            // list. Keep stretch/overscroll content inside the list viewport
            // so section titles never paint underneath the hero/status bar.
            .clipToBounds()
    ) {
        item {
            DashboardSection {
                Text(
                    "Provider overview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF10231B)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Focus on requests, today’s work, listing visibility and account health.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF63736C)
                )
            }
        }
        item { NestoraSectionDivider() }

        if (loadFailed && summary != null) {
            item {
                DashboardSection {
                    Text("Showing your last loaded overview", fontWeight = FontWeight.Bold, color = Color(0xFF8A5A00))
                    Spacer(Modifier.height(4.dp))
                    Text("Nestora could not refresh the dashboard. No provider data was changed.", fontSize = 11.sp, color = Color(0xFF765F2B))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Try again",
                        modifier = Modifier.clickable(onClick = onRetry),
                        color = NestoraMint,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            item { NestoraSectionDivider() }
        }

        when {
            isLoading && summary == null -> item { ProviderDashboardLoading() }
            loadFailed && summary == null -> item {
                DashboardSection {
                    Text("Dashboard unavailable", fontWeight = FontWeight.Bold, color = Color(0xFF9B2C2C))
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Nestora could not load your latest provider data. Your listings and bookings were not changed.",
                        fontSize = 12.sp,
                        color = Color(0xFF63736C)
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                    ) { Text("Try again") }
                }
            }
            summary != null -> providerDashboardItems(
                summary = summary,
                onOpenBookings = onOpenBookings,
                onOpenListings = onOpenListings,
                onOpenAvailability = onOpenAvailability,
                onOpenPackages = onOpenPackages,
                onRegisterService = onRegisterService,
                onOpenWallet = onOpenWallet,
                onOpenAccount = onOpenAccount
            )
        }

        item { Spacer(Modifier.height(28.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.providerDashboardItems(
    summary: ProviderDashboardSummary,
    onOpenBookings: () -> Unit,
    onOpenListings: () -> Unit,
    onOpenAvailability: () -> Unit,
    onOpenPackages: () -> Unit,
    onRegisterService: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAccount: () -> Unit
) {
    val actionFor: (ProviderDashboardDestination) -> Unit = { destination ->
        when (destination) {
            ProviderDashboardDestination.BOOKINGS -> onOpenBookings()
            ProviderDashboardDestination.LISTINGS -> onOpenListings()
            ProviderDashboardDestination.AVAILABILITY -> onOpenAvailability()
            ProviderDashboardDestination.PACKAGES -> onOpenPackages()
            ProviderDashboardDestination.REGISTER -> onRegisterService()
            ProviderDashboardDestination.WALLET -> onOpenWallet()
            ProviderDashboardDestination.ACCOUNT -> onOpenAccount()
        }
    }
    val attention = providerDashboardAttention(summary)

    if (attention.isNotEmpty()) {
        item {
            DashboardSection {
                DashboardSectionTitle("Needs attention")
                Spacer(Modifier.height(10.dp))
                attention.forEachIndexed { index, item ->
                    AttentionRow(item = item, onClick = { actionFor(item.destination) })
                    if (index != attention.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
        item { NestoraSectionDivider() }
    }

    item {
        DashboardSection {
            DashboardSectionTitle("Work at a glance")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardMetric("New requests", summary.requestedJobs.toString(), Icons.Default.Notifications, Modifier.weight(1f), onOpenBookings)
                DashboardMetric("Active work", summary.activeJobs.toString(), Icons.Default.Settings, Modifier.weight(1f), onOpenBookings)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardMetric("Today", summary.todayJobs.toString(), Icons.Default.DateRange, Modifier.weight(1f), onOpenBookings)
                DashboardMetric("Upcoming", summary.upcomingJobs.toString(), Icons.Default.DateRange, Modifier.weight(1f), onOpenBookings)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardMetric("Completed", summary.completedJobs.toString(), Icons.Default.CheckCircle, Modifier.weight(1f), onOpenBookings)
                DashboardMetric("Ended", summary.endedJobs.toString(), Icons.AutoMirrored.Filled.List, Modifier.weight(1f), onOpenBookings)
            }
        }
    }
    item { NestoraSectionDivider() }

    item {
        DashboardSection {
            DashboardSectionTitle("Business health")
            Spacer(Modifier.height(12.dp))
            DashboardStatusRow(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Listing visibility",
                value = "${summary.activeListings} of ${summary.totalListings} active",
                supporting = if (summary.inactiveListings > 0) "${summary.inactiveListings} inactive" else "All listings are visible",
                onClick = onOpenListings
            )
            DashboardStatusRow(
                icon = Icons.Default.Settings,
                title = "Request availability",
                value = if (summary.isAvailable) "Accepting requests" else "Paused",
                supporting = if (summary.isAvailable) "Customers can request your active services" else "New customer requests are currently unavailable",
                onClick = onOpenAvailability
            )
            DashboardStatusRow(
                icon = Icons.Default.Star,
                title = "Customer rating",
                value = if (summary.reviewCount == 0) "No reviews yet" else String.format(Locale.US, "%.1f from %d", summary.rating, summary.reviewCount),
                supporting = "Based only on recorded customer reviews",
                onClick = onOpenAccount
            )
            DashboardStatusRow(
                icon = Icons.Default.AccountCircle,
                title = "Profile strength",
                value = "${summary.profileScore.coerceIn(0, 100)}% complete",
                supporting = "Verification: ${summary.verificationStatus.lowercase().replaceFirstChar { it.uppercase() }} · ${summary.tier.lowercase().replaceFirstChar { it.uppercase() }} plan",
                onClick = onOpenAccount
            )
            DashboardStatusRow(
                icon = Icons.Default.CheckCircle,
                title = "Response rate",
                value = String.format(Locale.US, "%.0f%%", summary.responseRatePct.coerceIn(0.0, 100.0)),
                supporting = "Respond promptly to improve customer confidence",
                onClick = onOpenBookings
            )
        }
    }
    item { NestoraSectionDivider() }

    item {
        DashboardSection {
            DashboardSectionTitle("Nestora Money")
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenWallet),
                color = if (summary.walletBalance <= -50.0) Color(0xFFFFF1F1) else Color(0xFFF0F8F5),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (summary.walletBalance <= -50.0) Color(0xFFF0B8B8) else Color(0xFFD5E7E0))
            ) {
                Column(Modifier.padding(15.dp)) {
                    Text(
                        String.format(Locale.US, "₹%.2f", summary.walletBalance),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (summary.walletBalance <= -50.0) Color(0xFF9B2C2C) else Color(0xFF164C3D)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (summary.walletBalance <= -50.0)
                            "Booking acceptance is available only when your balance is above -₹50."
                        else "Your balance currently allows you to accept booking requests.",
                        fontSize = 12.sp,
                        color = Color(0xFF63736C)
                    )
                }
            }
        }
    }
    item { NestoraSectionDivider() }

    item {
        DashboardSection {
            DashboardSectionTitle("Manage your business")
            Spacer(Modifier.height(8.dp))
            DashboardActionRow(Icons.Default.Notifications, "Bookings", "Review requests and active work", onOpenBookings)
            DashboardActionRow(Icons.AutoMirrored.Filled.List, "Listings", "Edit service details, pricing and visibility", onOpenListings)
            DashboardActionRow(Icons.Default.DateRange, "Availability", "Set working days and booking hours", onOpenAvailability)
            DashboardActionRow(Icons.Default.Star, "Work items & packages", "Create customer-ready scope and pricing", onOpenPackages)
            DashboardActionRow(Icons.Default.AddCircle, "Add service", "Publish another service offering", onRegisterService)
        }
    }
}

@Composable
private fun DashboardSection(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 16.dp),
        content = content
    )
}

@Composable
private fun DashboardSectionTitle(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17251F))
}

@Composable
private fun DashboardMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAF9)),
        border = BorderStroke(1.dp, Color(0xFFE1E9E6))
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF173D32))
            Text(label, fontSize = 11.sp, color = Color(0xFF63736C))
        }
    }
}

@Composable
private fun AttentionRow(item: ProviderDashboardAttention, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color(0xFFFFF8E8),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF0DDA8))
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFA56A00), modifier = Modifier.size(21.dp))
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF503B12))
                Text(item.message, fontSize = 11.sp, lineHeight = 15.sp, color = Color(0xFF765F2B))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFA56A00), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DashboardStatusRow(
    icon: ImageVector,
    title: String,
    value: String,
    supporting: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).background(Color(0xFFF0F6F3), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontSize = 12.sp, color = Color(0xFF63736C))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17251F))
            Text(supporting, fontSize = 10.sp, color = Color(0xFF7A8983))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF8A9892), modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun DashboardActionRow(icon: ImageVector, title: String, supporting: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17251F))
            Text(supporting, fontSize = 11.sp, color = Color(0xFF63736C))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF8A9892), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProviderDashboardLoading() {
    DashboardSection {
        Text("Loading your latest provider overview…", fontSize = 13.sp, color = Color(0xFF63736C))
        Spacer(Modifier.height(14.dp))
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(64.dp).background(Color(0xFFF0F3F2), RoundedCornerShape(12.dp)))
            if (it != 2) Spacer(Modifier.height(10.dp))
        }
    }
}
