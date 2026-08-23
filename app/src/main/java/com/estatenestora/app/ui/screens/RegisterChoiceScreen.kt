package com.estatenestora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.AndroidBridgeResponse
import com.estatenestora.app.data.model.Category
import com.estatenestora.app.data.model.ServiceType
import com.estatenestora.app.ui.theme.*
import com.estatenestora.app.data.model.GeocodePlace
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.lazy.items


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegisterChoiceScreen(
    categories: List<Category>,
    selectedTab: Int = 0,
    onTabChange: (Int) -> Unit = {},
    onFetchServiceTypes: suspend (categorySlug: String) -> List<ServiceType>,
    onFetchAllServiceTypes: suspend () -> List<ServiceType>,
    onFetchServiceAttributes: suspend (serviceTypeSlug: String) -> List<com.estatenestora.app.data.model.ServiceAttributeTemplate>,
    onSubmit: suspend (
        categorySlug: String,
        serviceTypeSlug: String,
        basePrice: Double,
        locationDisplayName: String,
        city: String,
        description: String,
        collectedAttributes: Map<String, String>
    ) -> AndroidBridgeResponse?,
    onParse: suspend (String) -> AndroidBridgeResponse?,
    onSave: suspend () -> AndroidBridgeResponse?,
    onUpdate: suspend (String) -> AndroidBridgeResponse?,
    onReset: suspend () -> Unit = {},
    onBack: () -> Unit,
    currentLocation: String? = null,
    onSelectLocationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onFindServiceClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onReverseGeocode: suspend (Double, Double) -> GeocodePlace? = { _, _ -> null },
    onSearchAddress: suspend (String, Double?, Double?) -> List<GeocodePlace>,
    autoRegisterMessages: androidx.compose.runtime.snapshots.SnapshotStateList<com.estatenestora.app.data.model.TelegramChatMessage> = remember { mutableStateListOf() },
    onClearAutoRegisterChat: () -> Unit = {},
    pendingMapLocationToSend: String? = null,
    onClearPendingMapLocation: () -> Unit = {},
    userPhotoPath: String? = null,
    profileName: String? = null,
    onFetchMyListings: (suspend () -> AndroidBridgeResponse?)? = null,
    onSetProviderAvailability: (suspend (String, String) -> com.estatenestora.app.data.model.ProviderAvailabilitySettings?)? = null,
    onSetCustomProviderAvailability: (suspend (String, String, String, String) -> com.estatenestora.app.data.model.ProviderAvailabilitySettings?)? = null,
    onSetListingActive: (suspend (String, Boolean) -> AndroidBridgeResponse?)? = null,
    onUpdateListing: (suspend (String, String, String, Double, String, String, Double, Double) -> AndroidBridgeResponse?)? = null,
    onSaveListingEditor: (suspend (com.estatenestora.app.data.model.ListingEditorUpdate) -> AndroidBridgeResponse?)? = null,
    isProviderMode: Boolean = false,
    onModeToggle: () -> Unit = {},
    tabsList: List<com.estatenestora.app.ui.theme.NestoraTab> = emptyList(),
    selectedTabId: String = "register",
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

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible && selectedTab != 1,
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
                        // Register Tab Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onTabChange(0) }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Register",
                                tint = if (selectedTab == 0) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Register",
                                fontSize = 9.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) NestoraMint else Color(0xFF888888)
                            )
                        }

                        // Describe Tab Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onTabChange(1) }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = "Describe",
                                tint = if (selectedTab == 1) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Describe",
                                fontSize = 9.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) NestoraMint else Color(0xFF888888)
                            )
                        }

                        // Fill Tab Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onTabChange(2) }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Fill",
                                tint = if (selectedTab == 2) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Fill",
                                fontSize = 9.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 2) NestoraMint else Color(0xFF888888)
                            )
                        }

                        // Listings Tab Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onTabChange(3) }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Listings",
                                tint = if (selectedTab == 3) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Listings",
                                fontSize = 9.sp,
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 3) NestoraMint else Color(0xFF888888)
                            )
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
            when (selectedTab) {
                0 -> {
                    // Landing homepage for Register flow styled after the Firebase dashboard structure
                    var searchQuery by remember { mutableStateOf("") }
                    var isSearchFocused by remember { mutableStateOf(false) }
                    val listState = rememberLazyListState()
                    LaunchedEffect(isSearchFocused) {
                        // Once the regular sticky bar is removed, item 1 is the
                        // carousel. It becomes the only content behind the card.
                        if (isSearchFocused) listState.scrollToItem(1)
                    }
                    val isScrolled by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex > 0
                        }
                    }
                    // Android's transparent status bar can apply its own contrast
                    // scrim, making an otherwise identical composable color look
                    // like a different green. Set it explicitly for this page.
                    val registerContext = androidx.compose.ui.platform.LocalContext.current
                    DisposableEffect(isSearchFocused, isScrolled) {
                        val window = (registerContext as? android.app.Activity)?.window
                        if (window != null) {
                            val isLightSurface = isSearchFocused || isScrolled
                            window.statusBarColor = if (isLightSurface) {
                                android.graphics.Color.WHITE
                            } else {
                                currentTheme.backgroundGradient.first().toArgb()
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                window.isStatusBarContrastEnforced = false
                            }
                            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                                .isAppearanceLightStatusBars = isLightSurface
                        }
                        onDispose {
                            // Other homes use edge-to-edge drawing, so do not leave
                            // an opaque Register color behind after navigation.
                            window?.statusBarColor = android.graphics.Color.TRANSPARENT
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
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    isScrolled = isScrolled,
                                    hasCarouselBelow = true,
                                    onClick = { isSearchFocused = true },
                                    currentTheme = currentTheme
                                )
                            }
                        }

                        item {
                            HeroCarousel(theme = "register")
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(vertical = 24.dp)
                            ) {
                                Text(
                                    text = "Register & List Services",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NestoraMint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Manage your provider setup and list services on Nestora",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                
                                Spacer(Modifier.height(24.dp))
                                
                                // 2. Firebase-style 2-Column Dashboard Layout (Row: Left Large Action + Right Stacked column)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // LEFT: Primary Action Card (Describe with AI)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, NestoraMint),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .height(260.dp)
                                            .clickable { onTabChange(1) } // Navigate to Describe with AI tab
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
                                                    .background(NestoraMintBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Create,
                                                    contentDescription = "Describe with AI",
                                                    tint = NestoraMint,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Describe to AI",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NestoraMint,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = "Register instantly by chatting with Nestora's AI assistant. Fast, guided auto-setup.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF2C2C2C), // Dark charcoal gray
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 15.sp,
                                                    maxLines = 4,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            
                                            Button(
                                                onClick = { onTabChange(1) },
                                                colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text(
                                                    text = "Get Started",
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
                                        // Card 1: Fill Form (Manual Entry)
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = NestoraMintBg),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .clickable { onTabChange(2) } // Navigate to Manual Fill Form tab
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
                                                        contentDescription = "Fill Form",
                                                        tint = NestoraMint,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Manual Form",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NestoraMint,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(Modifier.height(1.dp))
                                                    Text(
                                                        text = "Traditional form registration.",
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
                                            colors = CardDefaults.cardColors(containerColor = NestoraMintBg),
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
                                                        tint = NestoraMint,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "My Bookings",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NestoraMint,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(Modifier.height(1.dp))
                                                    Text(
                                                        text = "View incoming requests.",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF2C2C2C),
                                                        lineHeight = 11.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Card 3: Find Services
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = NestoraMintBg),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .clickable { onFindServiceClick() }
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
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = "Find Services",
                                                        tint = NestoraMint,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Find Services",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NestoraMint,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(Modifier.height(1.dp))
                                                    Text(
                                                        text = "Explore current service ads.",
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

                        // Status bar background overlay to prevent content scrolling behind system status bar icons
                        val statusBarHeight = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(statusBarHeight)
                                .background(if (isSearchFocused || isScrolled) Color.White else currentTheme.backgroundGradient.first())
                        )
                        // This is deliberately the last Box child, so it covers
                        // the complete status-bar area and the underlying sticky
                        // search bar cannot draw above it.
                        FloatingSearchOverlay(
                            visible = isSearchFocused,
                            query = searchQuery,
                            title = "Search your provider workspace",
                            onQueryChange = { searchQuery = it },
                            onDismiss = { isSearchFocused = false }
                        )
                    }
                }
                1 -> {
                    AutoRegisterScreen(
                        onBack = { onTabChange(0) },
                        onParse = onParse,
                        onSave = onSave,
                        onUpdate = onUpdate,
                        onReset = onReset,
                        onReverseGeocode = onReverseGeocode,
                        messages = autoRegisterMessages,
                        onClearChat = onClearAutoRegisterChat,
                        onExploreClick = onExploreClick,
                        onSelectLocationClick = onSelectLocationClick,
                        pendingMapLocationToSend = pendingMapLocationToSend,
                        onClearPendingMapLocation = onClearPendingMapLocation,
                        userPhotoPath = userPhotoPath,
                        onFetchAllServiceTypes = onFetchAllServiceTypes,
                        userName = profileName
                    )
                }
                2 -> {
                    RegisterServiceScreen(
                        categories = categories,
                        onFetchServiceTypes = onFetchServiceTypes,
                        onFetchAllServiceTypes = onFetchAllServiceTypes,
                        onFetchServiceAttributes = onFetchServiceAttributes,
                        onSubmit = onSubmit,
                        onBack = { onTabChange(0) },
                        onSearchAddress = onSearchAddress,
                        onReverseGeocode = onReverseGeocode
                    )
                }
                3 -> {
                    ProviderListingsScreen(
                        onFetchMyListings = onFetchMyListings,
                        onSetListingActive = onSetListingActive,
                        onSetCustomProviderAvailability = onSetCustomProviderAvailability,
                        onUpdateListing = onUpdateListing,
                        categories = categories,
                        onFetchAllServiceTypes = onFetchAllServiceTypes,
                        onSelectLocationClick = onSelectLocationClick,
                        pendingMapLocationToSend = pendingMapLocationToSend,
                        onClearPendingMapLocation = onClearPendingMapLocation,
                        onBack = { onTabChange(0) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProviderListingsScreen(
    onFetchMyListings: (suspend () -> AndroidBridgeResponse?)?,
    onSetListingActive: (suspend (String, Boolean) -> AndroidBridgeResponse?)? = null,
    onSetCustomProviderAvailability: (suspend (String, String, String, String) -> com.estatenestora.app.data.model.ProviderAvailabilitySettings?)? = null,
    onUpdateListing: (suspend (String, String, String, Double, String, String, Double, Double) -> AndroidBridgeResponse?)? = null,
    categories: List<Category>,
    onFetchAllServiceTypes: suspend () -> List<ServiceType> = { emptyList() },
    onSelectLocationClick: () -> Unit = {},
    pendingMapLocationToSend: String? = null,
    onClearPendingMapLocation: () -> Unit = {},
    onBack: () -> Unit
) {
    var listings by remember { mutableStateOf<List<com.estatenestora.app.data.model.ServiceListing>>(emptyList()) }
    var allServiceTypes by remember { mutableStateOf<List<ServiceType>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var searchKey by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Dropdown expanded states
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var serviceTypeMenuExpanded by remember { mutableStateOf(false) }

    // Dropdown filter selections
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedServiceTypeFilter by remember { mutableStateOf("All") }

    // Filter states
    var filterSort by remember { mutableStateOf("Relevance") }
    var filterStatus by remember { mutableStateOf("All") }
    var filterMinPrice by remember { mutableStateOf("") }
    var filterMaxPrice by remember { mutableStateOf("") }
    var filterMinRating by remember { mutableStateOf(0f) }

    // Pending filter states for sheet
    var pendingSort by remember { mutableStateOf("Relevance") }
    var pendingStatus by remember { mutableStateOf("All") }
    var pendingMinPrice by remember { mutableStateOf("") }
    var pendingMaxPrice by remember { mutableStateOf("") }
    var pendingMinRating by remember { mutableStateOf(0f) }
    var pendingCategory by remember { mutableStateOf("Sort") }

    // State for Full Screen Details/Edit view
    var selectedListingForDetail by remember { mutableStateOf<com.estatenestora.app.data.model.ServiceListing?>(null) }

    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        allServiceTypes = try { onFetchAllServiceTypes() } catch (e: Exception) { emptyList() }
        if (onFetchMyListings != null) {
            try {
                val resp = onFetchMyListings()
                if (resp != null && resp.ok) {
                    listings = resp.listings.orEmpty().map { it.toServiceListing() }
                } else { errorMessage = resp?.reply ?: "Failed to load listings" }
            } catch (e: Exception) {
                errorMessage = e.message ?: "An error occurred while loading"
            } finally { isLoading = false }
        } else { isLoading = false }
    }

    val filteredListings = remember(listings, searchKey, filterSort, filterStatus, filterMinPrice, filterMaxPrice, filterMinRating, selectedCategoryFilter, selectedServiceTypeFilter) {
        var result = listings.filter { l ->
            val matchSearch = searchKey.isBlank() ||
                l.title.contains(searchKey, true) ||
                l.location.contains(searchKey, true) ||
                l.description.contains(searchKey, true)
            val matchStatus = when (filterStatus) {
                "Active" -> l.isActive
                "Inactive" -> !l.isActive
                else -> true
            }
            val minP = filterMinPrice.toDoubleOrNull()
            val maxP = filterMaxPrice.toDoubleOrNull()
            val matchPrice = (minP == null || l.price >= minP) && (maxP == null || l.price <= maxP)
            val matchRating = filterMinRating == 0f || l.rating >= filterMinRating
            val matchCategory = selectedCategoryFilter == "All" || l.categoryName.equals(selectedCategoryFilter, ignoreCase = true)
            val matchServiceType = selectedServiceTypeFilter == "All" || l.serviceType.equals(selectedServiceTypeFilter, ignoreCase = true)

            matchSearch && matchStatus && matchPrice && matchRating && matchCategory && matchServiceType
        }
        result = when (filterSort) {
            "Price: Low to High" -> result.sortedBy { it.price }
            "Price: High to Low" -> result.sortedByDescending { it.price }
            "Rating: High to Low" -> result.sortedByDescending { it.rating }
            else -> result
        }
        result
    }

    val hasActiveFilter = filterSort != "Relevance" || filterStatus != "All" ||
        filterMinPrice.isNotBlank() || filterMaxPrice.isNotBlank() || filterMinRating > 0f ||
        selectedCategoryFilter != "All" || selectedServiceTypeFilter != "All"

    val listState = rememberLazyListState()

    if (selectedListingForDetail != null) {
        val detailListing = selectedListingForDetail!!
        // ── 8. Full Details & Edit Page Overlay ──
        var dTitle by remember(detailListing.id) { mutableStateOf(detailListing.title) }
        var dDesc by remember(detailListing.id) { mutableStateOf(detailListing.description) }
        var dPrice by remember(detailListing.id) { mutableStateOf(if (detailListing.price > 0) detailListing.price.toInt().toString() else "") }
        var dLocation by remember(detailListing.id) { mutableStateOf(detailListing.location) }
        var dLat by remember { mutableStateOf(0.0) }
        var dLon by remember { mutableStateOf(0.0) }

        var availabilityPreset by remember { mutableStateOf("CUSTOM") }
        var customDays by remember { mutableStateOf(emptySet<Int>()) }
        var customStart by remember { mutableStateOf("09:00") }
        var customEnd by remember { mutableStateOf("18:00") }
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        LaunchedEffect(pendingMapLocationToSend) {
            if (pendingMapLocationToSend != null) {
                val parts = pendingMapLocationToSend.split("||")
                dLocation = parts.getOrNull(0)?.trim() ?: dLocation
                val coords = parts.getOrNull(1)?.split(",")
                dLat = coords?.getOrNull(0)?.trim()?.toDoubleOrNull() ?: dLat
                dLon = coords?.getOrNull(1)?.trim()?.toDoubleOrNull() ?: dLon
                onClearPendingMapLocation()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { selectedListingForDetail = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0D1A13))
                }
                Text("Edit Listing Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F2E23))
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = dTitle, onValueChange = { dTitle = it },
                label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = dDesc, onValueChange = { dDesc = it },
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = dPrice, onValueChange = { dPrice = it.filter { c -> c.isDigit() } },
                label = { Text("Base Price (₹)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelectLocationClick() },
                shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Color(0xFFBBBBBB)), color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = NestoraMint, modifier = Modifier.size(16.dp))
                    Text(
                        dLocation.ifBlank { "Tap to pick location from map" },
                        fontSize = 13.sp, color = if (dLocation.isBlank()) Color.Gray else Color(0xFF1C1C1C)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Work Availability Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF075D45))
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ASAP_ONLY" to "ASAP only", "CUSTOM" to "Custom hours").forEach { (preset, label) ->
                    val sel = availabilityPreset == preset
                    Surface(
                        modifier = Modifier.clickable { availabilityPreset = preset },
                        shape = RoundedCornerShape(20.dp),
                        color = if (sel) NestoraMint else Color.Transparent,
                        border = BorderStroke(1.dp, if (sel) NestoraMint else Color(0xFFDDE2E9))
                    ) {
                        Text(
                            label, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) Color.White else Color(0xFF666666),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                when (availabilityPreset) {
                    "ASAP_ONLY" -> "🚀 You'll accept jobs immediately — customers can book you right now."
                    else -> "🕒 You work on a schedule — customers book specific days & hours."
                }, fontSize = 11.sp, color = Color(0xFF607D72)
            )

            if (availabilityPreset == "CUSTOM") {
                Spacer(Modifier.height(10.dp))
                Text("Work days", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F5A47))
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (0..6).forEach { d ->
                        val sel = customDays.contains(d)
                        Surface(
                            modifier = Modifier.size(36.dp).clickable {
                                customDays = if (sel) customDays - d else customDays + d
                            }, shape = CircleShape,
                            color = if (sel) NestoraMint else Color(0xFFF1F3F5),
                            border = if (sel) BorderStroke(1.dp, NestoraMint) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    dayNames[d].first().toString(), fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold, color = if (sel) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Start" to customStart to { v: String -> customStart = v },
                        "End" to customEnd to { v: String -> customEnd = v })
                        .forEach { (labelAndVal, setter) ->
                            val (label, value) = labelAndVal
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = value, onValueChange = {}, label = { Text("$label Time") },
                                    readOnly = true, modifier = Modifier.fillMaxWidth()
                                )
                                Box(modifier = Modifier.matchParentSize().clickable {
                                    val p = value.split(":")
                                    val h = p.getOrNull(0)?.toIntOrNull() ?: if (label == "Start") 9 else 18
                                    val m = p.getOrNull(1)?.toIntOrNull() ?: 0
                                    android.app.TimePickerDialog(context, { _, hv, mv ->
                                        setter(String.format("%02d:%02d", hv, mv))
                                    }, h, m, true).show()
                                })
                            }
                        }
                }

                Spacer(Modifier.height(10.dp))
                // Separate availability set button
                Button(
                    onClick = {
                        scope.launch {
                            val daysCsv = customDays.sorted().joinToString(",")
                            if (daysCsv.isEmpty()) {
                                android.widget.Toast.makeText(context, "Pick at least one day.", android.widget.Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val r = onSetCustomProviderAvailability?.invoke(detailListing.id, daysCsv, customStart, customEnd)
                            if (r != null) {
                                val readableDays = customDays.sorted().joinToString(", ") { dayNames[it] }
                                android.widget.Toast.makeText(
                                    context,
                                    "✅ Availability saved for $readableDays ($customStart – $customEnd)",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                android.widget.Toast.makeText(context, "Could not save. Try again.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Set Availability", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Save listing changes button
            var savingEdit by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    val priceVal = dPrice.toDoubleOrNull() ?: 0.0
                    if (dTitle.isBlank()) {
                        android.widget.Toast.makeText(context, "Title cannot be empty.", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        savingEdit = true
                        val locParts = dLocation.split(",")
                        val cityGuess = locParts.lastOrNull()?.trim() ?: ""
                        val resp = onUpdateListing?.invoke(detailListing.id, dTitle, dDesc, priceVal, dLocation, cityGuess, dLat, dLon)
                        savingEdit = false
                        if (resp?.ok == true) {
                            android.widget.Toast.makeText(context, "✅ Listing updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            val updated = detailListing.copy(title = dTitle, description = dDesc, price = priceVal, location = dLocation)
                            listings = listings.map { if (it.id == updated.id) updated else it }
                            selectedListingForDetail = null
                        } else {
                            android.widget.Toast.makeText(context, resp?.reply ?: "Could not save. Try again.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !savingEdit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (savingEdit) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {

            // ── 1. Sticky Header Top Bar with statusBarsPadding() ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 10.dp, bottom = 8.dp)
                ) {
                    // Header Row: Back button + Search bar Box + Square action badge container
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F2E23),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Search Text Input Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F3F5),
                            border = BorderStroke(1.dp, Color(0xFFE2EAF2))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF757575),
                                    modifier = Modifier.size(20.dp)
                                )
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchKey,
                                    onValueChange = { searchKey = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color(0xFF1C1C1C)),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        if (searchKey.isEmpty()) {
                                            Text("Search for restaurant, area, vib...", fontSize = 14.sp, color = Color(0xFF9E9E9E))
                                        }
                                        inner()
                                    }
                                )
                            }
                        }

                        // Fixed Square Action Badge Container (Number Count Only, no Rs)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F5A47),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                                ) {
                                Text(
                                    text = "${filteredListings.size}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Horizontal Filter Track (Scrollable chips)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter Button Chip
                        Surface(
                            modifier = Modifier.clickable {
                                pendingSort = filterSort
                                pendingStatus = filterStatus
                                pendingMinPrice = filterMinPrice
                                pendingMaxPrice = filterMaxPrice
                                pendingMinRating = filterMinRating
                                pendingCategory = "Sort"
                                showFilterSheet = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (hasActiveFilter) NestoraMint else Color.White,
                            border = BorderStroke(1.dp, if (hasActiveFilter) NestoraMint else Color(0xFFDDE2E9))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Filter ⇅", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (hasActiveFilter) Color.White else Color(0xFF333333))
                            }
                        }

                        // Sort by Chip
                        val sortText = if (filterSort == "Relevance") "Sort by ▾" else "$filterSort ▾"
                        Surface(
                            modifier = Modifier.clickable {
                                pendingSort = filterSort; pendingStatus = filterStatus
                                pendingMinPrice = filterMinPrice; pendingMaxPrice = filterMaxPrice
                                pendingMinRating = filterMinRating; pendingCategory = "Sort"
                                showFilterSheet = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (filterSort != "Relevance") Color(0xFFE8F5E9) else Color.White,
                            border = BorderStroke(1.dp, if (filterSort != "Relevance") NestoraMint else Color(0xFFDDE2E9))
                        ) {
                            Text(sortText, fontSize = 12.sp, color = if (filterSort != "Relevance") NestoraMint else Color(0xFF444444),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }

                        // Category Dropdown Filter Chip
                        Box {
                            Surface(
                                modifier = Modifier.clickable { categoryMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedCategoryFilter != "All") Color(0xFFE8FAF4) else Color.White,
                                border = BorderStroke(1.dp, if (selectedCategoryFilter != "All") NestoraMint else Color(0xFFDDE2E9))
                            ) {
                                Text(
                                    text = if (selectedCategoryFilter == "All") "Category ▾" else "$selectedCategoryFilter ▾",
                                    fontSize = 12.sp,
                                    color = if (selectedCategoryFilter != "All") NestoraMint else Color(0xFF444444),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            val uniqueCategories = remember(listings) {
                                listOf("All") + listings.map { it.categoryName }.distinct()
                            }
                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                uniqueCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.replace("_", " ").replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            selectedCategoryFilter = cat
                                            categoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Service Type Dropdown Filter Chip
                        Box {
                            Surface(
                                modifier = Modifier.clickable { serviceTypeMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedServiceTypeFilter != "All") Color(0xFFE8FAF4) else Color.White,
                                border = BorderStroke(1.dp, if (selectedServiceTypeFilter != "All") NestoraMint else Color(0xFFDDE2E9))
                            ) {
                                Text(
                                    text = if (selectedServiceTypeFilter == "All") "Service Type ▾" else "${selectedServiceTypeFilter.replace("_", " ").replaceFirstChar { it.uppercase() }} ▾",
                                    fontSize = 12.sp,
                                    color = if (selectedServiceTypeFilter != "All") NestoraMint else Color(0xFF444444),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            val uniqueServiceTypes = remember(listings) {
                                listOf("All") + listings.map { it.serviceType }.distinct()
                            }
                            DropdownMenu(
                                expanded = serviceTypeMenuExpanded,
                                onDismissRequest = { serviceTypeMenuExpanded = false }
                            ) {
                                uniqueServiceTypes.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st.replace("_", " ").replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            selectedServiceTypeFilter = st
                                            serviceTypeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quick Filter: Active status
                        val isActiveFilter = filterStatus == "Active"
                        Surface(
                            modifier = Modifier.clickable {
                                filterStatus = if (isActiveFilter) "All" else "Active"
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActiveFilter) Color(0xFFE8F5E9) else Color.White,
                            border = BorderStroke(1.dp, if (isActiveFilter) NestoraMint else Color(0xFFDDE2E9))
                        ) {
                            Text("Active", fontSize = 12.sp, color = if (isActiveFilter) NestoraMint else Color(0xFF444444),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }

                        // Quick Filter: Inactive status
                        val isInactiveFilter = filterStatus == "Inactive"
                        Surface(
                            modifier = Modifier.clickable {
                                filterStatus = if (isInactiveFilter) "All" else "Inactive"
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isInactiveFilter) Color(0xFFE8F5E9) else Color.White,
                            border = BorderStroke(1.dp, if (isInactiveFilter) NestoraMint else Color(0xFFDDE2E9))
                        ) {
                            Text("Inactive", fontSize = 12.sp, color = if (isInactiveFilter) NestoraMint else Color(0xFF444444),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }

                        // Ratings Quick Filter
                        val ratingSelected = filterMinRating > 0f
                        Surface(
                            modifier = Modifier.clickable {
                                pendingSort = filterSort; pendingStatus = filterStatus
                                pendingMinPrice = filterMinPrice; pendingMaxPrice = filterMaxPrice
                                pendingMinRating = filterMinRating; pendingCategory = "Ratings"
                                showFilterSheet = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (ratingSelected) Color(0xFFE8F5E9) else Color.White,
                            border = BorderStroke(1.dp, if (ratingSelected) NestoraMint else Color(0xFFDDE2E9))
                        ) {
                            Text(
                                text = if (ratingSelected) "⭐ ${filterMinRating.toInt()}+" else "Ratings ▾",
                                fontSize = 12.sp,
                                color = if (ratingSelected) NestoraMint else Color(0xFF444444),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NestoraMint)
                }
            } else if (errorMessage != null) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage!!, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { isLoading = true; errorMessage = null },
                            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                        ) { Text("Retry") }
                    }
                }
            } else {
                // ── LazyColumn Container for Discovery Cards ──
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredListings, key = { it.id }) { listing ->
                        ProviderListingCard(
                            listing = listing,
                            serviceTypes = allServiceTypes,
                            onSetListingActive = onSetListingActive,
                            onImageClick = { selectedListingForDetail = it },
                            onListingUpdated = { updated ->
                                listings = listings.map { if (it.id == updated.id) updated else it }
                            }
                        )
                    }

                    if (filteredListings.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔍", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("No listings found", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F2E23))
                                Spacer(Modifier.height(4.dp))
                                Text("Try adjusting your search or filters.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Filter Modal BottomSheet ──
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filter", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1C1C1C))
                    IconButton(onClick = { showFilterSheet = false }) {
                        Text("✕", fontSize = 18.sp, color = Color.Gray)
                    }
                }
                HorizontalDivider()

                val filterNavItems = listOf("Sort", "Status", "Ratings", "Price Range")
                Row(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    LazyColumn(modifier = Modifier.width(130.dp).fillMaxHeight()) {
                        items(filterNavItems) { nav ->
                            val isSelected = pendingCategory == nav
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pendingCategory = nav }
                                    .background(if (isSelected) Color.White else Color(0xFFF5F6F8))
                                    .then(if (isSelected) Modifier.drawLeftBorder(NestoraMint, 3.dp) else Modifier)
                                    .padding(vertical = 16.dp, horizontal = 16.dp)
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

                    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        when (pendingCategory) {
                            "Sort" -> {
                                Text("SORT BY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(Modifier.height(12.dp))
                                listOf("Relevance", "Price: Low to High", "Price: High to Low", "Rating: High to Low").forEach { option ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { pendingSort = option }.padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        RadioButton(
                                            selected = pendingSort == option,
                                            onClick = { pendingSort = option },
                                            colors = RadioButtonDefaults.colors(selectedColor = NestoraMint)
                                        )
                                        Text(option, fontSize = 13.sp, color = Color(0xFF333333))
                                    }
                                }
                            }
                            "Status" -> {
                                Text("LISTING STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(Modifier.height(12.dp))
                                listOf("All", "Active", "Inactive").forEach { option ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { pendingStatus = option }.padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        RadioButton(
                                            selected = pendingStatus == option,
                                            onClick = { pendingStatus = option },
                                            colors = RadioButtonDefaults.colors(selectedColor = NestoraMint)
                                        )
                                        Text(option, fontSize = 13.sp, color = Color(0xFF333333))
                                    }
                                }
                            }
                            "Ratings" -> {
                                Text("MINIMUM RATING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(Modifier.height(12.dp))
                                listOf(0f, 3f, 3.5f, 4f, 4.5f).forEach { rating ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { pendingMinRating = rating }.padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        RadioButton(
                                            selected = pendingMinRating == rating,
                                            onClick = { pendingMinRating = rating },
                                            colors = RadioButtonDefaults.colors(selectedColor = NestoraMint)
                                        )
                                        Text(if (rating == 0f) "Any rating" else "⭐ ${rating}+", fontSize = 13.sp, color = Color(0xFF333333))
                                    }
                                }
                            }
                            "Price Range" -> {
                                Text("PRICE RANGE (₹)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = pendingMinPrice,
                                    onValueChange = { pendingMinPrice = it.filter { c -> c.isDigit() } },
                                    label = { Text("Min Price") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = pendingMaxPrice,
                                    onValueChange = { pendingMaxPrice = it.filter { c -> c.isDigit() } },
                                    label = { Text("Max Price") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        pendingSort = "Relevance"; pendingStatus = "All"
                        pendingMinPrice = ""; pendingMaxPrice = ""; pendingMinRating = 0f
                        filterSort = "Relevance"; filterStatus = "All"
                        filterMinPrice = ""; filterMaxPrice = ""; filterMinRating = 0f
                        showFilterSheet = false
                    }) {
                        Text("Clear Filters", fontSize = 14.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            filterSort = pendingSort; filterStatus = pendingStatus
                            filterMinPrice = pendingMinPrice; filterMaxPrice = pendingMaxPrice
                            filterMinRating = pendingMinRating
                            showFilterSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0D0D0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(130.dp)
                    ) {
                        Text("Apply", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    }
                }
            }
        }
    }
}

private fun Modifier.drawLeftBorder(color: Color, width: androidx.compose.ui.unit.Dp): Modifier =
    this.then(Modifier.drawWithContent {
        drawContent()
        drawRect(color = color, size = androidx.compose.ui.geometry.Size(width.toPx(), size.height))
    })

// Helper function to return exactly three image URLs related to the service type
fun getServiceTypeImages(serviceType: String): List<String> {
    val clean = serviceType.lowercase()
    return when {
        clean.contains("electrician") -> listOf(
            "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500",
            "https://images.unsplash.com/photo-1581092921461-eab62e97a780?w=500",
            "https://images.unsplash.com/photo-1605810230434-7631ac76ec81?w=500"
        )
        clean.contains("plumber") || clean.contains("plumbing") -> listOf(
            "https://images.unsplash.com/photo-1504328345606-18bbc8c9d7d1?w=500",
            "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=500",
            "https://images.unsplash.com/photo-1607472586893-edb57bdc0e39?w=500"
        )
        clean.contains("clean") || clean.contains("housekeeper") || clean.contains("maid") -> listOf(
            "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=500",
            "https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?w=500",
            "https://images.unsplash.com/photo-1583907659441-add36a28904e?w=500"
        )
        clean.contains("painter") || clean.contains("painting") -> listOf(
            "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=500",
            "https://images.unsplash.com/photo-1562259949-e8e7689d7828?w=500",
            "https://images.unsplash.com/photo-1534349762230-e0cadf78f5da?w=500"
        )
        clean.contains("salon") || clean.contains("hair") || clean.contains("beauty") || clean.contains("spa") -> listOf(
            "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500",
            "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500",
            "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500"
        )
        else -> listOf(
            "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=500",
            "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=500",
            "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProviderListingCard(
    listing: com.estatenestora.app.data.model.ServiceListing,
    serviceTypes: List<ServiceType> = emptyList(),
    onSetListingActive: (suspend (String, Boolean) -> AndroidBridgeResponse?)? = null,
    onImageClick: (com.estatenestora.app.data.model.ServiceListing) -> Unit,
    onListingUpdated: (com.estatenestora.app.data.model.ServiceListing) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var isActive by remember(listing.id, listing.isActive) { mutableStateOf(listing.isActive) }
    var savingActive by remember { mutableStateOf(false) }

    val resolvedServiceType = remember(listing.serviceType, serviceTypes) {
        serviceTypes.firstOrNull { it.slug == listing.serviceType }
    }
    val serviceTypeDisplayName = resolvedServiceType?.name
        ?: listing.serviceType.replace("_", " ").replaceFirstChar { it.uppercase() }
    val categoryDisplayName = remember(listing.categoryName, resolvedServiceType) {
        resolvedServiceType?.categorySlug?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
            ?: listing.categoryName.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val imageList = remember(listing.serviceType) { getServiceTypeImages(listing.serviceType) }

    // ── Zomato Structural Layout Card ──
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFEBEBEB)),
        onClick = { onImageClick(listing) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── 3. Image Slider Section (Top half of the card) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                    .clickable { onImageClick(listing) }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    AsyncImage(
                        model = imageList[page % imageList.size],
                        contentDescription = "Listing Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Favorite Icon (White wireframe heart outline at top-right corner)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(34.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.25f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Carousel Dot Indicators (Exactly three dots centered at bottom edge)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 16.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            // ── 4. Restaurant Information Metadata (Middle section) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Title & Rating Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listing.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111111),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Small green star icon followed by bold text rating string
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F7855)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (listing.rating > 0f) String.format("%.1f", listing.rating) else "4.3",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Address & 7. Cover Range Row (service_radius_km)
                Text(
                    text = "${listing.location.ifBlank { "City Centre 2, Rajarhat" }} • Cover range: ${listing.serviceRadiusKm} km",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // Cuisine / Service Row (without 'for two')
                Text(
                    text = "$categoryDisplayName, $serviceTypeDisplayName • ₹${listing.price.toInt()}",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // ── 9. Show description instead of offer and payment ──
                if (listing.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF2F2F2))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = listing.description,
                        fontSize = 13.sp,
                        color = Color(0xFF616161),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF2F2F2))
                Spacer(Modifier.height(8.dp))

                // ── 1. & 2. Toggle active & real count of created bookings on bottom left ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Small Switch
                    Switch(
                        checked = isActive,
                        enabled = !savingActive && onSetListingActive != null,
                        onCheckedChange = { active ->
                            scope.launch {
                                savingActive = true
                                val resp = onSetListingActive?.invoke(listing.id, active)
                                if (resp?.ok == true) {
                                    isActive = active
                                    android.widget.Toast.makeText(context,
                                        if (active) "✅ Listing activated" else "⏸ Listing deactivated",
                                        android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context,
                                        resp?.reply ?: "Could not update status.",
                                        android.widget.Toast.LENGTH_SHORT).show()
                                }
                                savingActive = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0F7855),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFBBBBBB)
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                    Text(
                        text = if (isActive) "Active" else "Inactive",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color(0xFF0F7855) else Color.Gray
                    )

                    // Booking real count of created bookings clearly understandable
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Bookings: ${listing.openBookingCount + listing.requestedBookingCount} (${listing.openBookingCount} open, ${listing.requestedBookingCount} requested)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F5A47)
                    )
                }
            }
        }
    }
}
