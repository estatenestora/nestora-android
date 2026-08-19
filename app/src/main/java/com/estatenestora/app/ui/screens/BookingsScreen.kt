package com.estatenestora.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
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
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.BookingSummary
import com.estatenestora.app.ui.theme.*

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
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF004332) else Color(0xFF888888))
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
                    color = Color(0xFF004332),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(theme: String = "home") {
    // Each section shows contextually-relevant banners
    // Direct unique Unsplash URLs per slide — no keyword lookup, guaranteed unique images
    data class CarouselBanner(val title: String, val subtitle: String, val imageUrl: String)
    val banners = remember(theme) {
        when (theme) {
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

    val actionLabel = when (theme) {
        "bookings" -> "View Bookings"
        "register" -> "Register Now"
        "finder" -> "Find Now"
        else -> "Book Now"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
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
                .height(200.dp)
        ) { page ->
            val banner = banners[page]
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = banner.imageUrl,
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
                                    Color.Black.copy(alpha = 0.10f),
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
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.clickable { }
                    ) {
                        Text(
                            text = actionLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF004332),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
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
    onRebookClick: (com.estatenestora.app.data.model.ServiceListing) -> Unit = {}
) {
    // No distinct "PROVIDER" role exists in the backend — a user is a
    // provider by virtue of owning listings. Sent = bookings the viewer made
    // as a customer; Received = booking requests the viewer got as a provider.
    val sentBookings = remember(bookings, viewerUserId) {
        bookings.filter { it.customerUserId == viewerUserId }
    }
    val receivedBookings = remember(bookings, viewerUserId) {
        bookings.filter { it.providerUserId == viewerUserId }
    }
    var selectedBookingsTab by remember { mutableStateOf(0) } // 0 = Bookings, 1 = Sent, 2 = Received
    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableStateOf(0) }

    LaunchedEffect(selectedBookingsTab, searchQuery) {
        currentPage = 0
    }

    Scaffold(
        bottomBar = {
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
                    // Bookings Tab Button: show calendar icon only, NO label per request
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedBookingsTab = 0 }
                    ) {
                        BookingsBottomCalendarIcon(isSelected = selectedBookingsTab == 0)
                    }

                    // Sent Tab Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedBookingsTab = 1 }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Sent",
                            tint = if (selectedBookingsTab == 1) NestoraMint else Color(0xFF888888),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Sent",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBookingsTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBookingsTab == 1) NestoraMint else Color(0xFF888888)
                        )
                    }

                    // Received Tab Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedBookingsTab = 2 }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Received",
                            tint = if (selectedBookingsTab == 2) NestoraMint else Color(0xFF888888),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Received",
                            fontSize = 11.sp,
                            fontWeight = if (selectedBookingsTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedBookingsTab == 2) NestoraMint else Color(0xFF888888)
                        )
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
            val isScrolled by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
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
                    val currentListSize = when (selectedBookingsTab) {
                        0 -> {
                            bookings.count {
                                searchQuery.isBlank() || it.listingTitle.contains(searchQuery, ignoreCase = true) || it.referenceCode.contains(searchQuery, ignoreCase = true)
                            }
                        }
                        1 -> sentBookings.size
                        else -> receivedBookings.size
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
                    .background(Color(0xFFEDF2EE)),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 1. Shared top bar
                item {
                    OnboardingTopBar(
                        currentLocation = currentLocation,
                        onSelectLocationClick = onSelectLocationClick,
                        onProfileClick = onProfileClick,
                        onRegisterServiceClick = onRegisterServiceClick,
                        onBookingsClick = onBookingsClick,
                        onFindServiceClick = onFindServiceClick,
                        onExploreClick = onExploreClick,
                        activeMenu = "bookings",
                        userPhotoPath = userPhotoPath
                    )
                }

                // 2. Sticky search bar matching Explore
                stickyHeader {
                    OnboardingSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        isScrolled = isScrolled,
                        hasCarouselBelow = true
                    )
                }

                // 3. Hero Carousel Banner
                item {
                    HeroCarousel(theme = "bookings")
                }

                // 4. Content titles/stats
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                        val headerText = when (selectedBookingsTab) {
                            0 -> "My Bookings"
                            1 -> "Sent Bookings"
                            else -> "Received Bookings"
                        }
                        val descText = when (selectedBookingsTab) {
                            0 -> "Track your active service requests & orders"
                            1 -> "View details of your sent service proposals"
                            else -> "Manage bookings requests received from customers"
                        }

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

                        if (selectedBookingsTab == 0) {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatPill(label = "${bookings.size}", sublabel = "Total")
                                StatPill(label = "${bookings.count { it.stage == "IN_PROGRESS" }}", sublabel = "In Progress")
                                StatPill(label = "${bookings.count { it.stage == "DONE" }}", sublabel = "Completed")
                                StatPill(label = "${bookings.count { it.stage == "PAYMENT" }}", sublabel = "Pending")
                            }
                        }
                    }
                }

                when (selectedBookingsTab) {
                    0 -> {
                        val filteredBookings = bookings.filter {
                            searchQuery.isBlank() || it.listingTitle.contains(searchQuery, ignoreCase = true) || it.referenceCode.contains(searchQuery, ignoreCase = true)
                        }
                        val pageSize = 5
                        val paginatedBookings = filteredBookings.take((currentPage + 1) * pageSize)

                        if (filteredBookings.isEmpty()) {
                            item {
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
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE8FAF4)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("📋", fontSize = 32.sp)
                                            }
                                            Text(
                                                text = "No Bookings Yet",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0D1A13)
                                            )
                                            Text(
                                                text = "Your service requests and flat visits will appear here with real-time tracking.",
                                                fontSize = 13.sp,
                                                color = NestoraTextMuted,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(paginatedBookings, key = { booking -> booking.id }) { booking ->
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
                        val pageSize = 5
                        val paginatedSent = sentBookings.take((currentPage + 1) * pageSize)

                        if (sentBookings.isEmpty()) {
                            item { BookingsEmptyState(emoji = "📤", title = "No Sent Requests Yet", body = "Services you book will show up here so you can track them.") }
                        } else {
                            items(paginatedSent, key = { booking -> booking.id }) { booking ->
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
                    2 -> {
                        val pageSize = 5
                        val paginatedReceived = receivedBookings.take((currentPage + 1) * pageSize)

                        if (!hasProviderListings) {
                            item {
                                BookingsEmptyState(
                                    emoji = "🧰",
                                    title = "Become a Provider",
                                    body = "Register a service to start receiving booking requests from customers.",
                                    actionLabel = "Register a Service",
                                    onAction = onRegisterServiceClick
                                )
                            }
                        } else if (receivedBookings.isEmpty()) {
                            item { BookingsEmptyState(emoji = "📥", title = "No Requests Yet", body = "Booking requests from customers will show up here.") }
                        } else {
                            items(paginatedReceived, key = { booking -> booking.id }) { booking ->
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

            // Status bar background overlay to prevent content scrolling behind system status bar icons
            val statusBarHeight = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight)
                    .background(if (isScrolled) Color.White else Color(0xFF005E46))
            )
        }
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
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8FAF4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 32.sp)
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
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF004332))
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
    onRebookClick: (com.estatenestora.app.data.model.ServiceListing) -> Unit = {}
) {
    val isViewerProvider = viewerUserId != null && booking.providerUserId == viewerUserId
    val counterpartLabel = if (isViewerProvider) "Customer" else "Provider"
    val counterpartName = if (isViewerProvider) booking.customerName else booking.providerName
    val isCustomer = viewerUserId != null && booking.customerUserId == viewerUserId

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

            HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 1.dp)

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
                HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 1.dp)

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

            HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 1.dp)

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
                    text = "Bill Total: ₹${booking.serviceFee.toInt()}",
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
