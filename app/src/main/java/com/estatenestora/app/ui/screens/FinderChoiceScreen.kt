package com.estatenestora.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.estatenestora.app.R
import com.estatenestora.app.data.model.Category
import com.estatenestora.app.data.model.TelegramChatMessage
import com.estatenestora.app.data.model.ServiceListing
import com.estatenestora.app.ui.components.ProjectFooter
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke

// ─── Quick-search shortcut model ─────────────────────────────────────────────
private data class ServiceShortcut(
    val label: String,
    val subtitle: String,
    val emoji: String,
    val query: String,
    val gradient: List<Color>
)

private data class PopularLocation(
    val name: String,
    val bgGradient: List<Color>,
    val textColor: Color
)

@Composable
private fun PopularLocationCard(
    location: PopularLocation,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 84.dp, height = 84.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(location.bgGradient))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = location.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = location.textColor,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationProviderResultsPage(
    location: String,
    listings: List<ServiceListing>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onListingClick: (ServiceListing) -> Unit,
    onBook: (ServiceListing) -> Unit
) {
    var filter by remember { mutableStateOf("all") }
    val visibleListings = remember(filter, listings) {
        listings
            .let { if (filter == "verified") it.filter(ServiceListing::isVerified) else it }
            .let { if (filter == "top_rated") it.sortedByDescending(ServiceListing::rating) else it }
            .let { if (filter == "budget") it.sortedBy(ServiceListing::price) else it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(location, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Service providers in this location", fontSize = 11.sp, color = NestoraTextMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "all" to "All",
                        "top_rated" to "Top rated",
                        "budget" to "Budget",
                        "verified" to "Verified"
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = filter == id,
                            onClick = { filter = id },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(9.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NestoraMint,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color(0xFF374151)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filter == id,
                                borderColor = Color(0xFFD7DEE8),
                                selectedBorderColor = NestoraMint
                            )
                        )
                    }
                }
            }
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), color = NestoraMint)
                    }
                }
            } else if (visibleListings.isEmpty()) {
                item {
                    Text(
                        text = "No active providers are available in $location right now.",
                        modifier = Modifier.padding(20.dp),
                        color = NestoraTextMuted
                    )
                }
            } else {
                items(visibleListings, key = ServiceListing::id) { listing ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        MarketplaceListingCard(
                            listing = listing,
                            onClick = { onListingClick(listing) },
                            onBookViaTelegram = { onBook(listing) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinderChoiceScreen(
    categories: List<Category>,
    selectedTab: Int = 0,
    onTabChange: (Int) -> Unit = {},
    chatMessages: androidx.compose.runtime.snapshots.SnapshotStateList<TelegramChatMessage>,
    userName: String? = null,
    onSendMessage: (String) -> Unit = {},
    onClearChat: () -> Unit = {},
    onBookListing: (ServiceListing) -> Unit = {},
    userPhotoPath: String? = null,
    onExploreClick: () -> Unit = {},
    onSelectLocationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onRegisterServiceClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    currentLocation: String? = null,
    isProviderMode: Boolean = false,
    onModeToggle: () -> Unit = {},
    tabsList: List<com.estatenestora.app.ui.theme.NestoraTab> = emptyList(),
    selectedTabId: String = "finder",
    onTabSelected: (String) -> Unit = {},
    currentTheme: com.estatenestora.app.ui.theme.RoyalTheme = com.estatenestora.app.ui.theme.RoyalThemeRepository.getThemeForToday(),
    // ── Feed params (moved from HomeScreen/Explore tab) ──────────────────────
    listings: List<ServiceListing> = emptyList(),
    isLoadingFeed: Boolean = false,
    onRefreshFeed: () -> Unit = {},
    onListingClick: (ServiceListing) -> Unit = {},
    onFetchLocationListings: suspend (String) -> List<ServiceListing> = { emptyList() }
) {
    val pageSurface = remember(currentTheme) { selectedMenuSurface(currentTheme) }
    var isBottomBarVisible by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("all") }
    var requestedLocation by remember { mutableStateOf<String?>(null) }
    var locationListings by remember { mutableStateOf<List<ServiceListing>>(emptyList()) }
    var isLoadingLocationListings by remember { mutableStateOf(false) }
    val locationListingCache = remember { mutableStateMapOf<String, List<ServiceListing>>() }

    val filteredListings = remember(selectedFilter, listings) {
        listings
            .let { if (selectedFilter == "verified") it.filter(ServiceListing::isVerified) else it }
            .let { if (selectedFilter == "top_rated") it.sortedByDescending(ServiceListing::rating) else it }
            .let { if (selectedFilter == "budget") it.sortedBy(ServiceListing::price) else it }
    }
    var visibleProviderCount by remember(filteredListings.size, selectedFilter) { mutableIntStateOf(8) }
    val pagedListings = remember(filteredListings, visibleProviderCount) {
        filteredListings.take(visibleProviderCount)
    }

    LaunchedEffect(requestedLocation) {
        val location = requestedLocation ?: return@LaunchedEffect
        locationListingCache[location]?.let {
            locationListings = it
            return@LaunchedEffect
        }
        isLoadingLocationListings = true
        try {
            locationListings = onFetchLocationListings(location)
            locationListingCache[location] = locationListings
        } finally {
            isLoadingLocationListings = false
        }
    }

    val shortcuts = remember {
        listOf(
            ServiceShortcut(
                "Plumbers\nnear me", "Fast fix, guaranteed", "🔧",
                "plumber near me",
                listOf(Color(0xFF1565C0), Color(0xFF1E88E5))
            ),
            ServiceShortcut(
                "Maids &\nCleaning", "Daily & weekly help", "🧹",
                "maid cleaning service",
                listOf(Color(0xFF2E7D32), Color(0xFF43A047))
            ),
            ServiceShortcut(
                "AC Repair\n& Service", "Beat the heat", "❄️",
                "ac repair service",
                listOf(Color(0xFF00838F), Color(0xFF00ACC1))
            ),
            ServiceShortcut(
                "Packers &\nMovers", "Stress-free shifting", "📦",
                "packers and movers",
                listOf(Color(0xFF6A1B9A), Color(0xFF8E24AA))
            )
        )
    }


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

    LaunchedEffect(selectedTab) {
        isBottomBarVisible = true
    }

    

    if (selectedTab == 1) {
        AIChatScreen(
            messages = chatMessages,
            onSendMessage = onSendMessage,
            onSendSupportPayload = {},
            onBookListing = onBookListing,
            onClearChat = onClearChat,
            currentLocation = currentLocation,
            onSelectLocationClick = onSelectLocationClick,
            onProfileClick = onProfileClick,
            onRegisterServiceClick = onRegisterServiceClick,
            onBookingsClick = onBookingsClick,
            onFindServiceClick = {},
            onExploreClick = { onTabChange(0) },
            onScrollChanged = {},
            userPhotoPath = userPhotoPath,
            userName = userName
        )
    } else {
        Scaffold(
            modifier = Modifier.nestedScroll(nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Finder Tab Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onTabChange(0) }
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Finder",
                                tint = if (selectedTab == 0) NestoraMint else Color(0xFF8FA7A0),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Finder",
                                fontSize = 9.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) NestoraMint else Color(0xFF8FA7A0)
                            )
                        }

                        // Assistant Tab Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onTabChange(1) }
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.nestora_bottom_logo),
                                contentDescription = "Assistant",
                                tint = if (selectedTab == 1) NestoraMint else Color(0xFF8FA7A0),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Assistant",
                                fontSize = 9.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) NestoraMint else Color(0xFF8FA7A0)
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
            var isSearchFocused by remember { mutableStateOf(false) }
            val listState = rememberLazyListState()
            LaunchedEffect(listState, filteredListings.size, visibleProviderCount) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                    .collect { lastVisibleIndex ->
                        // The listing section begins after the fixed discovery
                        // sections. Reveal the next page only as the customer
                        // approaches the last visible provider card.
                        val nearEnd = lastVisibleIndex >= 8 + pagedListings.size - 2
                        if (nearEnd && visibleProviderCount < filteredListings.size) {
                            visibleProviderCount = minOf(visibleProviderCount + 8, filteredListings.size)
                        }
                    }
            }
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

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
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

                    if (!isSearchFocused) {
                        stickyHeader {
                            OnboardingSearchBar(
                                searchQuery = "",
                                onSearchQueryChange = {},
                                isScrolled = isScrolled,
                                hasCarouselBelow = true,
                                onClick = { isSearchFocused = true },
                                currentTheme = currentTheme
                            )
                        }
                    }

                    // Swiggy-style Hero Carousel
                    item {
                        HeroCarousel(theme = "finder", canvasColor = pageSurface)
                    }

                    // ══════════════════════════════════════════════════════════════
                    // FINDER TAB — Swiggy-inspired discovery layout
                    // ══════════════════════════════════════════════════════════════

                    // ── Section A: "What do you need?" shortcut tiles ─────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                                .background(pageSurface)
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        ) {
                            val firstName = userName?.split(" ")?.firstOrNull() ?: "there"
                            Text(
                                text = "$firstName, what do you need?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                shortcuts.take(2).forEach { s ->
                                    FinderShortcutCard(
                                        shortcut = s,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onSendMessage(s.query); onTabChange(1) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                shortcuts.drop(2).forEach { s ->
                                    FinderShortcutCard(
                                        shortcut = s,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onSendMessage(s.query); onTabChange(1) }
                                    )
                                }
                            }
                        }
                    }



                    // ── Section C: "Quick Hires near you" horizontal strip ────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(vertical = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Quick Hires near you",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0D1A13)
                                    )
                                    Text(
                                        text = "Available & ready to book",
                                        fontSize = 11.sp,
                                        color = NestoraTextMuted
                                    )
                                }
                                if (isLoadingFeed) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = NestoraMint
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            if (listings.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(listings.take(6)) { listing ->
                                        QuickHireCard(
                                            listing = listing,
                                            onClick = { onListingClick(listing) },
                                            onBook = { onBookListing(listing) }
                                        )
                                    }
                                }
                            } else if (!isLoadingFeed) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    repeat(3) {
                                        Box(
                                            modifier = Modifier
                                                .width(170.dp)
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFFF0F0F0))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Section D: "Top Rated in Kolkata" editorial spotlight ──────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF002E22)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brush.linearGradient(listOf(Color(0xFF002E22), Color(0xFF005E46))))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = NestoraAmber.copy(alpha = 0.95f)
                                            ) {
                                                Text(
                                                    text = "⭐  TOP RATED",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    letterSpacing = 0.6.sp
                                                )
                                            }
                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                text = "Kolkata's most\ntrusted professionals",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                lineHeight = 21.sp
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "Background-checked & verified by Nestora",
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.70f),
                                                lineHeight = 14.sp
                                            )
                                            Spacer(Modifier.height(14.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(Color.White)
                                                    .clickable {
                                                        onSendMessage("show top rated verified professionals")
                                                        onTabChange(1)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = "FIND NOW  →",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF002E22)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = "🏆", fontSize = 58.sp)
                                    }
                                }
                            }
                        }
                    }

                    // ── Popular Locations Carousel (Between Banner and Providers List) ──
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(vertical = 16.dp)
                        ) {
                            Text(
                                text = "Popular locations",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D1A13),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(12.dp))

                            val locations = remember {
                                listOf(
                                    PopularLocation("Newtown", listOf(Color(0xFFFDE8F7), Color(0xFFF8D3F0)), Color(0xFF4A154B)),
                                    PopularLocation("Shapoorji", listOf(Color(0xFFE1F3FE), Color(0xFFCBEBFE)), Color(0xFF0C4A6E)),
                                    PopularLocation("Akankha\nMore", listOf(Color(0xFFE6F8F3), Color(0xFFC3F1E5)), Color(0xFF064E3B)),
                                    PopularLocation("Rajarhat", listOf(Color(0xFFEDE9FE), Color(0xFFDDD6FE)), Color(0xFF4C1D95)),
                                    PopularLocation("Salt\nLake", listOf(Color(0xFFFFECEB), Color(0xFFFCD5CE)), Color(0xFF78281F)),
                                    PopularLocation("Park\nStreet", listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD)), Color(0xFF0369A1)),
                                    PopularLocation("Camac\nStreet", listOf(Color(0xFFDCFCE7), Color(0xFFBBF7D0)), Color(0xFF15803D)),
                                    PopularLocation("Ballygunge", listOf(Color(0xFFF3E8FF), Color(0xFFE9D5FF)), Color(0xFF6B21A8))
                                )
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(locations) { loc ->
                                    PopularLocationCard(
                                        location = loc,
                                        onClick = {
                                            requestedLocation = loc.name.replace("\n", " ")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // ── Section E header: count + filter chips ────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                        ) {
                            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 6.dp)
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${filteredListings.size} providers to explore",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0D1A13)
                                    )
                                    Text(
                                        text = "in ${currentLocation?.substringBefore(",")?.trim() ?: "your area"}",
                                        fontSize = 12.sp,
                                        color = NestoraTextMuted
                                    )
                                }
                                if (isLoadingFeed) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = NestoraMint
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            // Filter chips (plain horizontal scroll — avoids nested lazy)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "all"       to "All",
                                    "top_rated" to "Top rated",
                                    "budget"    to "Budget",
                                    "verified"  to "Verified"
                                ).forEach { (id, label) ->
                                    FilterChip(
                                        selected = selectedFilter == id,
                                        onClick = { selectedFilter = id },
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (selectedFilter == id) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier.height(30.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NestoraMint,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFFF4F5F7),
                                            labelColor = Color(0xFF555555)
                                        ),
                                        shape = RoundedCornerShape(9.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // ── Section E body: full listing cards ────────────────────────
                    if (pagedListings.isNotEmpty()) {
                        items(pagedListings, key = ServiceListing::id) { listing ->
                            Box(
                                modifier = Modifier
                                    .background(Color.White)
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                MarketplaceListingCard(
                                    listing = listing,
                                    onClick = { onListingClick(listing) },
                                    onBookViaTelegram = { onBookListing(listing) }
                                )
                            }
                        }
                        if (visibleProviderCount < filteredListings.size) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = NestoraMint,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    } else if (!isLoadingFeed) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF7FAF8),
                                border = BorderStroke(1.dp, Color(0xFFE2EBE5))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "⚡ Looking for specific help?",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D1A13)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Tap a shortcut above or chat with AI to match with top providers instantly.",
                                        fontSize = 12.sp,
                                        color = NestoraTextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { onTabChange(1) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Chat with AI", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = onRefreshFeed,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, NestoraMint),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Refresh Feed", color = NestoraMint, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                    item { Box(modifier = Modifier.background(Color.White)) { ProjectFooter() } }
                    } // end LazyColumn

                requestedLocation?.let { location ->
                    LocationProviderResultsPage(
                        location = location,
                        listings = locationListings,
                        isLoading = isLoadingLocationListings,
                        onBack = { requestedLocation = null },
                        onListingClick = onListingClick,
                        onBook = onBookListing
                    )
                }

                // ── 3. Active Search Overlay ──
                if (isSearchFocused) {
                    val finderOverlayContext = androidx.compose.ui.platform.LocalContext.current
                    DisposableEffect(isSearchFocused) {
                        val window = (finderOverlayContext as? android.app.Activity)?.window
                        if (window != null && isSearchFocused) {
                            val oldColor = window.statusBarColor
                            window.statusBarColor = android.graphics.Color.WHITE
                            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                            val oldLightStatusBars = insetsController.isAppearanceLightStatusBars
                            insetsController.isAppearanceLightStatusBars = true
                            
                            onDispose {
                                window.statusBarColor = oldColor
                                insetsController.isAppearanceLightStatusBars = oldLightStatusBars
                            }
                        } else {
                            onDispose {}
                        }
                    }
                    // Dim backdrop overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isSearchFocused = false }
                    )

                    // Floating Search Card at the top (overlaps the top portion of HeroCarousel)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            // Top Title / Navigation Row
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Left side: Thin black back arrow icon (←)
                                IconButton(
                                    onClick = { isSearchFocused = false },
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF2A2A2A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Center/Title: Centered, clean dark text
                                Text(
                                    text = "Search for services & providers",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2A2A2A),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Input Box
                            var searchQueryState by remember { mutableStateOf("") }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(15.dp),
                                color = Color.White,
                                border = BorderStroke(1.5.dp, Color(0xFFE0E0E0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Input Box Area (No magnifying glass icon on the left)
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQueryState.isEmpty()) {
                                            Column(verticalArrangement = Arrangement.Center) {
                                                Text("Try 'Plumber'", color = Color(0xFF9E9E9E), fontSize = 13.sp)
                                                Text("Try 'Maid'", color = Color(0xFF9E9E9E).copy(alpha = 0.6f), fontSize = 10.sp)
                                            }
                                        }
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = searchQueryState,
                                            onValueChange = { searchQueryState = it },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 13.sp,
                                                color = Color(0xFF0D1A13)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Right side: vertical divider line and orange microphone icon
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(1.dp)
                                            .background(Color(0xFFEAEAEA))
                                            .padding(vertical = 12.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    MicIcon(
                                        color = Color(0xFFFF5722),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

// ─── FinderShortcutCard: Swiggy-style gradient shortcut tile ─────────────────
@Composable
private fun FinderShortcutCard(
    shortcut: ServiceShortcut,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = shortcut.gradient[0]
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(shortcut.gradient))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = shortcut.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 16.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = shortcut.subtitle,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        lineHeight = 11.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = shortcut.emoji, fontSize = 22.sp)
                }
            }
        }
    }
}

// ─── QuickHireCard: horizontal compact provider card (like Swiggy walk-in) ───
@Composable
private fun QuickHireCard(
    listing: ServiceListing,
    onClick: () -> Unit,
    onBook: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(0.8.dp, Color(0xFFF0F0F0))
    ) {
        Column {
            // Cover image with rating badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(Color(0xFFF0F4F2))
            ) {
                AsyncImage(
                    model = getRealLifeImageModel(listing.serviceType.ifBlank { listing.title }),
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.93f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = NestoraAmber,
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", listing.rating),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B6914)
                        )
                    }
                }
            }
            // Info block
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = listing.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1A13),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = NestoraMint,
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = listing.location,
                        fontSize = 10.sp,
                        color = NestoraTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${listing.price.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NestoraMint
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NestoraMint)
                            .clickable { onBook() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Book",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
