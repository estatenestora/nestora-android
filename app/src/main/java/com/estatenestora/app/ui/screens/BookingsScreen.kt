package com.estatenestora.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.drawWithContent

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.BookingSummary
import com.estatenestora.app.ui.theme.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

@Composable
fun BookingsBottomCalendarIcon(isSelected: Boolean, modifier: Modifier = Modifier) {
    val calendar = java.util.Calendar.getInstance()
    val today = calendar.get(java.util.Calendar.DAY_OF_MONTH).toString()
    val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.US)
    val month = monthFormat.format(calendar.time).uppercase()

    Card(
        modifier = modifier.size(22.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF00382B) else Color(0xFF888888))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().background(Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(Color(0xFFFF5252))
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = today,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00382B),
                    lineHeight = 7.sp
                )
                Text(
                    text = month,
                    fontSize = 4.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8FA7A0),
                    lineHeight = 4.sp
                )
            }
        }
    }
}

private fun Modifier.drawLeftBorder(color: Color, width: androidx.compose.ui.unit.Dp): Modifier =
    this.then(Modifier.drawWithContent {
        drawContent()
        drawRect(color = color, size = androidx.compose.ui.geometry.Size(width.toPx(), size.height))
    })

// ── Filter model ─────────────────────────────────────────────────────────────

/**
 * Holds the full set of active filter selections for the booking list.
 * Each field is nullable/empty = no filter applied for that dimension.
 */
data class BookingFilterState(
    val stages: Set<String> = emptySet(),          // e.g. {"DONE","CANCELLED"}
    val isHomeService: Boolean? = null,            // true=home-service only, false=on-site only
    val sortOrder: String = "newest",              // "newest" | "oldest" | "highest_fee" | "lowest_fee"
    val minFee: Double? = null,
    val maxFee: Double? = null
) {
    val activeCount: Int get() = listOf(
        stages.isNotEmpty(),
        isHomeService != null,
        sortOrder != "newest",
        minFee != null,
        maxFee != null
    ).count { it }
}

fun List<BookingSummary>.applyBookingFilter(f: BookingFilterState): List<BookingSummary> {
    var result = this
    if (f.stages.isNotEmpty()) result = result.filter { it.stage in f.stages }
    if (f.isHomeService != null) result = result.filter { it.isHomeService == f.isHomeService }
    if (f.minFee != null) result = result.filter { it.serviceFee >= f.minFee }
    if (f.maxFee != null) result = result.filter { it.serviceFee <= f.maxFee }
    result = when (f.sortOrder) {
        "oldest"      -> result.sortedBy { it.updatedAt }
        "highest_fee" -> result.sortedByDescending { it.serviceFee }
        "lowest_fee"  -> result.sortedBy { it.serviceFee }
        else          -> result.sortedByDescending { it.updatedAt }  // newest
    }
    return result
}

// ── Filter bottom sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFilterSheet(
    current: BookingFilterState,
    initialCategory: String = "Booking Status",
    onApply: (BookingFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(current) { mutableStateOf(current) }
    var pendingCategory by remember { mutableStateOf(initialCategory) }

    val allStages = listOf(
        "REQUESTED"   to "Requested",
        "CONFIRMED"   to "Confirmed",
        "IN_PROGRESS" to "In Progress",
        "PAYMENT"     to "Payment Due",
        "DONE"        to "Completed",
        "CANCELLED"   to "Cancelled",
        "ENDED"       to "Ended"
    )
    val sortOptions = listOf(
        "newest"      to "Newest first",
        "oldest"      to "Oldest first",
        "highest_fee" to "Highest fee first",
        "lowest_fee"  to "Lowest fee first"
    )

    FilterOverlaySheet(
        title = "Filter bookings",
        onDismissRequest = onDismiss
    ) {
            val filterNavItems = listOf("Booking Status", "Service Type", "Fee Range")
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                // Left pane: Navigation
                LazyColumn(
                    modifier = Modifier
                        .width(135.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFF5F6F8))
                ) {
                    items(filterNavItems) { nav ->
                        val isSelected = pendingCategory == nav
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingCategory = nav }
                                .background(if (isSelected) Color.White else Color(0xFFF5F6F8))
                                .then(if (isSelected) Modifier.drawLeftBorder(NestoraMint, 3.dp) else Modifier)
                                .padding(vertical = 16.dp, horizontal = 14.dp)
                        ) {
                            Text(
                                text = nav,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NestoraMint else Color(0xFF555555)
                            )
                        }
                    }
                }

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = FilterPaneDividerColor
                )

                // Right pane: Options Detail
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (pendingCategory) {
                        "Sort" -> {
                            Text("SORT BY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            sortOptions.forEach { (id, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { draft = draft.copy(sortOrder = id) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = draft.sortOrder == id,
                                        onClick = { draft = draft.copy(sortOrder = id) },
                                        colors = RadioButtonDefaults.colors(selectedColor = NestoraMint)
                                    )
                                    Text(label, fontSize = 13.sp, color = Color(0xFF333333))
                                }
                            }
                        }

                        "Booking Status" -> {
                            Text("BOOKING STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allStages) { (id, label) ->
                                    val selected = id in draft.stages
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                draft = draft.copy(
                                                    stages = if (selected) draft.stages - id else draft.stages + id
                                                )
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Checkbox(
                                            checked = selected,
                                            onCheckedChange = {
                                                draft = draft.copy(
                                                    stages = if (selected) draft.stages - id else draft.stages + id
                                                )
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = NestoraMint)
                                        )
                                        Text(label, fontSize = 13.sp, color = Color(0xFF333333))
                                    }
                                }
                            }
                        }

                        "Service Type" -> {
                            Text("SERVICE TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            listOf(
                                null to "All Services",
                                true to "Home Service",
                                false to "On-site Service"
                            ).forEach { (value, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { draft = draft.copy(isHomeService = value) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = draft.isHomeService == value,
                                        onClick = { draft = draft.copy(isHomeService = value) },
                                        colors = RadioButtonDefaults.colors(selectedColor = NestoraMint)
                                    )
                                    Text(label, fontSize = 13.sp, color = Color(0xFF333333))
                                }
                            }
                        }

                        "Fee Range" -> {
                            Text("FEE RANGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            listOf(
                                null to null to "Any",
                                null to 500.0 to "Under 500",
                                500.0 to 2000.0 to "500-2000",
                                2000.0 to 5000.0 to "2000-5000",
                                5000.0 to null to "Above 5000"
                            ).forEach { (range, label) ->
                                val (min, max) = range
                                val selected = draft.minFee == min && draft.maxFee == max
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { draft = draft.copy(minFee = min, maxFee = max) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { draft = draft.copy(minFee = min, maxFee = max) },
                                        colors = RadioButtonDefaults.colors(selectedColor = NestoraMint)
                                    )
                                    Text(label, fontSize = 13.sp, color = Color(0xFF333333))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)

            // Bottom CTA row (Clear / Apply)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        draft = BookingFilterState()
                        onApply(BookingFilterState())
                    }
                ) {
                    Text("Clear Filters", fontSize = 14.sp, color = Color.Gray)
                }
                Button(
                    onClick = { onApply(draft) },
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(130.dp)
                ) {
                    Text(
                        text = if (draft.activeCount == 0) "Apply" else "Apply (${draft.activeCount})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun standardTopHeroHeight(): Dp =
    (LocalConfiguration.current.screenHeightDp.dp * 0.30f).coerceIn(220.dp, 290.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    theme: String = "home",
    canvasColor: Color = Color.Transparent,
    carouselHeight: Dp = 172.dp,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 4.dp,
    cornerRadius: Dp = 22.dp,
    managedBanners: List<com.estatenestora.app.data.model.MediaAsset> = emptyList(),
    onResolveMedia: suspend (String) -> String? = { null }
) {
    // Each section shows contextually-relevant banners
    // Direct unique Unsplash URLs per slide — no keyword lookup, guaranteed unique images
    data class CarouselBanner(val title: String, val subtitle: String, val imageUrl: String = "", val telegramFileId: String = "", val actionLabel: String = "")
    val fallbackBanners = remember(theme) {
        when (theme) {
            "dashboard" -> listOf(
                CarouselBanner("Run Your Day with Confidence", "Track requests, schedules and active work from one provider dashboard",
                    "https://images.unsplash.com/photo-1521791136064-7986c2920216?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Keep Availability Accurate", "Open only the times you can serve and avoid booking conflicts",
                    "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Grow with Better Listings", "Clear pricing and service details help customers choose with confidence",
                    "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800&h=400&fit=crop&q=85")
            )
            "bookings" -> listOf(
                CarouselBanner("Track All Your Bookings", "Real-time status updates for every service you've booked",
                    "https://images.unsplash.com/photo-1586880244386-8b3e34c8382c?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Instant Cancellation", "Cancel for free up to 2 hours before the service starts",
                    "https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Rate & Review Providers", "Your feedback helps others find the best service providers",
                    "https://images.unsplash.com/photo-1521791136064-7986c2920216?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Need Help? Chat with AI", "Our Nestora AI will resolve any booking dispute instantly",
                    "https://images.unsplash.com/photo-1531746790731-6c087fecd65a?w=800&h=400&fit=crop&q=85")
            )
            "register" -> listOf(
                CarouselBanner("Earn ₹30,000/Month", "Register your service and start receiving verified customer bookings",
                    "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Zero Commission for 30 Days", "Join Nestora and list your service for free for the first month",
                    "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Get Verified Badge", "Complete profile verification to unlock more customer visibility",
                    "https://images.unsplash.com/photo-1600880292203-757bb62b4baf?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("AI Listing Assistant", "Let Nestora AI create your perfect service listing in minutes",
                    "https://images.unsplash.com/photo-1677442135703-1787eea5ce01?w=800&h=400&fit=crop&q=85")
            )
            "finder" -> listOf(
                CarouselBanner("Find Services Near You", "Discover local plumbers, electricians, maids & more in your area",
                    "https://images.unsplash.com/photo-1551434678-e076c223a692?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Zero Brokerage Rentals", "Browse verified flats with no middleman — direct from owners",
                    "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Book in 30 Seconds", "Pick a provider, choose your slot — done. No calls needed",
                    "https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Safe & Verified Providers", "Every Nestora provider is background-verified before listing",
                    "https://images.unsplash.com/photo-1579621970563-ebec7560ff3e?w=800&h=400&fit=crop&q=85")
            )
            else -> listOf( // "home" / explore
                CarouselBanner("50% OFF Rent Deposit", "Zero Brokerage Rentals with secure digital agreements",
                    "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Instant Home Services", "Book Plumbers & Electricians in under 30 mins",
                    "https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Verified Maids & Cooks", "Background-verified domestic helpers from ₹199/day",
                    "https://images.unsplash.com/photo-1563453392212-326f5e854473?w=800&h=400&fit=crop&q=85"),
                CarouselBanner("Find Expert Tutors", "Home & online tutors for all subjects and grade levels",
                    "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=800&h=400&fit=crop&q=85")
            )
        }
    }
    val banners = remember(managedBanners, fallbackBanners) {
        managedBanners
            .filter { it.scope == "APP_CAROUSEL" && it.status == "ACTIVE" }
            .sortedBy { it.displayOrder }
            .mapNotNull { asset ->
                asset.fileIdFor("HERO")?.let { fileId ->
                    CarouselBanner(asset.title, asset.subtitle, telegramFileId = fileId, actionLabel = asset.actionLabel)
                }
            }
            .ifEmpty { fallbackBanners }
    }

    val actionLabel = when (theme) {
        "dashboard" -> "View work"
        "bookings" -> "View Bookings"
        "register" -> "Register Now"
        "finder" -> "Find Now"
        else -> "Book Now"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(canvasColor)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
        ) {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { banners.size })

            // Auto-scroll every 4 seconds
            LaunchedEffect(pagerState) {
                while (true) {
                    kotlinx.coroutines.delay(4000)
                    val next = (pagerState.currentPage + 1) % banners.size
                    pagerState.animateScrollToPage(next)
                }
            }

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(carouselHeight)
            ) { page ->
            val banner = banners[page]
            var managedPath by remember(banner.telegramFileId) { mutableStateOf<String?>(null) }
            LaunchedEffect(banner.telegramFileId) {
                managedPath = banner.telegramFileId.takeIf(String::isNotBlank)?.let { onResolveMedia(it) }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = managedPath ?: banner.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.42f),
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.70f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = banner.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = banner.subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.90f),
                        lineHeight = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = banner.actionLabel.ifBlank { actionLabel },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clickable { }
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }

            // Page indicator dots
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(banners.size) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == pagerState.currentPage) 20.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (i == pagerState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.5f)
                            )
                        )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookingsScreen(
    bookings: List<BookingSummary>,
    viewerUserId: String?,
    hasProviderListings: Boolean,
    onBookingClick: (BookingSummary) -> Unit = {},
    onPayClick: (BookingSummary) -> Unit,
    onCancelClick: (BookingSummary) -> Unit,
    currentLocation: String? = null,
    onSelectLocationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onRegisterServiceClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onFindServiceClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onScrollChanged: (Boolean) -> Unit = {},
    userPhotoPath: String? = null,
    onRebookClick: (com.estatenestora.app.data.model.ServiceListing) -> Unit = {},
    isProviderMode: Boolean = false,
    onModeToggle: () -> Unit = {},
    tabsList: List<com.estatenestora.app.ui.theme.NestoraTab> = emptyList(),
    selectedTabId: String = "bookings",
    onTabSelected: (String) -> Unit = {},
    currentTheme: com.estatenestora.app.ui.theme.RoyalTheme = com.estatenestora.app.ui.theme.RoyalThemeRepository.getThemeForToday(),
    showHomeChrome: Boolean = true
) {
    // Concluded = stage reached a terminal state (DONE, CANCELLED, or ENDED).
    // Active/in-flight = everything else.
    val concludedStages = setOf("DONE", "CANCELLED", "ENDED")

    // Hire mode: "Bookings" tab shows concluded sent bookings; "Sent" shows active sent bookings.
    // Serve mode: "Bookings" tab shows concluded received bookings; "Received" shows active received ones.
    // GET_MY_BOOKINGS is already scoped by the backend and carries viewer_role.
    // Prefer that authoritative field: profile loading can briefly lag behind
    // the list response, and its UUID must never hide a real booking.
    val roleBookings = remember(bookings, viewerUserId, isProviderMode) {
        bookings.filter { bookingBelongsToSelectedRole(it, viewerUserId, isProviderMode) }
    }
    val concludedBookings = remember(roleBookings) {
        roleBookings.filter { it.stage in concludedStages }
    }
    val activeRoleBookings = remember(roleBookings) {
        roleBookings.filter { it.stage !in concludedStages }
    }
    val providerRoleBookings = remember(roleBookings, isProviderMode) {
        if (isProviderMode) roleBookings else emptyList()
    }

    // selectedBookingsTab: 0 = Bookings (concluded), 1 = Sent/Received (active)
    var selectedBookingsTab by remember { mutableStateOf(1) }
    var bookingViewMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }
    
    // Filter sheet states
    var filterState by remember { mutableStateOf(BookingFilterState()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterSheetCategory by remember { mutableStateOf("Booking Status") }

    var isBottomBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 12f && !isBottomBarVisible) {
                    isBottomBarVisible = true
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y < -12f && isBottomBarVisible) {
                    isBottomBarVisible = false
                } else if (consumed.y + available.y > 12f && !isBottomBarVisible) {
                    isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(selectedBookingsTab) {
        isBottomBarVisible = true
        filterState = BookingFilterState()
    }

    LaunchedEffect(selectedBookingsTab, searchQuery) {
        currentPage = 0
    }

    val showLegacyBookingsBottomBar = false
    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showLegacyBookingsBottomBar) {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2EAF2))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 0 — Bookings (concluded bookings for this role)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedBookingsTab = 0 }
                        ) {
                            BookingsBottomCalendarIcon(
                                isSelected = selectedBookingsTab == 0,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Bookings",
                                fontSize = 9.sp,
                                fontWeight = if (selectedBookingsTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedBookingsTab == 0) NestoraMint else Color(0xFF8FA7A0)
                            )
                        }

                        // Tab 1 — Sent (Hire mode) or Received (Serve mode): active/in-flight bookings
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedBookingsTab = 1 }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isProviderMode) Icons.Default.Email else Icons.Default.Send,
                                contentDescription = if (isProviderMode) "Received" else "Sent",
                                tint = if (selectedBookingsTab == 1) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = if (isProviderMode) "Received" else "Sent",
                                fontSize = 9.sp,
                                fontWeight = if (selectedBookingsTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedBookingsTab == 1) NestoraMint else Color(0xFF888888)
                            )
                        }
                    }
                }
            }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(isSearchFocused) {
                if (isSearchFocused) listState.animateScrollToItem(1)
            }
            val isScrolled by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0
                }
            }
            LaunchedEffect(isScrolled) {
                if (!isScrolled) {
                    isBottomBarVisible = true
                }
            }

            val lastVisibleItemIndex = remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val visibleItemsInfo = layoutInfo.visibleItemsInfo
                    if (visibleItemsInfo.isEmpty()) 0
                    else visibleItemsInfo.last().index
                }
            }

            LaunchedEffect(lastVisibleItemIndex.value) {
                val totalItems = listState.layoutInfo.totalItemsCount
                if (totalItems > 0 && lastVisibleItemIndex.value >= totalItems - 2) {
                    val currentListSize = if (isProviderMode) {
                        providerRoleBookings.size
                    } else when (selectedBookingsTab) {
                        0 -> {
                            bookings.count {
                                searchQuery.isBlank() || it.listingTitle.contains(searchQuery, ignoreCase = true) || it.referenceCode.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        1 -> activeRoleBookings.size
                        else -> 0
                    }
                    val pageSize = 5
                    if ((currentPage + 1) * pageSize < currentListSize) {
                        currentPage++
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF6F8F7)),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 1. Shared top bar
                if (showHomeChrome) {
                    item {
                        OnboardingTopBar(
                            currentLocation = currentLocation,
                            onSelectLocationClick = onSelectLocationClick,
                            onProfileClick = onProfileClick,
                            userPhotoPath = userPhotoPath,
                            isProviderMode = isProviderMode,
                            onModeToggle = onModeToggle,
                            tabsList = tabsList,
                            selectedTabId = selectedTabId,
                            onTabSelected = onTabSelected,
                            currentTheme = currentTheme
                        )
                    }
                } else {
                    item {
                        NestoraWorkspaceHeader(
                            icon = Icons.Default.DateRange,
                            title = "Bookings"
                        )
                    }
                }

                if (showHomeChrome) item { NestoraSectionDivider() }

                // The selected Bookings canvas ends with useful Nestora context,
                // not with controls. Booking operations remain in the neutral
                // section below, where they are easier to scan and use.
                if (showHomeChrome) item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = if (isProviderMode) "Manage customer bookings" else "Track your bookings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10231B)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isProviderMode) {
                                "Review incoming requests, follow active work and conclude every service from one place."
                            } else {
                                "See each request, payment and service update clearly from booking to completion."
                            },
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                item { NestoraSectionDivider() }

                // Booking statistics and filters deliberately sit below the
                // highlighted canvas in a neutral, task-focused surface.
                item {
                    val headerText = when {
                        isProviderMode -> "Bookings"
                        selectedBookingsTab == 0 -> "Bookings"
                        else -> "Sent"
                    }
                    val descText = when {
                        isProviderMode ->
                            "Review every customer request from arrival through completion"
                        selectedBookingsTab == 0 ->
                            "Completed and cancelled services you booked"
                        isProviderMode ->
                            "Active requests from customers not yet concluded"
                        else ->
                            "Services you booked that are still in progress"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            color = Color.White,
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = if (isProviderMode) 0.dp else 18.dp
                                )
                            ) {
                        if (!isProviderMode) Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = headerText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0D1A13)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = descText,
                                    fontSize = 13.sp,
                                    color = NestoraTextMuted
                                )
                            }
                            if (!isProviderMode) {
                            Box {
                                Surface(
                                    onClick = { bookingViewMenuExpanded = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0F4F2),
                                    border = BorderStroke(1.dp, Color(0xFFD8E3DE))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (selectedBookingsTab == 1) "Active" else "History",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NestoraMint
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Change booking view",
                                            tint = NestoraMint,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = bookingViewMenuExpanded,
                                    onDismissRequest = { bookingViewMenuExpanded = false }
                                ) {
                                    bookingWorkspaceViews(isProviderMode).forEach { view ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = view.label,
                                                    fontWeight = if (selectedBookingsTab == view.tabIndex) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedBookingsTab = view.tabIndex
                                                bookingViewMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            }
                        }

                        // Stats row — only on the Bookings (concluded) view
                        // Filter buttons row (styled like ss1)
                        Spacer(Modifier.height(NestoraFilterPanelSpacing))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Filter Button
                            Surface(
                                onClick = {
                                    filterSheetCategory = "Booking Status"
                                    showFilterSheet = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (filterState.activeCount > 0) NestoraMint else Color(0xFFDDE2E9)),
                                color = if (filterState.activeCount > 0) Color(0xFFE8FAF4) else Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (filterState.activeCount > 0) "Filter (${filterState.activeCount})" else "Filter",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (filterState.activeCount > 0) NestoraMint else Color(0xFF333333)
                                    )
                                }
                            }

                            // 3. Status Button
                            val isStatusFiltered = filterState.stages.isNotEmpty()
                            Surface(
                                onClick = {
                                    filterSheetCategory = "Booking Status"
                                    showFilterSheet = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isStatusFiltered) NestoraMint else Color(0xFFDDE2E9)),
                                color = if (isStatusFiltered) Color(0xFFE8FAF4) else Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isStatusFiltered) "Status (${filterState.stages.size})" else "Status",
                                        fontSize = 12.sp,
                                        color = if (isStatusFiltered) NestoraMint else Color(0xFF333333)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (isStatusFiltered) NestoraMint else Color(0xFF333333),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // 4. Service Type Button
                            val isServiceTypeFiltered = filterState.isHomeService != null
                            Surface(
                                onClick = {
                                    filterSheetCategory = "Service Type"
                                    showFilterSheet = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isServiceTypeFiltered) NestoraMint else Color(0xFFDDE2E9)),
                                color = if (isServiceTypeFiltered) Color(0xFFE8FAF4) else Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val serviceTypeText = when (filterState.isHomeService) {
                                        true -> "Home Service"
                                        false -> "On-site"
                                        else -> "Service Type"
                                    }
                                    Text(
                                        text = serviceTypeText,
                                        fontSize = 12.sp,
                                        color = if (isServiceTypeFiltered) NestoraMint else Color(0xFF333333)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (isServiceTypeFiltered) NestoraMint else Color(0xFF333333),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // 5. Fee Range Button
                            val isFeeFiltered = filterState.minFee != null || filterState.maxFee != null
                            Surface(
                                onClick = {
                                    filterSheetCategory = "Fee Range"
                                    showFilterSheet = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isFeeFiltered) NestoraMint else Color(0xFFDDE2E9)),
                                color = if (isFeeFiltered) Color(0xFFE8FAF4) else Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val feeText = when {
                                        filterState.minFee == null && filterState.maxFee == 500.0 -> "Under 500"
                                        filterState.minFee == 500.0 && filterState.maxFee == 2000.0 -> "500-2000"
                                        filterState.minFee == 2000.0 && filterState.maxFee == 5000.0 -> "2000-5000"
                                        filterState.minFee == 5000.0 -> "Above 5000"
                                        else -> "Fee Range"
                                    }
                                    Text(
                                        text = feeText,
                                        fontSize = 12.sp,
                                        color = if (isFeeFiltered) NestoraMint else Color(0xFF333333)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (isFeeFiltered) NestoraMint else Color(0xFF333333),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (isProviderMode || selectedBookingsTab == 0) {
                            Spacer(Modifier.height(NestoraFilterPanelSpacing))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val statsBookings = if (isProviderMode) providerRoleBookings else concludedBookings
                                StatPill(label = "${statsBookings.size}", sublabel = "Total")
                                StatPill(
                                    label = "${statsBookings.count { it.stage !in concludedStages }}",
                                    sublabel = "In Progress"
                                )
                                StatPill(
                                    label = "${statsBookings.count { it.stage == "DONE" }}",
                                    sublabel = "Completed"
                                )
                                StatPill(
                                    label = "${statsBookings.count { it.stage == "CANCELLED" }}",
                                    sublabel = "Cancelled"
                                )
                            }
                        }
                        Spacer(Modifier.height(NestoraFilterPanelSpacing))
                            }
                        }
                    }
                }

                // 5. Tab content — 2-tab: 0=Bookings(concluded), 1=Sent/Received(active)
                item { NestoraSectionDivider() }

                if (isProviderMode) {
                    val baseList = providerRoleBookings.filter {
                        searchQuery.isBlank() ||
                            it.listingTitle.contains(searchQuery, ignoreCase = true) ||
                            it.referenceCode.contains(searchQuery, ignoreCase = true)
                    }
                    val filteredList = baseList.applyBookingFilter(filterState)
                    val paginatedList = filteredList.take((currentPage + 1) * 5)

                    if (!shouldShowProviderInbox(hasProviderListings, providerRoleBookings.size)) {
                        item {
                            BookingsEmptyState(
                                emoji = "",
                                title = "Become a Provider",
                                body = "Register a service to start receiving booking requests from customers.",
                                actionLabel = "Register a Service",
                                onAction = onRegisterServiceClick
                            )
                        }
                    } else if (filteredList.isEmpty()) {
                        item {
                            BookingsEmptyState(
                                emoji = "",
                                title = "No Matching Bookings",
                                body = "New and previous customer bookings will appear here. Adjust filters to see more."
                            )
                        }
                    } else {
                        items(paginatedList, key = { it.id }) { booking ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                BookingCard(
                                    booking = booking,
                                    viewerUserId = viewerUserId,
                                    onClick = { onBookingClick(booking) },
                                    onPayClick = onPayClick,
                                    onCancelClick = onCancelClick,
                                    onRebookClick = onRebookClick
                                )
                            }
                        }
                    }
                } else when (selectedBookingsTab) {
                    0 -> {
                        // Concluded bookings for this role, filtered by search + sheet filters
                        val baseList = concludedBookings.filter {
                            searchQuery.isBlank() ||
                                it.listingTitle.contains(searchQuery, ignoreCase = true) ||
                                it.referenceCode.contains(searchQuery, ignoreCase = true)
                        }
                        val filteredList = baseList.applyBookingFilter(filterState)
                        val pageSize = 5
                        val paginatedList = filteredList.take((currentPage + 1) * pageSize)

                        if (filteredList.isEmpty()) {
                            item {
                                BookingsEmptyState(
                                    emoji = "",
                                    title = "No Bookings Yet",
                                    body = "Completed and cancelled bookings will appear here."
                                )
                            }
                        } else {
                            items(paginatedList, key = { it.id }) { booking ->
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                    BookingCard(
                                        booking = booking,
                                        viewerUserId = viewerUserId,
                                        onClick = { onBookingClick(booking) },
                                        onPayClick = onPayClick,
                                        onCancelClick = onCancelClick,
                                        onRebookClick = onRebookClick
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Active/in-flight bookings for this role, filtered by search + sheet filters
                        val baseList = activeRoleBookings.filter {
                            searchQuery.isBlank() ||
                                it.listingTitle.contains(searchQuery, ignoreCase = true) ||
                                it.referenceCode.contains(searchQuery, ignoreCase = true)
                        }
                        val filteredList = baseList.applyBookingFilter(filterState)
                        val pageSize = 5
                        val paginatedList = filteredList.take((currentPage + 1) * pageSize)

                        if (isProviderMode && !shouldShowProviderInbox(hasProviderListings, activeRoleBookings.size)) {
                            item {
                                BookingsEmptyState(
                                    emoji = "",
                                    title = "Become a Provider",
                                    body = "Register a service to start receiving booking requests from customers.",
                                    actionLabel = "Register a Service",
                                    onAction = onRegisterServiceClick
                                )
                            }
                        } else if (filteredList.isEmpty()) {
                            item {
                                BookingsEmptyState(
                                    emoji = "",
                                    title = if (isProviderMode) "No Active Requests" else "No Active Bookings",
                                    body = if (isProviderMode)
                                        "Active booking requests from customers will appear here."
                                    else
                                        "Services you have booked that are in progress will appear here."
                                )
                            }
                        } else {
                            items(paginatedList, key = { it.id }) { booking ->
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                    BookingCard(
                                        booking = booking,
                                        viewerUserId = viewerUserId,
                                        onClick = { onBookingClick(booking) },
                                        onPayClick = onPayClick,
                                        onCancelClick = onCancelClick,
                                        onRebookClick = onRebookClick
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }

            FloatingSearchOverlay(
                visible = isSearchFocused,
                query = searchQuery,
                title = "Search your bookings",
                onQueryChange = { searchQuery = it },
                onDismiss = { isSearchFocused = false }
            )

        }
    }

    if (showFilterSheet) {
        BookingFilterSheet(
            current = filterState,
            initialCategory = filterSheetCategory,
            onApply = { newFilter ->
                filterState = newFilter
                showFilterSheet = false
                currentPage = 0
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun BookingsEmptyState(
    emoji: String,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (emoji.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8FAF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 32.sp)
                    }
                }
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1A13))
                Text(
                    text = body,
                    fontSize = 13.sp,
                    color = NestoraTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                if (actionLabel != null) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatPill(label: String, sublabel: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00382B))
            Text(sublabel, fontSize = 10.sp, color = Color(0xFF3E5C50), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BookingCard(
    booking: BookingSummary,
    viewerUserId: String?,
    onClick: () -> Unit = {},
    onPayClick: (BookingSummary) -> Unit,
    onCancelClick: (BookingSummary) -> Unit,
    onRebookClick: (com.estatenestora.app.data.model.ServiceListing) -> Unit = {},
    isProviderMode: Boolean = false,
    onModeToggle: () -> Unit = {},
    tabsList: List<com.estatenestora.app.ui.theme.NestoraTab> = emptyList(),
    selectedTabId: String = "bookings",
    onTabSelected: (String) -> Unit = {},
    currentTheme: com.estatenestora.app.ui.theme.RoyalTheme = com.estatenestora.app.ui.theme.RoyalThemeRepository.getThemeForToday()
) {
    // The backend supplies viewer_role with every list row. Falling back to
    // IDs keeps older cached rows compatible while avoiding a wrong card when
    // the profile request has not completed yet.
    val isViewerProvider = booking.viewerRole.equals("PROVIDER", ignoreCase = true) ||
        (booking.viewerRole.isBlank() && viewerUserId != null && booking.providerUserId == viewerUserId)
    val counterpartLabel = if (isViewerProvider) "Customer" else "Provider"
    val counterpartName = if (isViewerProvider) booking.customerName else booking.providerName
    val isCustomer = viewerUserId != null && booking.customerUserId == viewerUserId
    val nextStepMessage = if (isViewerProvider) booking.providerMessage else booking.customerMessage

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, Color(0xFFF0F2F5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row (Restaurant/Provider Image, Title, Location, Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Rounded image of service
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    AsyncImage(
                        model = getRealLifeImageUrl(booking.listingTitle),
                        contentDescription = booking.listingTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Middle: Title & Location
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.listingTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1C1C1C),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "ORDER #${booking.referenceCode.ifBlank { booking.id.take(8) }} • $counterpartName",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Right: Status indicator with icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    val statusColor = when (booking.stage) {
                        "DONE" -> Color(0xFF2E7D32)
                        "ENDED" -> Color(0xFFC62828)
                        "PAYMENT" -> Color(0xFFEF6C00)
                        else -> Color(0xFF00796B)
                    }
                    val statusIcon = when (booking.stage) {
                        "DONE" -> Icons.Default.CheckCircle
                        "ENDED" -> Icons.Default.Warning
                        "PAYMENT" -> Icons.Default.Info
                        else -> Icons.Default.Info
                    }
                    
                    Text(
                        text = booking.stageLabel,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = booking.stageLabel,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)

            if (nextStepMessage.isNotBlank()) {
                Text(
                    text = nextStepMessage,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF486158),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            booking.serviceScope?.let { scope ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F8F5),
                    border = BorderStroke(1.dp, Color(0xFFD9E8E0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (scope.kind.equals("PACKAGE", ignoreCase = true)) "Selected package" else "Selected services",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF486158)
                        )
                        Text(
                            text = scope.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15231D),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append("${scope.itemCount} item")
                                if (scope.itemCount != 1) append("s")
                                append(" · Provider amount ₹${scope.providerAmount.toInt()} · ${scope.durationMinutes} min")
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF60756B)
                        )
                    }
                }
            }

            // Items list style: provider location + customer address rows
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Provider location — dotted line leading to customer below (like the ss2 diagram)
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon column with dotted connector line below
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Provider location",
                            tint = Color(0xFF00796B),
                            modifier = Modifier.size(18.dp)
                        )
                        // Dotted connector
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(4.dp)
                                    .background(Color(0xFFB0BEC5))
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = booking.providerAddress.ifBlank { "Provider Address" },
                        fontSize = 13.sp,
                        color = Color(0xFF1C1C1C),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Customer home address
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.width(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Customer address",
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = booking.customerAddress.ifBlank { "Customer Address" },
                        fontSize = 13.sp,
                        color = Color(0xFF1C1C1C),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Divider before Ratings (only shown if done)
            if (booking.stage == "DONE") {
                HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)

                // Swiggy-style rating columns
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isViewerProvider) "Your Customer Rating" else "Service Rating",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(5) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(Color(0xFFF0F2F5))
                    )

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isViewerProvider) "Customer Rating" else "Provider Rating",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(4) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300), // Swiggy style rating stars
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)

            // Button/Footer Actions
            if (isCustomer) {
                // Client who booked this service sees the big Swiggy-style "REBOOK >" button
                Button(
                    onClick = {
                        val rebookListing = com.estatenestora.app.data.model.ServiceListing(
                            id = booking.listingId,
                            title = booking.listingTitle,
                            categoryName = "Service",
                            serviceType = booking.listingTitle,
                            providerName = booking.providerName,
                            price = booking.serviceFee,
                            location = ""
                        )
                        onRebookClick(rebookListing)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFF2EC), // Swiggy light orange background
                        contentColor = Color(0xFFFF6D2E) // Swiggy orange text color
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "REBOOK",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF6D2E)
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.PlayArrow, // Right arrow symbol
                            contentDescription = null,
                            tint = Color(0xFFFF6D2E),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                // Non-client (e.g. provider) sees the button option label with last status, and can tap to view details
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEDF2EE), // Soft green tint background
                        contentColor = Color(0xFF004D40) // Deep green text color
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${booking.stageLabel.uppercase()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF004D40)
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF004D40),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Bottom metadata line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ordered: ${booking.updatedAt}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = "${if (booking.serviceScope == null) "Starting from" else "Provider amount"}: ₹${(booking.serviceScope?.providerAmount ?: booking.serviceFee).toInt()}",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InfoRow(icon: String, label: String, value: String, valueColor: Color = Color(0xFF0D1A13)) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.width(130.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, color = NestoraTextMuted, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StepDot(label: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isActive) NestoraMint else Color(0xFFD4EFE6)),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, color = if (isActive) NestoraMint else NestoraTextMuted, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StepLine(isActive: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(2.dp)
            .padding(horizontal = 4.dp)
            .background(if (isActive) NestoraMint else Color(0xFFD4EFE6), RoundedCornerShape(1.dp))
    )
}

/**
 * [stage] drives color (the small, stable simplified-stage set from the
 * backend's DisplayStageForStatus), [label] is whatever text to show —
 * normally the backend's own stage_label, kept as a separate param so
 * callers can override it (e.g. a raw-status-specific label) without
 * duplicating the color logic.
 */
@Composable
fun StatusBadge(stage: String, label: String) {
    val (bgColor, textColor) = when (stage) {
        "PAYMENT" -> Color(0xFFFFF8E1) to NestoraWarning
        "DONE"    -> Color(0xFFE8FAF4) to NestoraMint
        "ENDED"   -> Color(0xFFFFEBEE) to NestoraError
        else      -> Color(0xFFE8FAF4) to NestoraMint
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bgColor) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
