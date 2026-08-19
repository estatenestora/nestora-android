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
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState


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
    onFetchMyListings: (suspend () -> AndroidBridgeResponse?)? = null
) {

    Scaffold(
        bottomBar = {
            if (selectedTab != 1) {
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
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Register",
                                tint = if (selectedTab == 0) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Register",
                                fontSize = 11.sp,
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
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = "Describe",
                                tint = if (selectedTab == 1) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Describe",
                                fontSize = 11.sp,
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
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Fill",
                                tint = if (selectedTab == 2) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Fill",
                                fontSize = 11.sp,
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
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Listings",
                                tint = if (selectedTab == 3) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Listings",
                                fontSize = 11.sp,
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
                    val listState = rememberLazyListState()
                    val isScrolled by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
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
                                onRegisterServiceClick = {},
                                onBookingsClick = onBookingsClick,
                                onFindServiceClick = onFindServiceClick,
                                onExploreClick = onExploreClick,
                                activeMenu = "register",
                                userPhotoPath = userPhotoPath
                            )
                        }

                        stickyHeader {
                            OnboardingSearchBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                isScrolled = isScrolled,
                                hasCarouselBelow = true
                            )
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
                                    color = Color(0xFF004D40), // Primary Deep Teal Brand Green
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
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF004D40)),
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
                                                    .background(Color(0xFFE8F5E9)), // Secondary Soft Mint Green background
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Create,
                                                    contentDescription = "Describe with AI",
                                                    tint = Color(0xFF004D40), // Deep Teal Accent
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
                                                    color = Color(0xFF004D40),
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
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40)),
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
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
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
                                                        tint = Color(0xFF004D40),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Manual Form",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF004D40),
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
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
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
                                                        tint = Color(0xFF004D40),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Find Services",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF004D40),
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
                                .background(if (isScrolled) Color.White else Color(0xFF005E46))
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
                        categories = categories,
                        onBack = { onTabChange(0) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListingsScreen(
    onFetchMyListings: (suspend () -> AndroidBridgeResponse?)?,
    categories: List<Category>,
    onBack: () -> Unit
) {
    var listings by remember { mutableStateOf<List<com.estatenestora.app.data.model.ServiceListing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var searchKey by remember { mutableStateOf("") }
    var selectedCategorySlug by remember { mutableStateOf("All") }
    var selectedServiceTypeSlug by remember { mutableStateOf("All") }
    var sortByPriceAsc by remember { mutableStateOf<Boolean?>(null) }
    var currentPage by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (onFetchMyListings != null) {
            try {
                val resp = onFetchMyListings()
                if (resp != null && resp.ok) {
                    listings = resp.listings.orEmpty().map { it.toServiceListing() }
                } else {
                    errorMessage = resp?.reply ?: "Failed to load listings"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "An error occurred while loading"
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(searchKey, selectedCategorySlug, selectedServiceTypeSlug, sortByPriceAsc) {
        currentPage = 0
    }

    val availableServiceTypes: List<String> = remember(selectedCategorySlug, listings) {
        if (selectedCategorySlug == "All") {
            listings.map { it.serviceType }.distinct()
        } else {
            listings.filter { it.categoryName.lowercase().contains(selectedCategorySlug.lowercase()) }
                .map { it.serviceType }.distinct()
        }
    }

    val filteredListings = remember(listings, searchKey, selectedCategorySlug, selectedServiceTypeSlug, sortByPriceAsc) {
        var result = listings.filter { listing ->
            val matchesSearch = searchKey.isBlank() || 
                listing.title.contains(searchKey, ignoreCase = true) ||
                listing.location.contains(searchKey, ignoreCase = true) ||
                listing.serviceType.contains(searchKey, ignoreCase = true)
                
            val matchesCategory = selectedCategorySlug == "All" ||
                listing.categoryName.lowercase().contains(selectedCategorySlug.lowercase())

            val matchesServiceType = selectedServiceTypeSlug == "All" ||
                listing.serviceType.equals(selectedServiceTypeSlug, ignoreCase = true)

            matchesSearch && matchesCategory && matchesServiceType
        }

        sortByPriceAsc?.let { asc ->
            result = if (asc) {
                result.sortedBy { it.price }
            } else {
                result.sortedByDescending { it.price }
            }
        }

        result
    }

    val pageSize = 5
    val paginatedListings = filteredListings.take((currentPage + 1) * pageSize)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "My Listings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F2E23)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, color = Color(0xFF005E46), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF7F9FB))
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NestoraMint)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage!!, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { isLoading = true; errorMessage = null }, colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                val scrollState = rememberLazyListState()

                val lastVisibleItemIndex = remember {
                    derivedStateOf {
                        val layoutInfo = scrollState.layoutInfo
                        val visibleItemsInfo = layoutInfo.visibleItemsInfo
                        if (visibleItemsInfo.isEmpty()) 0
                        else visibleItemsInfo.last().index
                    }
                }

                LaunchedEffect(lastVisibleItemIndex.value) {
                    val totalItems = scrollState.layoutInfo.totalItemsCount
                    if (totalItems > 0 && lastVisibleItemIndex.value >= totalItems - 2) {
                        if ((currentPage + 1) * pageSize < filteredListings.size) {
                            currentPage++
                        }
                    }
                }

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            OutlinedTextField(
                                value = searchKey,
                                onValueChange = { searchKey = it },
                                placeholder = { Text("Search by title, location...", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NestoraMint,
                                    unfocusedBorderColor = Color(0xFFE2EAF2),
                                    focusedContainerColor = Color(0xFFF9F9F9),
                                    unfocusedContainerColor = Color(0xFFF9F9F9)
                                )
                            )

                            Spacer(Modifier.height(12.dp))

                            Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val catFilterOptions = listOf("All") + categories.map { it.name }
                                catFilterOptions.distinct().forEach { catOption ->
                                    val isSelected = selectedCategorySlug.equals(catOption, ignoreCase = true) || 
                                        (catOption == "All" && selectedCategorySlug == "All")
                                    Surface(
                                        modifier = Modifier.clickable {
                                            selectedCategorySlug = if (catOption == "All") "All" else catOption
                                            selectedServiceTypeSlug = "All"
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFFE8FAF4) else Color(0xFFF1F3F5),
                                        border = if (isSelected) BorderStroke(1.dp, NestoraMint) else null
                                    ) {
                                        Text(
                                            text = catOption,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF005E46) else Color(0xFF555555),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text("Service Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val serviceTypeOptions = listOf("All") + availableServiceTypes
                                serviceTypeOptions.forEach { typeOption ->
                                    val isSelected = selectedServiceTypeSlug == typeOption
                                    Surface(
                                        modifier = Modifier.clickable {
                                            selectedServiceTypeSlug = typeOption
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFFE8FAF4) else Color(0xFFF1F3F5),
                                        border = if (isSelected) BorderStroke(1.dp, NestoraMint) else null
                                    ) {
                                        Text(
                                            text = typeOption.replace("_", " ").replaceFirstChar { it.uppercase() },
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF005E46) else Color(0xFF555555),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sort Price:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                
                                val sortOptions = listOf(
                                    "Default" to null,
                                    "Low to High" to true,
                                    "High to Low" to false
                                )
                                sortOptions.forEach { (label, value) ->
                                    val isSelected = sortByPriceAsc == value
                                    Surface(
                                        modifier = Modifier.clickable { sortByPriceAsc = value },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) NestoraMint else Color.Transparent,
                                        border = BorderStroke(1.dp, if (isSelected) NestoraMint else Color(0xFFE2EAF2))
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF555555),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Showing ${filteredListings.size} Listing(s)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    if (filteredListings.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔍", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No listings found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F2E23)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Try adjusting your filters or search text.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(paginatedListings, key = { it.id }) { listing ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                ProviderListingCard(listing = listing)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderListingCard(listing: com.estatenestora.app.data.model.ServiceListing) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFECEFF1))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE0F2F1)
                    ) {
                        Text(
                            text = listing.categoryName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00796B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFECEFF1)
                    ) {
                        Text(
                            text = listing.serviceType.replace("_", " ").replaceFirstChar { it.uppercase() },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF555555),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                if (listing.isVerified) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFF00B0FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = listing.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1C)
            )

            if (listing.location.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = listing.location,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F3F4))
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BASE PRICE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "₹${listing.price.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D1A13)
                    )
                }

                if (listing.rating > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format("%.1f", listing.rating),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1C)
                        )
                    }
                }
            }
        }
    }
}
