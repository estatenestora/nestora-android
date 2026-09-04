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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.AndroidBridgeResponse
import com.estatenestora.app.data.model.Category
import com.estatenestora.app.data.model.ServiceType
import com.estatenestora.app.ui.theme.*
import com.estatenestora.app.ui.theme.NestoraMint
import com.estatenestora.app.ui.theme.RoyalTheme
import com.estatenestora.app.ui.theme.RoyalThemeRepository
import com.estatenestora.app.data.model.GeocodePlace
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


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
        latitude: Double,
        longitude: Double,
        serviceName: String,
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
    onFetchProviderServiceCatalog: (suspend (String) -> AndroidBridgeResponse?)? = null,
    onSaveProviderServiceOffering: (suspend (String, com.google.gson.JsonObject) -> AndroidBridgeResponse?)? = null,
    onSaveProviderServicePackage: (suspend (String, com.google.gson.JsonObject) -> AndroidBridgeResponse?)? = null,
    onFetchMediaAssets: (suspend (String, String) -> AndroidBridgeResponse?)? = null,
    onUploadManagedMedia: (suspend (android.net.Uri, String, String, String) -> AndroidBridgeResponse)? = null,
    onArchiveMediaAsset: (suspend (String) -> AndroidBridgeResponse?)? = null,
    onResolveMedia: (suspend (String) -> String?)? = null,
    isProviderMode: Boolean = false,
    onModeToggle: () -> Unit = {},
    tabsList: List<com.estatenestora.app.ui.theme.NestoraTab> = emptyList(),
    selectedTabId: String = "register",
    onTabSelected: (String) -> Unit = {},
    currentTheme: com.estatenestora.app.ui.theme.RoyalTheme = com.estatenestora.app.ui.theme.RoyalThemeRepository.getThemeForToday(),
    showHomeChrome: Boolean = true,
    onNestedPageChanged: (Boolean) -> Unit = {}
) {
    val pageSurface = remember(currentTheme) { selectedMenuSurface(currentTheme) }

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
        onNestedPageChanged(selectedTab != 0)
    }
    DisposableEffect(Unit) {
        onDispose { onNestedPageChanged(false) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                            val isLightSurface = !showHomeChrome || isSearchFocused || isScrolled
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

                    val landingContent: @Composable () -> Unit = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
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
                                    icon = Icons.Default.Build,
                                    title = "Register"
                                )
                            }
                            item { NestoraSectionDivider() }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                                    .background(if (showHomeChrome) pageSurface else Color.White)
                                    .padding(top = 16.dp, bottom = 24.dp)
                            ) {
                                if (!showHomeChrome) {
                                    Text(
                                        text = "Choose a registration method",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF10231B)
                                    )
                                }
                                if (!showHomeChrome) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onTabChange(1) },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFFB8D1C7))
                                        ) {
                                            Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(7.dp))
                                            Text("Ask Nestora", fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { onTabChange(2) },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFFB8D1C7))
                                        ) {
                                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(7.dp))
                                            Text("Manual form", fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                } else {
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
                                                    text = "Ask Nestora",
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
                        }

                        if (showHomeChrome) {
                            // Home chrome owns the system status-area colour.
                            val statusBarHeight = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(statusBarHeight)
                                    .background(if (isSearchFocused || isScrolled) Color.White else currentTheme.backgroundGradient.first())
                            )
                        }
                    }
                    }
                    landingContent()
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
                        onFetchProviderServiceCatalog = onFetchProviderServiceCatalog,
                        onSaveProviderServiceOffering = onSaveProviderServiceOffering,
                        onSaveProviderServicePackage = onSaveProviderServicePackage,
                        onFetchMediaAssets = onFetchMediaAssets,
                        onUploadManagedMedia = onUploadManagedMedia,
                        onArchiveMediaAsset = onArchiveMediaAsset,
                        onResolveMedia = onResolveMedia,
                        categories = categories,
                        onFetchAllServiceTypes = onFetchAllServiceTypes,
                        onFetchServiceAttributes = onFetchServiceAttributes,
                        onSelectLocationClick = onSelectLocationClick,
                        pendingMapLocationToSend = pendingMapLocationToSend,
                        onClearPendingMapLocation = onClearPendingMapLocation,
                        onBack = { onTabChange(0) },
                        tabsList = tabsList,
                        selectedTabId = selectedTabId,
                        onTabSelected = onTabSelected,
                        isProviderMode = isProviderMode,
                        onModeToggle = onModeToggle,
                        currentTheme = currentTheme,
                        currentLocation = currentLocation ?: "",
                        onProfileClick = onProfileClick,
                        userPhotoPath = userPhotoPath
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
    onSaveListingEditor: (suspend (com.estatenestora.app.data.model.ListingEditorUpdate) -> AndroidBridgeResponse?)? = null,
    onFetchProviderServiceCatalog: (suspend (String) -> AndroidBridgeResponse?)? = null,
    onSaveProviderServiceOffering: (suspend (String, com.google.gson.JsonObject) -> AndroidBridgeResponse?)? = null,
    onSaveProviderServicePackage: (suspend (String, com.google.gson.JsonObject) -> AndroidBridgeResponse?)? = null,
    onFetchMediaAssets: (suspend (String, String) -> AndroidBridgeResponse?)? = null,
    onUploadManagedMedia: (suspend (android.net.Uri, String, String, String) -> AndroidBridgeResponse)? = null,
    onArchiveMediaAsset: (suspend (String) -> AndroidBridgeResponse?)? = null,
    onResolveMedia: (suspend (String) -> String?)? = null,
    categories: List<Category>,
    onFetchAllServiceTypes: suspend () -> List<ServiceType> = { emptyList() },
    onFetchServiceAttributes: suspend (String) -> List<com.estatenestora.app.data.model.ServiceAttributeTemplate> = { emptyList() },
    onSelectLocationClick: () -> Unit = {},
    pendingMapLocationToSend: String? = null,
    onClearPendingMapLocation: () -> Unit = {},
    onBack: () -> Unit,
    tabsList: List<com.estatenestora.app.ui.theme.NestoraTab> = emptyList(),
    selectedTabId: String = "",
    onTabSelected: (String) -> Unit = {},
    isProviderMode: Boolean = false,
    onModeToggle: () -> Unit = {},
    currentTheme: com.estatenestora.app.ui.theme.RoyalTheme = com.estatenestora.app.ui.theme.RoyalThemeRepository.getThemeForToday(),
    currentLocation: String = "",
    onProfileClick: () -> Unit = {},
    userPhotoPath: String? = null,
    showHomeChrome: Boolean = true,
    initialSection: String = "listings",
    onNestedPageChanged: (Boolean) -> Unit = {}
) {
    var activeListingsSubTab by remember(initialSection) {
        mutableStateOf(providerListingSectionOrDefault(initialSection))
    }
    var workspaceReturnSection by remember(initialSection) {
        mutableStateOf(if (initialSection == "listings") "listings" else "manage")
    }
    var avTabPreset by remember { mutableStateOf("ASAP_ONLY") }
    var avTabDays by remember { mutableStateOf(setOf(0, 1, 2, 3, 4, 5, 6)) }
    var avTabStart by remember { mutableStateOf("09:00") }
    var avTabEnd by remember { mutableStateOf("18:00") }
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
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
    var pendingCategory by remember { mutableStateOf("Status") }

    // State for Full Screen Details/Edit view
    var selectedListingForDetail by remember { mutableStateOf<com.estatenestora.app.data.model.ServiceListing?>(null) }
    var listingDetailMode by remember { mutableStateOf("preview") }
    var preferredAvailabilityListingId by remember { mutableStateOf<String?>(null) }
    var preferredPackageListingId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedListingForDetail, activeListingsSubTab) {
        onNestedPageChanged(selectedListingForDetail != null || activeListingsSubTab != "listings")
    }
    DisposableEffect(Unit) {
        onDispose { onNestedPageChanged(false) }
    }

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
    var isListingsFilterVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            isListingsFilterVisible = resolveScrollAwareFilterVisibility(
                previousIndex = previousIndex,
                previousOffset = previousOffset,
                currentIndex = index,
                currentOffset = offset,
                currentlyVisible = isListingsFilterVisible
            )
            previousIndex = index
            previousOffset = offset
        }
    }

    if (selectedListingForDetail != null && listingDetailMode == "preview") {
        val previewListing = selectedListingForDetail!!
        ProviderListingPreviewPage(
            listing = previewListing,
            serviceTypes = allServiceTypes,
            onBack = { selectedListingForDetail = null },
            onEdit = { listingDetailMode = "edit" },
            onAvailability = {
                preferredAvailabilityListingId = previewListing.id
                workspaceReturnSection = "listings"
                selectedListingForDetail = null
                activeListingsSubTab = "availability"
            }
        )
    } else if (selectedListingForDetail != null) {
        val detailListing = selectedListingForDetail!!
        // Dedicated listing editor. Availability is intentionally managed on
        // its own page so saving listing details never changes working hours.
        var dTitle by remember(detailListing.id) { mutableStateOf(detailListing.title) }
        var dTagline by remember(detailListing.id) { mutableStateOf(detailListing.tagline) }
        var dDesc by remember(detailListing.id) { mutableStateOf(detailListing.description) }
        var dPrice by remember(detailListing.id) { mutableStateOf(if (detailListing.price > 0) detailListing.price.toInt().toString() else "") }
        var dPricingModel by remember(detailListing.id) { mutableStateOf(detailListing.pricingModel.ifBlank { "FIXED" }) }
        var isPricingModelMenuExpanded by remember(detailListing.id) { mutableStateOf(false) }
        var dCurrency by remember(detailListing.id) { mutableStateOf(detailListing.currencyCode.ifBlank { "INR" }) }
        var dUnitLabel by remember(detailListing.id) { mutableStateOf(detailListing.unitLabel) }
        var dPlatformNote by remember(detailListing.id) { mutableStateOf(detailListing.platformNote) }
        var dNegotiable by remember(detailListing.id) { mutableStateOf(detailListing.isNegotiable) }
        var dLocation by remember(detailListing.id) { mutableStateOf(detailListing.location) }
        var dLat by remember(detailListing.id) { mutableStateOf(detailListing.latitude) }
        var dLon by remember(detailListing.id) { mutableStateOf(detailListing.longitude) }
        var dRadius by remember(detailListing.id) { mutableStateOf(detailListing.serviceRadiusKm.toString()) }
        var dAttributes by remember(detailListing.id) { mutableStateOf(detailListing.attributes) }
        var dMediaUrls by remember(detailListing.id) { mutableStateOf(detailListing.mediaUrls.joinToString("\n")) }

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
                IconButton(onClick = { listingDetailMode = "preview" }) {
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
                value = dTagline, onValueChange = { dTagline = it },
                label = { Text("Short tagline") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = dDesc, onValueChange = { dDesc = it },
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5
            )
            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { isPricingModelMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = listingPricingModelLabel(dPricingModel),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            fontSize = 13.sp
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Choose pricing model")
                    }
                    DropdownMenu(
                        expanded = isPricingModelMenuExpanded,
                        onDismissRequest = { isPricingModelMenuExpanded = false }
                    ) {
                        listingPricingModelOptions.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(listingPricingModelLabel(model)) },
                                onClick = {
                                    dPricingModel = model
                                    isPricingModelMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = dCurrency,
                    onValueChange = { dCurrency = it.uppercase().take(3) },
                    label = { Text("Currency") },
                    modifier = Modifier.weight(0.65f),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = dUnitLabel, onValueChange = { dUnitLabel = it },
                label = { Text("Price unit, for example per visit") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = dPlatformNote, onValueChange = { dPlatformNote = it },
                label = { Text("Pricing note for customers") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4
            )
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Price is negotiable", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF20372D))
                    Text("Customers will see that the final service amount may vary.", fontSize = 11.sp, color = Color(0xFF64748B))
                }
                Switch(checked = dNegotiable, onCheckedChange = { dNegotiable = it })
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = dPrice, onValueChange = { dPrice = it.filter { c -> c.isDigit() } },
                label = { Text("Base Price (Rs.)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
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

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = dRadius,
                onValueChange = { dRadius = it.filter(Char::isDigit).take(3) },
                label = { Text("Customer coverage radius (km)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            if (dAttributes.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text("Service attributes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15362A))
                Spacer(Modifier.height(8.dp))
                dAttributes.toSortedMap().forEach { (key, value) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { updated -> dAttributes = dAttributes.toMutableMap().apply { put(key, updated) } },
                        label = { Text(key.replace("_", " ").replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = dMediaUrls,
                onValueChange = { dMediaUrls = it },
                label = { Text("Media URLs, one per line") },
                supportingText = { Text("Add up to 8 customer-visible images.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            // Save listing changes button
            var savingEdit by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    val priceVal = dPrice.toDoubleOrNull() ?: 0.0
                    val radiusVal = dRadius.toIntOrNull()
                    val mediaValues = dMediaUrls.lineSequence().map(String::trim).filter(String::isNotBlank).distinct().toList()
                    if (dTitle.isBlank()) {
                        android.widget.Toast.makeText(context, "Title cannot be empty.", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (radiusVal == null || radiusVal !in 1..200) {
                        android.widget.Toast.makeText(context, "Enter a service radius between 1 and 200 km.", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (mediaValues.size > 8) {
                        android.widget.Toast.makeText(context, "Add no more than 8 media URLs.", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        savingEdit = true
                        val locParts = dLocation.split(",")
                        val cityGuess = locParts.lastOrNull()?.trim() ?: ""
                        val fullUpdate = com.estatenestora.app.data.model.ListingEditorUpdate(
                            listingId = detailListing.id,
                            title = dTitle.trim(),
                            tagline = dTagline.trim(),
                            description = dDesc.trim(),
                            basePrice = priceVal,
                            pricingModel = dPricingModel.trim().ifBlank { "FIXED" },
                            currency = dCurrency.trim().ifBlank { "INR" },
                            unitLabel = dUnitLabel.trim(),
                            platformNote = dPlatformNote.trim(),
                            isNegotiable = dNegotiable,
                            location = dLocation.trim(),
                            city = cityGuess,
                            latitude = dLat,
                            longitude = dLon,
                            serviceRadiusKm = radiusVal,
                            attributes = dAttributes,
                            mediaUrls = mediaValues
                        )
                        val resp = onSaveListingEditor?.invoke(fullUpdate)
                            ?: onUpdateListing?.invoke(detailListing.id, dTitle, dDesc, priceVal, dLocation, cityGuess, dLat, dLon)
                        savingEdit = false
                        if (resp?.ok == true) {
                            android.widget.Toast.makeText(context, "Listing updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            val updated = detailListing.copy(
                                title = dTitle.trim(), tagline = dTagline.trim(), description = dDesc.trim(),
                                price = priceVal, pricingModel = fullUpdate.pricingModel, currencyCode = fullUpdate.currency,
                                unitLabel = fullUpdate.unitLabel, platformNote = fullUpdate.platformNote,
                                isNegotiable = dNegotiable, location = dLocation.trim(), latitude = dLat, longitude = dLon,
                                serviceRadiusKm = radiusVal, attributes = dAttributes, mediaUrls = mediaValues
                            )
                            listings = listings.map { if (it.id == updated.id) updated else it }
                            selectedListingForDetail = updated
                            listingDetailMode = "preview"
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
        val pageSurface = remember(currentTheme) { selectedMenuSurface(currentTheme) }
        val showLegacyWorkspaceBottomBar = false
        Scaffold(
            bottomBar = {
                if (showLegacyWorkspaceBottomBar) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(0.8.dp, Color(0xFFE2EAF2))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Listings
                        val isListingsSelected = activeListingsSubTab == "listings"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { activeListingsSubTab = "listings" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Listings",
                                tint = if (isListingsSelected) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Listings",
                                fontSize = 9.sp,
                                fontWeight = if (isListingsSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isListingsSelected) NestoraMint else Color(0xFF888888)
                            )
                        }

                        // 2. Availability
                        val isAvailabilitySelected = activeListingsSubTab == "availability"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { activeListingsSubTab = "availability" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Availability",
                                tint = if (isAvailabilitySelected) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Availability",
                                fontSize = 9.sp,
                                fontWeight = if (isAvailabilitySelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAvailabilitySelected) NestoraMint else Color(0xFF888888)
                            )
                        }

                        // 3. Packages
                        val isPackagesSelected = activeListingsSubTab == "packages"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { activeListingsSubTab = "packages" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Packages",
                                tint = if (isPackagesSelected) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Packages",
                                fontSize = 9.sp,
                                fontWeight = if (isPackagesSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPackagesSelected) NestoraMint else Color(0xFF888888)
                            )
                        }

                        // 4. Settings
                        val isSettingsSelected = activeListingsSubTab == "settings"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { activeListingsSubTab = "settings" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isSettingsSelected) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Settings",
                                fontSize = 9.sp,
                                fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSettingsSelected) NestoraMint else Color(0xFF888888)
                            )
                        }
                    }
                }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF6F8F7))
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                if (showHomeChrome) {
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
                } else if (activeListingsSubTab == "listings") {
                    NestoraWorkspaceHeader(
                        icon = Icons.Default.List,
                        title = "Listings",
                        actionLabel = "Manage",
                        actionIcon = Icons.Default.Settings,
                        onAction = { activeListingsSubTab = "manage" }
                    )
                }

                when (activeListingsSubTab) {
                    "manage" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF6F8F7))
                                .verticalScroll(rememberScrollState())
                        ) {
                            ProviderWorkspaceHeader(
                                title = "Manage services",
                                description = "Control when customers can book, what they can select, and how your listings are discovered.",
                                background = pageSurface,
                                onBack = { activeListingsSubTab = "listings" }
                            )
                            NestoraSectionDivider()
                            ProviderWorkspaceTools(
                                onAvailability = {
                                    preferredAvailabilityListingId = null
                                    workspaceReturnSection = "manage"
                                    activeListingsSubTab = "availability"
                                },
                                onPackages = {
                                    workspaceReturnSection = "manage"
                                    activeListingsSubTab = "packages"
                                },
                                onSettings = {
                                    workspaceReturnSection = "manage"
                                    activeListingsSubTab = "settings"
                                }
                            )
                        }
                    }

                    "listings" -> {
                        NestoraSectionDivider()

                        // Wrap filter row and list rendering inside Column matching original structure
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFFF6F8F7))
                        ) {
                            // Render Listings list, filters, loading states
                            // To make listings list scrollable along with filters, we keep the original Row & LazyColumn
                            // But wait! Since original listings list was weight(1f), we keep it as is.
                            // Let's print the rest of the listings UI:
                                                // Horizontal Filter Track (Scrollable chips matching ss1)
                    AnimatedVisibility(
                        visible = isListingsFilterVisible,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(vertical = NestoraFilterPanelSpacing),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Filter Button Chip
                        Surface(
                            modifier = Modifier.clickable {
                                pendingSort = filterSort
                                pendingStatus = filterStatus
                                pendingMinPrice = filterMinPrice
                                pendingMaxPrice = filterMaxPrice
                                pendingMinRating = filterMinRating
                                pendingCategory = "Status"
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
                                Text(
                                    text = "Filter",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasActiveFilter) Color.White else Color(0xFF333333)
                                )
                            }
                        }

                        // 3. Category Dropdown Filter Chip
                        Box {
                            Surface(
                                modifier = Modifier.clickable { categoryMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedCategoryFilter != "All") Color(0xFFE8FAF4) else Color.White,
                                border = BorderStroke(1.dp, if (selectedCategoryFilter != "All") NestoraMint else Color(0xFFDDE2E9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (selectedCategoryFilter == "All") "Category" else selectedCategoryFilter,
                                        fontSize = 12.sp,
                                        color = if (selectedCategoryFilter != "All") NestoraMint else Color(0xFF444444)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (selectedCategoryFilter != "All") NestoraMint else Color(0xFF444444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
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

                        // 4. Service Type Dropdown Filter Chip
                        Box {
                            Surface(
                                modifier = Modifier.clickable { serviceTypeMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedServiceTypeFilter != "All") Color(0xFFE8FAF4) else Color.White,
                                border = BorderStroke(1.dp, if (selectedServiceTypeFilter != "All") NestoraMint else Color(0xFFDDE2E9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (selectedServiceTypeFilter == "All") "Service Type" else selectedServiceTypeFilter.replace("_", " ").replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp,
                                        color = if (selectedServiceTypeFilter != "All") NestoraMint else Color(0xFF444444)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (selectedServiceTypeFilter != "All") NestoraMint else Color(0xFF444444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
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

                        // 5. Quick Filter: Active status
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

                        // 6. Quick Filter: Inactive status
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

                        // 7. Ratings Quick Filter
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
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (ratingSelected) "Rating: ${filterMinRating.toInt()}+" else "Ratings",
                                    fontSize = 12.sp,
                                    color = if (ratingSelected) NestoraMint else Color(0xFF444444)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (ratingSelected) NestoraMint else Color(0xFF444444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    }
                    NestoraSectionDivider()

            if (isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NestoraMint)
                }
            } else if (errorMessage != null) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(36.dp)
                        )
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
                // -- LazyColumn Container for Discovery Cards --
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 14.dp, top = NestoraFilterPanelSpacing, end = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredListings, key = { it.id }) { listing ->
                        ProviderListingCard(
                            listing = listing,
                            serviceTypes = allServiceTypes,
                            onSetListingActive = onSetListingActive,
                            onImageClick = {
                                selectedListingForDetail = it
                                listingDetailMode = "preview"
                            },
                            onAvailabilityClick = {
                                preferredAvailabilityListingId = it.id
                                workspaceReturnSection = "listings"
                                activeListingsSubTab = "availability"
                            },
                            onPackagesClick = {
                                preferredPackageListingId = it.id
                                workspaceReturnSection = "listings"
                                activeListingsSubTab = "packages"
                            },
                            onEditClick = {
                                selectedListingForDetail = it
                                listingDetailMode = "edit"
                            },
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
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
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
                    "availability" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF6F8F7))
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProviderWorkspaceHeader(
                                title = "Work availability",
                                description = "Choose when each listing can receive bookings. Customers only see times you make available.",
                                background = pageSurface,
                                onBack = { activeListingsSubTab = workspaceReturnSection }
                            )

                            if (listings.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    Text("No listings registered yet. Register a service to configure availability.", color = Color.Gray, textAlign = TextAlign.Center)
                                }
                            } else {
                                var selectedListingIdForAvailability by remember(listings, preferredAvailabilityListingId) {
                                    mutableStateOf(
                                        preferredAvailabilityListingId
                                            ?.takeIf { preferredId -> listings.any { it.id == preferredId } }
                                            ?: listings.first().id
                                    )
                                }
                                var dropdownExpanded by remember { mutableStateOf(false) }
                                val currentSelectedListing = listings.find { it.id == selectedListingIdForAvailability } ?: listings.first()

                                Text("Listing", fontWeight = FontWeight.Bold, color = Color(0xFF0F2E23), fontSize = 13.sp)
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE0E7E3))
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(currentSelectedListing.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF15231D))
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Choose listing", tint = Color(0xFF60756B))
                                    }
                                }
                                Box {
                                    DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                                        listings.forEach { l ->
                                            DropdownMenuItem(
                                                text = { Text(l.title) },
                                                onClick = {
                                                    selectedListingIdForAvailability = l.id
                                                    preferredAvailabilityListingId = null
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
                                Spacer(Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE9EFEC))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("ASAP_ONLY" to "ASAP only", "CUSTOM" to "Custom hours").forEach { (preset, label) ->
                                        val sel = avTabPreset == preset
                                        Surface(
                                            modifier = Modifier.weight(1f).clickable { avTabPreset = preset },
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (sel) NestoraMint else Color.Transparent,
                                            border = null
                                        ) {
                                            Text(
                                                label, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sel) Color.White else Color(0xFF666666),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    when (avTabPreset) {
                                        "ASAP_ONLY" -> "You will accept jobs immediately - customers can book you right now."
                                        else -> "You work on a schedule - customers book specific days & hours."
                                    }, fontSize = 11.sp, color = Color(0xFF607D72)
                                )

                                if (avTabPreset == "CUSTOM") {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Work days", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F5A47))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        (0..6).forEach { d ->
                                            val sel = avTabDays.contains(d)
                                            Surface(
                                                modifier = Modifier.size(36.dp).clickable {
                                                    avTabDays = if (sel) avTabDays - d else avTabDays + d
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
                                    Spacer(Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Start" to avTabStart to { v: String -> avTabStart = v },
                                            "End" to avTabEnd to { v: String -> avTabEnd = v })
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
                                }

                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val daysCsv = if (avTabPreset == "ASAP_ONLY") "0,1,2,3,4,5,6" else avTabDays.sorted().joinToString(",")
                                            if (avTabPreset == "CUSTOM" && daysCsv.isEmpty()) {
                                                android.widget.Toast.makeText(context, "Pick at least one day.", android.widget.Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }
                                            val r = onSetCustomProviderAvailability?.invoke(currentSelectedListing.id, daysCsv, avTabStart, avTabEnd)
                                            if (r != null) {
                                                val readableDays = if (avTabPreset == "ASAP_ONLY") "All Days" else avTabDays.sorted().joinToString(", ") { dayNames[it] }
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Availability saved for $readableDays ($avTabStart - $avTabEnd)",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Could not save. Try again.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Save availability", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                    "packages" -> {
                        ProviderPackagesWorkspace(
                            listings = listings,
                            initialListingId = preferredPackageListingId,
                            pageSurface = pageSurface,
                            onBack = {
                                preferredPackageListingId = null
                                activeListingsSubTab = workspaceReturnSection
                            },
                            onFetchCatalog = onFetchProviderServiceCatalog,
                            onSaveOffering = onSaveProviderServiceOffering,
                            onSavePackage = onSaveProviderServicePackage,
                            onFetchMediaAssets = onFetchMediaAssets,
                            onUploadManagedMedia = onUploadManagedMedia,
                            onArchiveMediaAsset = onArchiveMediaAsset,
                            onResolveMedia = onResolveMedia,
                            onFetchServiceAttributes = onFetchServiceAttributes
                        )
                    }
                    "settings" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF6F8F7))
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ProviderWorkspaceHeader(
                                title = "Listing settings",
                                description = "Review how customers discover your services. Edit listing-specific information from each listing card.",
                                background = pageSurface,
                                onBack = { activeListingsSubTab = workspaceReturnSection }
                            )
                            ProviderSettingsCard(
                                title = "Customer matching",
                                description = "Location and service type decide which nearby customers can discover a listing.",
                                rows = listOf(
                                    "Service radius" to "Managed per listing",
                                    "Availability" to "Managed in Availability"
                                )
                            )
                            ProviderSettingsCard(
                                title = "Listing visibility",
                                description = "Only active listings appear in customer search and discovery.",
                                rows = listOf(
                                    "Active status" to "Managed on each listing card",
                                    "Pricing and media" to "Managed in listing details"
                                )
                            )
                            Text(
                                text = "Keeping these details accurate improves matching and prevents requests for unavailable services.",
                                color = Color(0xFF60756B),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // -- Full-screen listing filters --
    if (showFilterSheet) {
        FilterOverlaySheet(
            title = "Filter listings",
            onDismissRequest = { showFilterSheet = false }
        ) {
                val filterNavItems = listOf("Status", "Ratings", "Price Range")
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(
                        modifier = Modifier
                            .width(130.dp)
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

                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 1.dp,
                        color = FilterPaneDividerColor
                    )

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
                                        Text(if (rating == 0f) "Any rating" else "Rating: ${rating}+", fontSize = 13.sp, color = Color(0xFF333333))
                                    }
                                }
                            }
                            "Price Range" -> {
                                Text("PRICE RANGE (Rs.)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
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

                HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
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
                        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(130.dp)
                    ) {
                        Text("Apply", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
        }
    }
}

private val listingPricingModelOptions = listOf(
    "STARTING_FROM", "FIXED", "PER_VISIT", "HOURLY", "PER_DAY",
    "PER_SQFT", "PER_PLATE", "COMMISSION_PCT", "NEGOTIABLE"
)

private fun listingPricingModelLabel(model: String): String = when (model) {
    "STARTING_FROM" -> "Starting from"
    "FIXED" -> "Fixed price"
    "PER_VISIT" -> "Per visit"
    "HOURLY" -> "Hourly"
    "PER_DAY" -> "Per day"
    "PER_SQFT" -> "Per sq ft"
    "PER_PLATE" -> "Per plate"
    "COMMISSION_PCT" -> "Commission percent"
    "NEGOTIABLE" -> "Negotiable"
    else -> "Starting from"
}

@Composable
private fun ProviderWorkspaceTools(
    onAvailability: () -> Unit,
    onPackages: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Service management",
            color = Color(0xFF0F2E23),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Each setting has one clear home, so changes remain easy to find and maintain.",
            color = Color(0xFF60756B),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(14.dp))
        providerListingTools().forEachIndexed { index, tool ->
            val (icon, onClick) = when (tool) {
                ProviderListingTool.Availability -> Icons.Default.LocationOn to onAvailability
                ProviderListingTool.Packages -> Icons.Default.Star to onPackages
                ProviderListingTool.Settings -> Icons.Default.Settings to onSettings
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClick,
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF7FAF9),
                border = BorderStroke(1.dp, Color(0xFFDCE8E3))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE7F3EE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(21.dp))
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            text = tool.label,
                            color = Color(0xFF17251F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tool.description,
                            color = Color(0xFF60756B),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open ${tool.label}",
                        tint = Color(0xFF7A8983),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (index != providerListingTools().lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ProviderWorkspaceHeader(
    title: String,
    description: String,
    background: Color,
    onBack: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF10231B))
                    }
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF10231B),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = Color(0xFF60756B),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }
            HorizontalDivider(color = Color(0xFFE8ECEA), thickness = 1.dp)
        }
    }
}

@Composable
private fun ProviderComingSoonFeature(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E7E3))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F5F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF0F3F1)) {
                        Text(
                            text = "Coming soon",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF60756B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(description, color = Color(0xFF60756B), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun ProviderPackagesWorkspace(
    listings: List<com.estatenestora.app.data.model.ServiceListing>,
    initialListingId: String?,
    pageSurface: Color,
    onBack: () -> Unit,
    onFetchCatalog: (suspend (String) -> AndroidBridgeResponse?)?,
    onSaveOffering: (suspend (String, com.google.gson.JsonObject) -> AndroidBridgeResponse?)?,
    onSavePackage: (suspend (String, com.google.gson.JsonObject) -> AndroidBridgeResponse?)?,
    onFetchMediaAssets: (suspend (String, String) -> AndroidBridgeResponse?)?,
    onUploadManagedMedia: (suspend (android.net.Uri, String, String, String) -> AndroidBridgeResponse)?,
    onArchiveMediaAsset: (suspend (String) -> AndroidBridgeResponse?)?,
    onResolveMedia: (suspend (String) -> String?)?,
    onFetchServiceAttributes: suspend (String) -> List<com.estatenestora.app.data.model.ServiceAttributeTemplate>
) {
    var selectedListingId by remember(listings, initialListingId) {
        mutableStateOf(listings.firstOrNull { it.id == initialListingId }?.id ?: listings.firstOrNull()?.id.orEmpty())
    }
    var catalog by remember { mutableStateOf<com.estatenestora.app.data.model.ListingServiceCatalog?>(null) }
    var loading by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf("overview") }
    var editingOffer by remember { mutableStateOf<com.estatenestora.app.data.model.ProviderServiceOffering?>(null) }
    var editingPackage by remember { mutableStateOf<com.estatenestora.app.data.model.ProviderServicePackage?>(null) }
    var mediaTarget by remember { mutableStateOf<ProviderMediaTarget?>(null) }
    var attributeTemplates by remember { mutableStateOf<List<com.estatenestora.app.data.model.ServiceAttributeTemplate>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun reload() {
        if (selectedListingId.isBlank() || onFetchCatalog == null) return
        scope.launch {
            loading = true
            val response = onFetchCatalog(selectedListingId)
            catalog = response?.serviceCatalog
            feedback = if (response?.ok == true) null else response?.reply ?: "Could not load packages. Try again."
            feedbackIsError = response?.ok != true
            loading = false
        }
    }
    LaunchedEffect(selectedListingId) {
        reload()
        attributeTemplates = try { onFetchServiceAttributes(listings.firstOrNull { it.id == selectedListingId }?.serviceType.orEmpty()) } catch (_: Exception) { emptyList() }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF6F8F7)).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProviderWorkspaceHeader(
            title = when (section) { "overview" -> "Packages"; "offer" -> "Work item"; "media" -> "Service images"; else -> "Package details" },
            description = when (section) {
                "overview" -> "Create clear work items, then combine work from this one service type into customer-ready packages."
                "offer" -> "A work item is one job you can price and schedule on its own."
                "media" -> "Keep clear, current images. Nestora creates lightweight thumbnail, card and hero sizes automatically."
                else -> "A package can contain several of your work items for this same service type only."
            },
            background = pageSurface,
            onBack = if (section == "overview") onBack else { { section = "overview"; editingOffer = null; editingPackage = null; mediaTarget = null } }
        )

        if (listings.isEmpty()) {
            Text("Register a service listing before creating packages.", color = Color(0xFF60756B), fontSize = 13.sp)
        } else {
            var listingMenuOpen by remember { mutableStateOf(false) }
            val listing = listings.firstOrNull { it.id == selectedListingId } ?: listings.first()
            Box {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { listingMenuOpen = true },
                    shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFDCE8E3))
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Package workspace · shared across your ${listing.serviceType} listings", fontSize = 10.sp, color = Color(0xFF60756B))
                            Text(listing.serviceType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
                        }
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF60756B))
                    }
                }
                DropdownMenu(expanded = listingMenuOpen, onDismissRequest = { listingMenuOpen = false }) {
                    listings.forEach { item ->
                        DropdownMenuItem(text = { Text(item.title) }, onClick = { selectedListingId = item.id; listingMenuOpen = false; section = "overview" })
                    }
                }
            }

            when (section) {
                "media" -> mediaTarget?.let { target ->
                    ProviderMediaManager(
                        target = target,
                        onFetchMediaAssets = onFetchMediaAssets,
                        onUploadManagedMedia = onUploadManagedMedia,
                        onArchiveMediaAsset = onArchiveMediaAsset,
                        onResolveMedia = onResolveMedia
                    )
                }
                "offer" -> ProviderWorkItemEditor(
                    existing = editingOffer,
                    attributeTemplates = attributeTemplates,
                    saving = loading,
                    feedback = feedback?.takeIf { feedbackIsError },
                    onSave = { payload ->
                        scope.launch {
                            loading = true
                            feedback = null
                            val response = onSaveOffering?.invoke(selectedListingId, payload)
                            if (response?.ok == true && response.serviceCatalog != null) catalog = response.serviceCatalog
                            feedback = response?.reply ?: "Could not save this work item."
                            feedbackIsError = response?.ok != true
                            loading = false
                            if (response?.ok == true) { section = "overview"; editingOffer = null }
                        }
                    }
                )

                "package" -> ProviderPackageEditor(
                    existing = editingPackage,
                    offerings = catalog?.offerings.orEmpty(),
                    attributeTemplates = attributeTemplates,
                    saving = loading,
                    feedback = feedback?.takeIf { feedbackIsError },
                    onCreateOffering = { payload ->
                        val response = onSaveOffering?.invoke(selectedListingId, payload)
                        if (response?.ok == true && response.serviceCatalog != null) {
                            catalog = response.serviceCatalog
                        }
                        response
                    },
                    onSave = { payload ->
                        scope.launch {
                            loading = true
                            feedback = null
                            val response = onSavePackage?.invoke(selectedListingId, payload)
                            if (response?.ok == true && response.serviceCatalog != null) catalog = response.serviceCatalog
                            feedback = response?.reply ?: "Could not save this package."
                            feedbackIsError = response?.ok != true
                            loading = false
                            if (response?.ok == true) { section = "overview"; editingPackage = null }
                        }
                    }
                )

                else -> {
                    feedback?.let { Text(it, color = if (feedbackIsError) Color(0xFFB3261E) else Color(0xFF0F5A47), fontSize = 12.sp) }
                    if (loading) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NestoraMint) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { editingOffer = null; section = "offer" }, modifier = Modifier.weight(1f)) { Text("Add work item") }
                        Button(onClick = { editingPackage = null; section = "package" }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)) { Text("Create package") }
                    }
                    OutlinedButton(
                        onClick = { mediaTarget = ProviderMediaTarget("LISTING", selectedListingId, listing.title); section = "media" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Manage listing photos") }
                    Text("Individual work items", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
                    if (catalog?.offerings.isNullOrEmpty()) {
                        ProviderCatalogEmptyState("Add the specific jobs you offer first. Customers may request one item or a package.")
                    } else {
                        catalog!!.offerings.forEach { offer ->
                            ProviderCatalogRow(
                                title = offer.title, subtitle = "₹${offer.priceAmount.toInt()} · ${offer.durationMinutes} min · ${if (offer.isActive) "Active" else "Hidden"}",
                                onClick = { editingOffer = offer; section = "offer" },
                                onManageImages = { mediaTarget = ProviderMediaTarget("OFFERING", offer.id, offer.title); section = "media" }
                            )
                        }
                    }
                    Text("Customer packages", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
                    if (catalog?.packages.isNullOrEmpty()) {
                        ProviderCatalogEmptyState("Packages must contain your active work items and are never shared across service types.")
                    } else {
                        catalog!!.packages.forEach { pack ->
                            val packageItemCount = pack.items.sumOf { it.quantity.coerceAtLeast(1) }
                            val savings = providerPackageSavings(pack)
                            ProviderCatalogRow(
                                title = pack.name, subtitle = buildString {
                                    append("₹${pack.packagePriceAmount.toInt()} · ${pack.durationMinutes} min · $packageItemCount included · ${pack.status.lowercase().replaceFirstChar { it.uppercase() }}")
                                    if (savings > 0) append(" · saves ₹${savings.toInt()}")
                                },
                                onClick = { editingPackage = pack; section = "package" },
                                onManageImages = { mediaTarget = ProviderMediaTarget("PACKAGE", pack.id, pack.name); section = "media" }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ProviderMediaTarget(val scope: String, val id: String, val title: String)

@Composable
private fun ProviderMediaManager(
    target: ProviderMediaTarget,
    onFetchMediaAssets: (suspend (String, String) -> AndroidBridgeResponse?)?,
    onUploadManagedMedia: (suspend (android.net.Uri, String, String, String) -> AndroidBridgeResponse)?,
    onArchiveMediaAsset: (suspend (String) -> AndroidBridgeResponse?)?,
    onResolveMedia: (suspend (String) -> String?)?
) {
    val scope = rememberCoroutineScope()
    var assets by remember(target) { mutableStateOf<List<com.estatenestora.app.data.model.MediaAsset>>(emptyList()) }
    var loading by remember(target) { mutableStateOf(false) }
    var feedback by remember(target) { mutableStateOf<String?>(null) }
    var selectedRole by remember(target) { mutableStateOf("PRIMARY") }

    fun reload() {
        if (onFetchMediaAssets == null) return
        scope.launch {
            loading = true
            val response = onFetchMediaAssets(target.scope, target.id)
            assets = response?.mediaAssets.orEmpty().filter { it.status == "ACTIVE" }
            if (response?.ok != true) feedback = response?.reply ?: "Could not load images."
            loading = false
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && onUploadManagedMedia != null) {
            scope.launch {
                loading = true
                feedback = "Optimizing and saving image..."
                val response = onUploadManagedMedia(uri, target.scope, target.id, selectedRole)
                feedback = response.reply
                loading = false
                if (response.ok) reload()
            }
        }
    }
    LaunchedEffect(target) { reload() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(target.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF15231D))
        Text(
            "Primary is the main customer card image. Gallery images show additional work details. Upload JPG, PNG or GIF; Nestora crops and compresses it for each screen.",
            color = Color(0xFF60756B), fontSize = 12.sp, lineHeight = 17.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedRole == "PRIMARY", onClick = { selectedRole = "PRIMARY" }, label = { Text("Primary") })
            FilterChip(selected = selectedRole == "GALLERY", onClick = { selectedRole = "GALLERY" }, label = { Text("Gallery") })
        }
        Button(
            onClick = { picker.launch("image/*") },
            enabled = !loading && onUploadManagedMedia != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
        ) { Text(if (selectedRole == "PRIMARY") "Choose primary image" else "Add gallery image", fontWeight = FontWeight.Bold) }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NestoraMint)
        feedback?.let { Text(it, color = Color(0xFF60756B), fontSize = 12.sp) }
        if (!loading && assets.isEmpty()) {
            ProviderCatalogEmptyState("No managed images yet. Customers will continue to see Nestora's service fallback image.")
        }
        assets.forEach { asset ->
            ProviderManagedMediaRow(
                asset = asset,
                onResolveMedia = onResolveMedia,
                onRemove = {
                    if (onArchiveMediaAsset != null) scope.launch {
                        loading = true
                        val response = onArchiveMediaAsset(asset.id)
                        feedback = response?.reply ?: "Could not remove image."
                        loading = false
                        if (response?.ok == true) reload()
                    }
                }
            )
        }
    }
}

@Composable
private fun ProviderManagedMediaRow(
    asset: com.estatenestora.app.data.model.MediaAsset,
    onResolveMedia: (suspend (String) -> String?)?,
    onRemove: () -> Unit
) {
    val fileId = remember(asset.id) { asset.fileIdFor("THUMBNAIL") }
    var localPath by remember(fileId) { mutableStateOf<String?>(null) }
    LaunchedEffect(fileId) { localPath = if (fileId != null) onResolveMedia?.invoke(fileId) else null }
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFDCE8E3))) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(78.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE7F1ED)), contentAlignment = Alignment.Center) {
                if (!localPath.isNullOrBlank()) AsyncImage(model = localPath, contentDescription = asset.role, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF60756B))
            }
            Column(Modifier.weight(1f)) {
                Text(asset.role.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                val card = asset.variants.firstOrNull { it.variant == "CARD" }
                Text(card?.let { "${it.width} x ${it.height} optimized" } ?: "Optimized image", color = Color(0xFF60756B), fontSize = 11.sp)
            }
            TextButton(onClick = onRemove) { Text("Remove", color = Color(0xFFB3261E)) }
        }
    }
}

@Composable
private fun ProviderCatalogEmptyState(message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFDCE8E3))) {
        Text(message, modifier = Modifier.padding(14.dp), color = Color(0xFF60756B), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun ProviderCatalogRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onManageImages: (() -> Unit)? = null
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFDCE8E3))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onClick)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF60756B))
            }
            onManageImages?.let { action ->
                TextButton(onClick = action) { Text("Images", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

internal fun providerPackageItemPayloads(
    offerings: List<com.estatenestora.app.data.model.ProviderServiceOffering>,
    quantities: Map<String, Int>
): com.google.gson.JsonArray = com.google.gson.JsonArray().apply {
    offerings.filter { offer -> (quantities[offer.id] ?: 0) in 1..25 }.take(25).forEachIndexed { index, offer ->
        add(com.google.gson.JsonObject().apply {
            addProperty("offering_id", offer.id)
            addProperty("quantity", quantities.getValue(offer.id))
            addProperty("display_order", index)
        })
    }
}

internal fun newlyCreatedPackageOffering(
    previousOfferingIds: Set<String>,
    response: AndroidBridgeResponse?
): com.estatenestora.app.data.model.ProviderServiceOffering? {
    if (response?.ok != true) return null
    return response.serviceCatalog?.offerings?.firstOrNull { it.id !in previousOfferingIds }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderWorkItemEditor(
    existing: com.estatenestora.app.data.model.ProviderServiceOffering?,
    attributeTemplates: List<com.estatenestora.app.data.model.ServiceAttributeTemplate>,
    saving: Boolean,
    feedback: String?,
    forceActive: Boolean = false,
    onSave: (com.google.gson.JsonObject) -> Unit
) {
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var price by remember(existing) { mutableStateOf(existing?.priceAmount?.toInt()?.toString().orEmpty()) }
    var duration by remember(existing) { mutableStateOf(existing?.durationMinutes?.toString().orEmpty()) }
    var active by remember(existing) { mutableStateOf(existing?.isActive ?: true) }
    val attributeValues = remember(existing, attributeTemplates) {
        mutableStateMapOf<String, String>().apply {
            attributeTemplates.forEach { template ->
                val saved = existing?.attributeValues?.get(template.key)
                put(template.key, when {
                    saved == null || saved.isJsonNull -> ""
                    saved.isJsonArray -> saved.asJsonArray.joinToString(", ") { it.asString }
                    saved.isJsonPrimitive -> saved.asString
                    else -> ""
                })
            }
        }
    }
    OutlinedTextField(title, { title = it }, label = { Text("Work item name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(description, { description = it }, label = { Text("What is included") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Price (₹)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        OutlinedTextField(duration, { duration = it.filter(Char::isDigit) }, label = { Text("Minutes") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
    }
    if (forceActive) {
        Text("This work item will be active and added to the package with quantity 1.", fontSize = 12.sp, color = Color(0xFF14513D), fontWeight = FontWeight.SemiBold)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Switch(checked = active, onCheckedChange = { active = it })
            Spacer(Modifier.width(8.dp))
            Text(if (active) "Visible for customer selection" else "Hidden from new customers", fontSize = 12.sp, color = Color(0xFF60756B))
        }
    }
    if (attributeTemplates.isNotEmpty()) {
        Text("Service details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
        Text("Add the details that make this work item clear to customers.", fontSize = 11.sp, color = Color(0xFF60756B))
        attributeTemplates.forEach { template ->
            val value = attributeValues[template.key].orEmpty()
            val label = if (template.isRequired) "${template.displayLabel} *" else template.displayLabel
            when (template.inputType) {
                "boolean" -> {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF24362E))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Yes" to "true", "No" to "false").forEach { (text, stored) ->
                            FilterChip(selected = value == stored, onClick = { attributeValues[template.key] = if (value == stored) "" else stored }, label = { Text(text) })
                        }
                    }
                }
                "select", "multiselect" -> {
                    val options = template.options.orEmpty()
                    if (options.isNotEmpty()) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF24362E))
                        val selected = value.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            options.forEach { option ->
                                val chosen = option in selected
                                FilterChip(selected = chosen, onClick = {
                                    val updated = if (template.inputType == "select") {
                                        if (chosen) emptySet() else setOf(option)
                                    } else if (chosen) selected - option else selected + option
                                    attributeValues[template.key] = updated.joinToString(", ")
                                }, label = { Text(option) })
                            }
                        }
                    } else {
                        OutlinedTextField(value, { attributeValues[template.key] = it.take(180) }, label = { Text(label) }, supportingText = template.hintText?.let { hint -> { Text(hint) } }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }
                "number" -> OutlinedTextField(value, { input -> if (input.all { it.isDigit() || it == '.' }) attributeValues[template.key] = input }, label = { Text(label) }, supportingText = template.hintText?.let { hint -> { Text(hint) } }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                else -> OutlinedTextField(value, { attributeValues[template.key] = it.take(180) }, label = { Text(label) }, supportingText = template.hintText?.let { hint -> { Text(hint) } }, modifier = Modifier.fillMaxWidth(), singleLine = template.inputType != "text")
            }
        }
    }
    feedback?.let { Text(it, color = Color(0xFFB3261E), fontSize = 12.sp) }
    if (saving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NestoraMint)
    Button(
        onClick = {
            val payload = com.google.gson.JsonObject().apply {
                if (existing != null) addProperty("id", existing.id)
                addProperty("title", title); addProperty("description", description)
                add("attribute_values", com.google.gson.JsonObject().apply {
                    attributeTemplates.forEach { template ->
                        val value = attributeValues[template.key].orEmpty()
                        if (value.isBlank()) return@forEach
                        when (template.inputType) {
                            "boolean" -> addProperty(template.key, value == "true")
                            "number" -> value.toDoubleOrNull()?.let { addProperty(template.key, it) } ?: addProperty(template.key, value)
                            "multiselect" -> add(template.key, com.google.gson.JsonArray().apply { value.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach(::add) })
                            else -> addProperty(template.key, value)
                        }
                    }
                })
                addProperty("price_amount", price.toDoubleOrNull() ?: -1.0); addProperty("duration_minutes", duration.toIntOrNull() ?: 0)
                addProperty("is_active", if (forceActive) true else active); addProperty("display_order", existing?.displayOrder ?: 0)
            }
            onSave(payload)
        }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
    ) { Text(if (saving) "Saving work item" else if (existing == null) "Save work item" else "Save changes") }
}

@Composable
private fun ProviderPackageEditor(
    existing: com.estatenestora.app.data.model.ProviderServicePackage?,
    offerings: List<com.estatenestora.app.data.model.ProviderServiceOffering>,
    attributeTemplates: List<com.estatenestora.app.data.model.ServiceAttributeTemplate>,
    saving: Boolean,
    feedback: String?,
    onCreateOffering: suspend (com.google.gson.JsonObject) -> AndroidBridgeResponse?,
    onSave: (com.google.gson.JsonObject) -> Unit
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember(existing) { mutableStateOf(existing?.description.orEmpty()) }
    var included by remember(existing) { mutableStateOf(existing?.includedText.orEmpty()) }
    var excluded by remember(existing) { mutableStateOf(existing?.excludedText.orEmpty()) }
    var price by remember(existing) { mutableStateOf(existing?.packagePriceAmount?.toInt()?.toString().orEmpty()) }
    var duration by remember(existing) { mutableStateOf(existing?.durationMinutes?.toString().orEmpty()) }
    var status by remember(existing) { mutableStateOf(existing?.status ?: "DRAFT") }
    var showNewWorkItem by remember(existing?.id) { mutableStateOf(false) }
    var creatingWorkItem by remember(existing?.id) { mutableStateOf(false) }
    var createWorkItemError by remember(existing?.id) { mutableStateOf<String?>(null) }
    val editorScope = rememberCoroutineScope()
    val chosenQuantities = remember(existing?.id) {
        mutableStateMapOf<String, Int>().apply {
            existing?.items?.forEach { item -> put(item.id, item.quantity.coerceIn(1, 25)) }
        }
    }
    OutlinedTextField(name, { name = it }, label = { Text("Package name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(description, { description = it }, label = { Text("Package summary") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    OutlinedTextField(included, { included = it }, label = { Text("Included details") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    OutlinedTextField(excluded, { excluded = it }, label = { Text("Not included (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    Text("Choose work items and quantities", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
    Text("Select existing work or create a new item here. A newly saved item is added to this package with quantity 1.", fontSize = 11.sp, color = Color(0xFF60756B))
    OutlinedButton(
        onClick = {
            createWorkItemError = null
            showNewWorkItem = true
        },
        enabled = !saving && !creatingWorkItem,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, NestoraMint)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text("Add new work item", color = NestoraMint, fontWeight = FontWeight.Bold)
    }
    if (offerings.isEmpty()) {
        ProviderCatalogEmptyState("No work items yet. Create one above and it will be selected automatically for this package.")
    }
    offerings.forEach { offer ->
        val quantity = chosenQuantities[offer.id] ?: 0
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = if (quantity > 0) Color(0xFFE7F7F1) else Color(0xFFF7F9F8),
            border = BorderStroke(1.dp, if (quantity > 0) NestoraMint else Color(0xFFD9E3DF))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(offer.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF15231D))
                    Text(
                        if (offer.isActive) "Available to customers" else "Inactive work item — publish only after activating it",
                        fontSize = 11.sp,
                        color = if (offer.isActive) Color(0xFF60756B) else Color(0xFF8A4B00)
                    )
                }
                TextButton(
                    onClick = {
                        if (quantity <= 1) chosenQuantities.remove(offer.id) else chosenQuantities[offer.id] = quantity - 1
                    },
                    enabled = quantity > 0
                ) { Text("-") }
                Text(quantity.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center)
                TextButton(
                    onClick = {
                        when {
                            quantity == 0 && offer.isActive && chosenQuantities.size < 25 -> chosenQuantities[offer.id] = 1
                            quantity in 1..24 && offer.isActive -> chosenQuantities[offer.id] = quantity + 1
                        }
                    },
                    enabled = offer.isActive && (quantity in 1..24 || (quantity == 0 && chosenQuantities.size < 25))
                ) { Text("+") }
            }
        }
    }
    val selectedOffers = offerings.filter { (chosenQuantities[it.id] ?: 0) > 0 }
    val selectedUnitCount = selectedOffers.sumOf { chosenQuantities[it.id] ?: 0 }
    val individualTotal = selectedOffers.sumOf { offer -> offer.priceAmount * (chosenQuantities[offer.id] ?: 0) }
    val individualDuration = selectedOffers.sumOf { offer -> offer.durationMinutes * (chosenQuantities[offer.id] ?: 0) }
    if (selectedOffers.isNotEmpty()) {
        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF0F8F4), modifier = Modifier.fillMaxWidth()) {
            Text(
                "$selectedUnitCount included item(s) · ₹${individualTotal.toInt()} when booked separately · $individualDuration min",
                modifier = Modifier.padding(10.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF14513D)
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Package price (₹)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        OutlinedTextField(duration, { duration = it.filter(Char::isDigit) }, label = { Text("Minutes") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
    }
    price.toDoubleOrNull()?.takeIf { selectedOffers.isNotEmpty() }?.let { packagePrice ->
        when {
            individualTotal > packagePrice -> Text("Customers save ₹${(individualTotal - packagePrice).toInt()} with this package.", fontSize = 12.sp, color = Color(0xFF14513D), fontWeight = FontWeight.SemiBold)
            individualTotal < packagePrice -> Text("This package is ₹${(packagePrice - individualTotal).toInt()} above the selected items separately.", fontSize = 12.sp, color = Color(0xFF8A4B00))
            else -> Text("This package matches the selected items' separate total.", fontSize = 12.sp, color = Color(0xFF60756B))
        }
    }
    Text("Package visibility", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        listOf("DRAFT" to "Draft", "PUBLISHED" to "Published", "PAUSED" to "Paused", "ARCHIVED" to "Archived").forEach { (value, label) ->
            FilterChip(selected = status == value, onClick = { status = value }, label = { Text(label) })
        }
    }
    Text(
        when (status) {
            "PUBLISHED" -> "Published packages are visible to customers with their fixed scope and provider amount."
            "PAUSED" -> "Paused packages are hidden from customers and can be published again later."
            "ARCHIVED" -> "Archived packages are hidden from customers and retained only for your records."
            else -> "Draft packages are private until you publish them."
        },
        fontSize = 11.sp, color = Color(0xFF60756B)
    )
    feedback?.let { Text(it, color = Color(0xFFB3261E), fontSize = 12.sp) }
    if (saving) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NestoraMint)
    Button(
        onClick = {
            val items = providerPackageItemPayloads(offerings, chosenQuantities)
            onSave(com.google.gson.JsonObject().apply {
                if (existing != null) addProperty("id", existing.id)
                addProperty("name", name); addProperty("description", description); addProperty("included_text", included); addProperty("excluded_text", excluded)
                addProperty("package_price_amount", price.toDoubleOrNull() ?: -1.0); addProperty("duration_minutes", duration.toIntOrNull() ?: 0)
                addProperty("status", status); addProperty("display_order", existing?.displayOrder ?: 0); add("items", items)
            })
        }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
    ) { Text(if (saving) "Saving package" else when (status) { "PUBLISHED" -> "Publish package"; "PAUSED" -> "Pause package"; "ARCHIVED" -> "Archive package"; else -> "Save draft" }) }

    if (showNewWorkItem) {
        FullScreenModalSheet(
            title = "New work item",
            onDismissRequest = { if (!creatingWorkItem) showNewWorkItem = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProviderWorkItemEditor(
                    existing = null,
                    attributeTemplates = attributeTemplates,
                    saving = creatingWorkItem,
                    feedback = createWorkItemError,
                    forceActive = true,
                    onSave = { payload ->
                        editorScope.launch {
                            val previousIds = offerings.mapTo(mutableSetOf()) { it.id }
                            creatingWorkItem = true
                            createWorkItemError = null
                            val response = onCreateOffering(payload)
                            val created = newlyCreatedPackageOffering(previousIds, response)
                            if (created != null) {
                                chosenQuantities[created.id] = 1
                                showNewWorkItem = false
                            } else {
                                createWorkItemError = if (response?.ok == true) {
                                    "The work item was saved, but Nestora could not add it to this package automatically. Close and reopen the package to select it."
                                } else {
                                    response?.reply ?: "The work item could not be created. Check the details and try again."
                                }
                            }
                            creatingWorkItem = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProviderSettingsCard(
    title: String,
    description: String,
    rows: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E7E3))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
            Spacer(Modifier.height(5.dp))
            Text(description, color = Color(0xFF60756B), fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(14.dp))
            rows.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, modifier = Modifier.weight(1f), color = Color(0xFF24362E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(value, color = Color(0xFF60756B), fontSize = 11.sp, textAlign = TextAlign.End)
                }
                if (index < rows.lastIndex) {
                    HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProviderListingPreviewPage(
    listing: com.estatenestora.app.data.model.ServiceListing,
    serviceTypes: List<ServiceType>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAvailability: () -> Unit
) {
    val resolvedType = remember(listing.serviceType, serviceTypes) {
        serviceTypes.firstOrNull { it.slug == listing.serviceType }
    }
    val serviceName = resolvedType?.name
        ?: listing.serviceType.replace("_", " ").replaceFirstChar { it.uppercase() }
    val categoryName = listing.categoryName.replace("_", " ").replaceFirstChar { it.uppercase() }
    val images = remember(listing.id, listing.mediaUrls) {
        listing.mediaUrls.filter { it.isNotBlank() }.ifEmpty { getServiceTypeImages(listing.serviceType) }
    }
    val pagerState = rememberPagerState(pageCount = { images.size })

    NestoraTaskScaffold(title = "Listing details", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF6F8F7)),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(Color(0xFFE8ECEA))) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        AsyncImage(
                            model = images[page],
                            contentDescription = "$serviceName listing image ${page + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (images.size > 1) {
                        Row(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            images.indices.forEach { index ->
                                Box(
                                    Modifier
                                        .height(6.dp)
                                        .width(if (pagerState.currentPage == index) 18.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = if (pagerState.currentPage == index) 1f else 0.58f))
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(listing.title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10231B))
                            Spacer(Modifier.height(4.dp))
                            Text("$categoryName  •  $serviceName", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (listing.isActive) Color(0xFFE6F5EE) else Color(0xFFFFECEC)
                        ) {
                            Text(
                                if (listing.isActive) "ACTIVE" else "INACTIVE",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (listing.isActive) Color(0xFF08724F) else Color(0xFFB42318)
                            )
                        }
                    }
                    if (listing.tagline.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(listing.tagline, fontSize = 13.sp, color = Color(0xFF42564D))
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF0F5F3)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("All bookings", fontSize = 12.sp, color = Color(0xFF53675E))
                            Text(listing.totalBookingCount.toString(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = NestoraMint)
                        }
                    }
                }
            }
            item { NestoraSectionDivider() }
            item {
                ProviderListingDetailSection("Service information") {
                    ProviderListingDetailRow("Starting price", "${listing.currencyCode} ${listing.price.toInt()}")
                    ProviderListingDetailRow("Pricing model", listing.pricingModel.ifBlank { "Starting price" })
                    if (listing.unitLabel.isNotBlank()) ProviderListingDetailRow("Price unit", listing.unitLabel)
                    ProviderListingDetailRow("Negotiable", if (listing.isNegotiable) "Yes" else "No")
                    ProviderListingDetailRow("Rating", if (listing.rating > 0f) String.format("%.1f", listing.rating) else "No ratings yet")
                    ProviderListingDetailRow("Provider", listing.providerName)
                }
            }
            if (listing.description.isNotBlank() || listing.platformNote.isNotBlank()) {
                item { NestoraSectionDivider() }
                item {
                    ProviderListingDetailSection("About this listing") {
                        if (listing.description.isNotBlank()) Text(listing.description, fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF42564D))
                        if (listing.platformNote.isNotBlank()) {
                            if (listing.description.isNotBlank()) Spacer(Modifier.height(10.dp))
                            Text(listing.platformNote, fontSize = 12.sp, lineHeight = 17.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }
            item { NestoraSectionDivider() }
            item {
                ProviderListingDetailSection("Service location") {
                    ProviderListingDetailRow("Address", listing.location.ifBlank { "Not added" })
                    ProviderListingDetailRow("Customer coverage", "${listing.serviceRadiusKm} km radius")
                    if (listing.latitude != 0.0 || listing.longitude != 0.0) {
                        ProviderListingDetailRow("Map location", "%.5f, %.5f".format(listing.latitude, listing.longitude))
                    }
                }
            }
            if (listing.attributes.isNotEmpty()) {
                item { NestoraSectionDivider() }
                item {
                    ProviderListingDetailSection("Service attributes") {
                        listing.attributes.toSortedMap().forEach { (key, value) ->
                            ProviderListingDetailRow(
                                key.replace("_", " ").replaceFirstChar { it.uppercase() },
                                value.ifBlank { "Not specified" }
                            )
                        }
                    }
                }
            }
            item { NestoraSectionDivider() }
            item {
                ProviderListingDetailSection("Manage this listing") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProviderListingActionCard(
                            icon = Icons.Default.DateRange,
                            title = "Availability",
                            supporting = "Hours and booking slots",
                            onClick = onAvailability,
                            modifier = Modifier.weight(1f)
                        )
                        ProviderListingActionCard(
                            icon = Icons.Default.Create,
                            title = "Edit listing",
                            supporting = "Pricing and details",
                            onClick = onEdit,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderListingDetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10231B))
        content()
    }
}

@Composable
private fun ProviderListingDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, modifier = Modifier.weight(0.42f), fontSize = 12.sp, color = Color(0xFF718078))
        Text(value, modifier = Modifier.weight(0.58f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF24372F), textAlign = TextAlign.End)
    }
}

@Composable
private fun ProviderListingActionCard(
    icon: ImageVector,
    title: String,
    supporting: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF4F8F6),
        border = BorderStroke(1.dp, Color(0xFFD7E4DE))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(shape = CircleShape, color = Color(0xFFE3F1EB), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(17.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15362A), maxLines = 1)
                Text(supporting, fontSize = 9.sp, color = Color(0xFF6A7B73), maxLines = 2, lineHeight = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProviderListingCard(
    listing: com.estatenestora.app.data.model.ServiceListing,
    serviceTypes: List<ServiceType> = emptyList(),
    onSetListingActive: (suspend (String, Boolean) -> AndroidBridgeResponse?)? = null,
    onImageClick: (com.estatenestora.app.data.model.ServiceListing) -> Unit,
    onAvailabilityClick: ((com.estatenestora.app.data.model.ServiceListing) -> Unit)? = null,
    onPackagesClick: ((com.estatenestora.app.data.model.ServiceListing) -> Unit)? = null,
    onEditClick: ((com.estatenestora.app.data.model.ServiceListing) -> Unit)? = null,
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

    // -- Zomato Structural Layout Card --
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFEBEBEB)),
        onClick = { onImageClick(listing) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // -- 3. Image Slider Section (Top half of the card) --
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

            // -- 4. Restaurant Information Metadata (Middle section) --
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
                    text = "${listing.location.ifBlank { "City Centre 2, Rajarhat" }} - Cover range: ${listing.serviceRadiusKm} km",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // Cuisine / Service Row (without 'for two')
                Text(
                    text = "$categoryDisplayName, $serviceTypeDisplayName - Rs.${listing.price.toInt()}",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFEDEDED), thickness = 1.dp)
                Spacer(Modifier.height(8.dp))

                // -- 1. & 2. Toggle active & real count of created bookings on bottom left --
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
                                        if (active) "Listing activated" else "Listing deactivated",
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

                    // One authoritative lifetime total. Requested bookings are
                    // already a subset of open bookings and must not be added.
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEAF4F0)
                    ) {
                        Text(
                            text = "${listing.totalBookingCount} total bookings",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F5A47),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onAvailabilityClick != null) ProviderListingActionCard(
                        icon = Icons.Default.DateRange,
                        title = "Availability",
                        supporting = "Hours and booking slots",
                        onClick = { onAvailabilityClick(listing) },
                        modifier = Modifier.weight(1f)
                    )
                    if (onEditClick != null) ProviderListingActionCard(
                        icon = Icons.Default.Create,
                        title = "Edit listing",
                        supporting = "Pricing and details",
                        onClick = { onEditClick(listing) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (onPackagesClick != null) {
                    Spacer(Modifier.height(8.dp))
                    ProviderListingActionCard(
                        icon = Icons.Default.Star,
                        title = "Work items & packages",
                        supporting = when {
                            listing.workItemCount == 0 && listing.packageCount == 0 -> "Add customer service options"
                            else -> "${listing.workItemCount} work items · ${listing.packageCount} packages"
                        },
                        onClick = { onPackagesClick(listing) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
