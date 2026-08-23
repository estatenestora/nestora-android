package com.estatenestora.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.estatenestora.app.R
import com.estatenestora.app.data.model.Category
import com.estatenestora.app.data.model.TelegramChatMessage
import com.estatenestora.app.data.model.ServiceListing
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
    currentTheme: com.estatenestora.app.ui.theme.RoyalTheme = com.estatenestora.app.ui.theme.RoyalThemeRepository.getThemeForToday()
) {
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

                        // Services Tab Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onTabChange(2) }
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Services",
                                tint = if (selectedTab == 2) NestoraMint else Color(0xFF8FA7A0),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Services",
                                fontSize = 9.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 2) NestoraMint else Color(0xFF8FA7A0)
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
                        .background(Color(0xFFF9F9F9)),
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
                        HeroCarousel(theme = "finder")
                    }

                    // ── 2. Center body part, visible ONLY in the Assistant tab (selectedTab == 1) ──
                    if (selectedTab == 2) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(vertical = 24.dp)
                            ) {
                                Text(
                                    text = "Find Services & Rentals",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF004D40),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Find, book, and review expert service providers near you",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // LEFT: Primary Action Card (Chat with Assistant)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF004D40)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .height(260.dp)
                                            .clickable { onTabChange(1) }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE8F5E9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.nestora_bottom_logo),
                                                    contentDescription = "Chat with Assistant",
                                                    tint = Color(0xFF004D40),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "AI Assistant",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF004D40),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = "Describe what you need in plain words to match with top local service providers.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF2C2C2C),
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 15.sp,
                                                    maxLines = 4,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Button(
                                                onClick = { onTabChange(1) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40)),
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text(
                                                    text = "Start Chat",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // RIGHT: Column of stacked Selection Cards (Secondary Actions)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(260.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Card 1: Browse Categories
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .clickable { onExploreClick() }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.List,
                                                        contentDescription = "Categories",
                                                        tint = Color(0xFF004D40),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Categories",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF004D40),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(Modifier.height(1.dp))
                                                    Text(
                                                        text = "Browse by category.",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF2C2C2C),
                                                        lineHeight = 11.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        // Card 2: My Bookings
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .clickable { onBookingsClick() }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = "My Bookings",
                                                        tint = Color(0xFF004D40),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "My Bookings",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF004D40),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(Modifier.height(1.dp))
                                                    Text(
                                                        text = "View active bookings.",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF2C2C2C),
                                                        lineHeight = 11.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        // Card 3: Register as Partner
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .clickable { onRegisterServiceClick() }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Build,
                                                        contentDescription = "Register Service",
                                                        tint = Color(0xFF004D40),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Become Partner",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF004D40),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(Modifier.height(1.dp))
                                                    Text(
                                                        text = "List your own services.",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF2C2C2C),
                                                        lineHeight = 11.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
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
