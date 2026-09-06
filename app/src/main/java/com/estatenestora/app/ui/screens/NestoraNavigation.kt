package com.estatenestora.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NavigationGreen = Color(0xFF064E3B)
private val NavigationSelectedSurface = Color(0xFFE7F3EE)
private val NavigationMuted = Color(0xFF64748B)

enum class NestoraPrimaryDestination(
    val label: String,
    val icon: ImageVector
) {
    Dashboard("Dashboard", Icons.Default.Home),
    Register("Register", Icons.Default.Build),
    Listings("Listings", Icons.AutoMirrored.Filled.List),
    Bookings("Bookings", Icons.Default.DateRange),
    Explore("Explore", Icons.Default.Home),
    Finder("Finder", Icons.Default.Search),
    Account("Account", Icons.Default.Person)
}

enum class ProviderListingTool(
    val label: String,
    val description: String,
    val sectionId: String
) {
    Availability(
        "Availability",
        "Set working days and booking hours for each listing.",
        "availability"
    ),
    Packages(
        "Work items & packages",
        "Define clear scope and pricing for each service type.",
        "packages"
    ),
    Settings(
        "Listing settings",
        "Review customer matching and listing visibility controls.",
        "settings"
    )
}

data class BookingWorkspaceView(val tabIndex: Int, val label: String)

fun providerListingTools(): List<ProviderListingTool> = listOf(
    ProviderListingTool.Availability,
    ProviderListingTool.Packages,
    ProviderListingTool.Settings
)

fun providerListingSectionOrDefault(sectionId: String): String =
    when (sectionId) {
        "manage", "availability", "packages", "settings" -> sectionId
        else -> "listings"
    }

fun bookingWorkspaceViews(isProviderMode: Boolean): List<BookingWorkspaceView> = listOf(
    BookingWorkspaceView(1, if (isProviderMode) "Active requests" else "Active bookings"),
    BookingWorkspaceView(0, "History")
)

fun primaryDestinationsFor(isProviderMode: Boolean): List<NestoraPrimaryDestination> =
    if (isProviderMode) {
        listOf(
            NestoraPrimaryDestination.Dashboard,
            NestoraPrimaryDestination.Register,
            NestoraPrimaryDestination.Listings,
            NestoraPrimaryDestination.Bookings,
            NestoraPrimaryDestination.Account
        )
    } else {
        listOf(
            NestoraPrimaryDestination.Explore,
            NestoraPrimaryDestination.Finder,
            NestoraPrimaryDestination.Account
        )
    }

fun primaryDestinationFor(
    isProviderMode: Boolean,
    activeScreen: String,
    selectedTab: Int
): NestoraPrimaryDestination = if (isProviderMode) {
    when (activeScreen) {
        "dashboard" -> NestoraPrimaryDestination.Dashboard
        "register_choice", "register_service", "auto_register" -> NestoraPrimaryDestination.Register
        "listings" -> NestoraPrimaryDestination.Listings
        else -> if (selectedTab == 3) {
            NestoraPrimaryDestination.Account
        } else {
            NestoraPrimaryDestination.Bookings
        }
    }
} else {
    when (selectedTab) {
        1 -> NestoraPrimaryDestination.Finder
        3 -> NestoraPrimaryDestination.Account
        else -> NestoraPrimaryDestination.Explore
    }
}

fun shouldShowPrimaryNavigation(
    isProviderMode: Boolean,
    activeScreen: String,
    selectedTab: Int,
    selectedRegisterTab: Int = 0,
    selectedFinderTab: Int = 0,
    nestedPageOpen: Boolean = false
): Boolean = if (isProviderMode) {
    !nestedPageOpen && when (activeScreen) {
        "dashboard", "listings" -> true
        "register_choice" -> selectedRegisterTab == 0
        "main" -> selectedTab == 2 || selectedTab == 3
        else -> false
    }
} else {
    activeScreen == "main" && when (selectedTab) {
        0, 3 -> true
        1 -> selectedFinderTab == 0
        else -> false
    }
}

fun isProviderFullScreenDestination(activeScreen: String, selectedTab: Int): Boolean =
    activeScreen in listOf("register_choice", "register_service", "auto_register", "listings") ||
        (activeScreen == "main" && selectedTab == 2)

fun resolveScrollAwareFilterVisibility(
    previousIndex: Int,
    previousOffset: Int,
    currentIndex: Int,
    currentOffset: Int,
    currentlyVisible: Boolean
): Boolean {
    if (currentIndex == 0 && currentOffset == 0) return true
    val movingDown = currentIndex > previousIndex ||
        (currentIndex == previousIndex && currentOffset > previousOffset)
    val movingUp = currentIndex < previousIndex ||
        (currentIndex == previousIndex && currentOffset < previousOffset)
    return when {
        movingUp -> true
        movingDown -> false
        else -> currentlyVisible
    }
}

@Composable
fun NestoraPrimaryNavigationBar(
    isProviderMode: Boolean,
    selectedDestination: NestoraPrimaryDestination,
    onDestinationSelected: (NestoraPrimaryDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            primaryDestinationsFor(isProviderMode).forEach { destination ->
                val isSelected = destination == selectedDestination
                val itemBackground by animateColorAsState(
                    targetValue = if (isSelected) NavigationSelectedSurface else Color.Transparent,
                    label = "primaryNavigationBackground"
                )
                val itemColor by animateColorAsState(
                    targetValue = if (isSelected) NavigationGreen else NavigationMuted,
                    label = "primaryNavigationContent"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .background(itemBackground, RoundedCornerShape(14.dp))
                        .semantics {
                            selected = isSelected
                        }
                        .clickable(
                            role = Role.Tab,
                            onClick = { onDestinationSelected(destination) }
                        )
                        .padding(top = 5.dp, bottom = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val strings = com.estatenestora.app.ui.theme.LocalNestoraStrings.current
                    val localizedLabel = when (destination) {
                        NestoraPrimaryDestination.Explore -> strings.navExplore
                        NestoraPrimaryDestination.Finder -> strings.navFinder
                        NestoraPrimaryDestination.Account -> strings.navAccount
                        NestoraPrimaryDestination.Dashboard -> strings.navDashboard
                        NestoraPrimaryDestination.Register -> strings.navRegister
                        NestoraPrimaryDestination.Listings -> strings.navListings
                        NestoraPrimaryDestination.Bookings -> strings.navBookings
                    }
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = localizedLabel,
                        tint = itemColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = localizedLabel,
                        color = itemColor,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(3.dp)
                            .background(
                                if (isSelected) NavigationGreen else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}
