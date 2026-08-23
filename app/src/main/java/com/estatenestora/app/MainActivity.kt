package com.estatenestora.app

import android.os.Bundle
import android.widget.Toast
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocationOn
import com.estatenestora.app.ui.screens.CategoriesScreen
import com.estatenestora.app.ui.screens.ServicesScreen
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.estatenestora.app.data.model.AndroidBridgeListing
import com.estatenestora.app.data.model.Category
import com.estatenestora.app.data.model.ServiceListing
import com.estatenestora.app.data.model.TelegramChatMessage
import com.estatenestora.app.data.model.UserProfile
import com.estatenestora.app.data.repository.BookingPollingController
import com.estatenestora.app.data.repository.NestoraRepository
import com.estatenestora.app.data.telegram.TdLibManager
import com.estatenestora.app.ui.auth.TelegramAuthScreen
import com.estatenestora.app.ui.screens.AIChatScreen
import com.estatenestora.app.ui.screens.BookingCreateSheet
import com.estatenestora.app.ui.screens.BookingLoaderScreen
import com.estatenestora.app.ui.screens.BookingDetailScreen
import com.estatenestora.app.ui.screens.BookingsScreen
import com.estatenestora.app.ui.screens.HomeScreen
import com.estatenestora.app.ui.screens.ProfileScreen
import com.estatenestora.app.ui.screens.LocationAccessScreen
import com.estatenestora.app.ui.screens.GuestProfileScreen
import com.estatenestora.app.ui.screens.NotificationAccessScreen
import com.estatenestora.app.ui.screens.MapLocationPickerScreen
import com.estatenestora.app.ui.screens.RegisterServiceScreen
import com.estatenestora.app.ui.screens.RegisterChoiceScreen
import com.estatenestora.app.ui.screens.AutoRegisterScreen
import com.estatenestora.app.ui.screens.AdminPaymentsScreen
import com.estatenestora.app.ui.screens.FinderChoiceScreen
import com.estatenestora.app.ui.theme.*
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.estatenestora.app.ui.screens.getRealLifeImageUrl
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import kotlin.math.abs
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.scale
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.gestures.detectDragGestures

class MainActivity : ComponentActivity() {

    private val repository = NestoraRepository()
    private val bookingPolling by lazy { BookingPollingController(repository, lifecycleScope) }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        TdLibManager.init(applicationContext)

        setContent {
            // The original mint palette remains the default. Dynamic colour
            // rotation is explicitly enabled at build time with
            // NESTORA_DYNAMIC_THEME=true.
            val appRoyalTheme = remember {
                if (BuildConfig.DYNAMIC_THEME_ENABLED) {
                    RoyalThemeRepository.themes.random()
                } else {
                    RoyalThemeRepository.legacyMintTheme
                }
            }
            NestoraTheme(darkTheme = false, royalTheme = appRoyalTheme) {
                val context = LocalContext.current
                val prefs = remember(context) { context.getSharedPreferences("nestora_prefs", Context.MODE_PRIVATE) }
                
                var guestMode by remember { mutableStateOf(prefs.getBoolean("guest_mode", false)) }
                var mapPickerSource by remember { mutableStateOf("") }
                var pendingMapLocationToSend by remember { mutableStateOf<String?>(null) }
                // Booking-creation address override, set only when the user picks
                // a different location via the map while the create sheet is open.
                var pendingBookingAddress by remember { mutableStateOf<String?>(null) }
                var pendingBookingLat by remember { mutableStateOf<Double?>(null) }
                var pendingBookingLon by remember { mutableStateOf<Double?>(null) }
                var loaderListingTitle by remember { mutableStateOf("") }
                var loaderAddressText by remember { mutableStateOf("") }
                var selectedRegisterTab by remember { mutableStateOf(0) }
                var selectedFinderTab by remember { mutableStateOf(0) }
                val scope = rememberCoroutineScope()

                val hasLocationPerm = remember {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                }
                
                var locationGranted by remember { 
                    mutableStateOf(hasLocationPerm || prefs.getBoolean("location_granted", false)) 
                }
                
                var notificationsGranted by remember { 
                    mutableStateOf(prefs.getBoolean("notifications_granted", false)) 
                }
                
                var userLocation by remember { 
                    mutableStateOf(prefs.getString("user_location", "Salt Lake, Sector V")) 
                }

                val authState by TdLibManager.authState.collectAsState()

                // Automatically fetch and geocode GPS location on app start / permission grant
                LaunchedEffect(locationGranted) {
                    if (locationGranted && com.estatenestora.app.util.isSystemLocationEnabled(context)) {
                        try {
                            val loc = com.estatenestora.app.util.getCurrentLocation(context)
                            if (loc != null) {
                                val place = repository.reverseGeocode(loc.latitude, loc.longitude)
                                if (place != null) {
                                    val formatted = if (place.subtitle.isNotBlank()) "${place.title}, ${place.subtitle}" else place.title
                                    userLocation = formatted
                                    prefs.edit().putString("user_location", userLocation).apply()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Failed to auto-detect current location", e)
                        }
                    }
                }

                // 1. Auth Gate (Figma Page 3 & 4)
                if (authState !is TdLibManager.AuthState.Ready && !guestMode) {
                    TelegramAuthScreen(
                        authState = authState,
                        onSkip = { 
                            prefs.edit().putBoolean("guest_mode", true).apply()
                            guestMode = true 
                        }
                    )
                    return@NestoraTheme
                }

                // 2. Location Onboarding Gate (Figma Page 8 & 9)
                if (!locationGranted) {
                    LocationAccessScreen(
                        onLocationGranted = {
                            prefs.edit().putBoolean("location_granted", true).apply()
                            locationGranted = true
                        },
                        onManualLocation = { city ->
                            prefs.edit().putBoolean("location_granted", true).apply()
                            userLocation = city
                            prefs.edit().putString("user_location", userLocation).apply()
                            locationGranted = true
                        },
                        onBack = {
                            this@MainActivity.finish()
                        }
                    )
                    return@NestoraTheme
                }

                // 3. Notification Onboarding Gate (Figma Page 10 & 11)
                if (!notificationsGranted) {
                    NotificationAccessScreen(
                        onNotificationsEnabled = {
                            prefs.edit().putBoolean("notifications_granted", true).apply()
                            notificationsGranted = true
                        },
                        onNotNow = {
                            prefs.edit().putBoolean("notifications_granted", true).apply()
                            notificationsGranted = true
                        },
                        onBack = {
                            locationGranted = false
                        }
                    )
                    return@NestoraTheme
                }

                var activeScreen by remember { mutableStateOf("main") }
                var selectedTab by remember { mutableStateOf(0) } // Default to Explore tab
                var isProviderMode by remember { mutableStateOf(prefs.getBoolean("provider_mode", false)) }
                val onModeToggle = {
                    isProviderMode = !isProviderMode
                    prefs.edit().putBoolean("provider_mode", isProviderMode).apply()
                    if (isProviderMode) {
                        // Switched to Provider: Finder (tab 1) is not allowed
                        if (selectedTab == 1) {
                            selectedTab = 0 // default to Explore
                        }
                    } else {
                        // Switched to Customer: Register is not allowed
                        if (activeScreen == "register_choice" || activeScreen == "register_service" || activeScreen == "auto_register") {
                            activeScreen = "main"
                            selectedTab = 0 // default to Explore
                        }
                    }
                }
                val fullTabsList = remember {
                    listOf(
                        com.estatenestora.app.ui.theme.NestoraTab("explore", "Explore", "🧭", visibleInHireMode = true, visibleInServeMode = true),
                        com.estatenestora.app.ui.theme.NestoraTab("finder", "Finder", "🔍", visibleInHireMode = true, visibleInServeMode = false),
                        com.estatenestora.app.ui.theme.NestoraTab("register", "Register", "🧰", visibleInHireMode = false, visibleInServeMode = true),
                        com.estatenestora.app.ui.theme.NestoraTab("bookings", "Bookings", "🧾", visibleInHireMode = true, visibleInServeMode = true)
                    )
                }
                val activeTabsList = remember(isProviderMode) {
                    fullTabsList.filter { 
                        if (isProviderMode) it.visibleInServeMode else it.visibleInHireMode 
                    }
                }
                val todayTheme = appRoyalTheme
                val selectedTabId = remember(activeScreen, selectedTab) {
                    if (activeScreen == "register_choice" || activeScreen == "register_service" || activeScreen == "auto_register") {
                        "register"
                    } else {
                        when (selectedTab) {
                            0 -> "explore"
                            1 -> "finder"
                            2 -> "bookings"
                            else -> "explore"
                        }
                    }
                }
                val onTabSelected = { tabId: String ->
                    when (tabId) {
                        "explore" -> {
                            activeScreen = "main"
                            selectedTab = 0
                        }
                        "finder" -> {
                            activeScreen = "main"
                            selectedTab = 1
                            selectedFinderTab = 0
                        }
                        "register" -> {
                            selectedRegisterTab = 0
                            activeScreen = "register_choice"
                        }
                        "bookings" -> {
                            activeScreen = "main"
                            selectedTab = 2
                        }
                    }
                }
                var isScrolled by remember { mutableStateOf(false) }
                var dismissedBookingIds by remember { mutableStateOf(setOf<String>()) }
                var showAllBookingsExpanded by remember { mutableStateOf(false) }
                var isBubbleDismissedByUser by remember { mutableStateOf(false) }
                var bubbleDragX by remember { mutableStateOf(0f) }
                var bubbleDragY by remember { mutableStateOf(0f) }
                var isDraggingBubble by remember { mutableStateOf(false) }
                var totalDragDistanceThisSession by remember { mutableStateOf(0f) }

                LaunchedEffect(selectedTab) {
                    isScrolled = false
                    if (selectedTab == 0) {
                        dismissedBookingIds = emptySet()
                        isBubbleDismissedByUser = false
                    }
                    showAllBookingsExpanded = false
                    bubbleDragX = 0f
                    bubbleDragY = 0f
                    isDraggingBubble = false
                    totalDragDistanceThisSession = 0f
                }

                // Booking list polling now starts once on auth-ready (see the
                // authState LaunchedEffect below) and stays on the whole
                // session so the sticky running-booking banner has live data
                // everywhere, not just while the Bookings tab is open — this
                // effect no longer needs to start/stop it per tab.

                LaunchedEffect(isScrolled) {
                    enableEdgeToEdge(
                        statusBarStyle = androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    )
                }
                fun getCurrentFormattedTime(): String {
                    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                    return sdf.format(java.util.Date())
                }
                var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
                var feedListings by remember { mutableStateOf<List<ServiceListing>>(emptyList()) }
                var isLoadingFeed by remember { mutableStateOf(false) }
                val bookings by bookingPolling.bookings.collectAsState()
                val bookingDetail by bookingPolling.detail.collectAsState()
                var shownAnimationBookingIds by remember { mutableStateOf(setOf<String>()) }

                LaunchedEffect(bookingDetail) {
                    val detail = bookingDetail
                    if (detail != null && detail.status == "CONFIRMED" && !shownAnimationBookingIds.contains(detail.id)) {
                        shownAnimationBookingIds = shownAnimationBookingIds + detail.id
                        loaderListingTitle = detail.listingTitle
                        loaderAddressText = if (detail.customerAddress.isNotBlank()) detail.customerAddress else "Center Appointment"
                        activeScreen = "booking_loader"
                    }
                }

                var selectedBookingId by remember { mutableStateOf<String?>(null) }
                
                // Deep link routing check
                val currentIntent = (context as? MainActivity)?.intent
                val deepLinkBookingId = currentIntent?.getStringExtra("deep_link_booking_id")
                
                LaunchedEffect(deepLinkBookingId, authState) {
                    if (authState is TdLibManager.AuthState.Ready && !deepLinkBookingId.isNullOrEmpty()) {
                        Log.i("MainActivity", "Routing deep link for booking ID: $deepLinkBookingId")
                        selectedBookingId = deepLinkBookingId
                        bookingPolling.openDetail(deepLinkBookingId)
                        activeScreen = "booking_detail"
                        // Clear the extra so we don't repeatedly route to it if MainActivity is recreated
                        currentIntent.removeExtra("deep_link_booking_id")
                    }
                }

                var bookingSheetListing by remember { mutableStateOf<ServiceListing?>(null) }

                var currentLat by remember { mutableStateOf(0.0) }
                var currentLon by remember { mutableStateOf(0.0) }

                LaunchedEffect(locationGranted, bookingSheetListing) {
                    if (locationGranted && bookingSheetListing != null) {
                        try {
                            val loc = com.estatenestora.app.util.getCurrentLocation(context)
                            if (loc != null) {
                                currentLat = loc.latitude
                                currentLon = loc.longitude
                            }
                        } catch (e: Exception) {}
                    }
                }

                BackHandler(enabled = activeScreen != "main" || selectedTab != 0) {
                    if (activeScreen == "booking_detail") {
                        bookingPolling.closeDetail()
                        selectedBookingId = null
                        activeScreen = "main"
                        selectedTab = 2
                    } else if (activeScreen == "map_picker") {
                        if (mapPickerSource == "register_choice") {
                            activeScreen = "register_choice"
                        } else if (mapPickerSource == "booking_details") {
                            activeScreen = "booking_detail"
                        } else {
                            activeScreen = "main"
                        }
                    } else if (activeScreen == "register_choice" || activeScreen == "register_service" || activeScreen == "auto_register") {
                        activeScreen = "main"
                        selectedTab = 3
                    } else if (activeScreen == "booking_loader") {
                        activeScreen = "main"
                    } else if (activeScreen == "main" && selectedTab != 0) {
                        selectedTab = 0
                    } else {
                        activeScreen = "main"
                    }
                }

                fun dialListingPhone(listing: ServiceListing) {
                    val phone = listing.phone?.ifBlank { null } ?: "+917076783428"
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = android.net.Uri.parse("tel:$phone")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open dialer for $phone", Toast.LENGTH_SHORT).show()
                    }
                }
                var profile by remember { mutableStateOf<UserProfile?>(null) }
                var userPhotoPath by remember { mutableStateOf<String?>(null) }

                // Adaptive polling follows app foreground/background — see
                // BookingPollingController's doc comment for why (avoids
                // hammering the Telegram bridge while the app isn't visible).
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> bookingPolling.onAppForeground()
                            Lifecycle.Event.ON_STOP -> bookingPolling.onAppBackground()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }


                val chatMessages = remember {
                    mutableStateListOf<com.estatenestora.app.data.model.TelegramChatMessage>(
                        /*
                        TelegramChatMessage(
                            id = "welcome-1",
                            sender = "Nestora Bot",
                            text = "Hello! I'm Nestora AI. I can help you find flats, book home services (plumbers, electricians, AC repair), hire domestic help (maids, cooks), or get support. What are you looking for?",
                            timestamp = getCurrentFormattedTime(),
                            isUser = false
                        )
                        */
                    )
                }

                val autoRegisterMessages = remember {
                    mutableStateListOf(
                        TelegramChatMessage(
                            id = "welcome",
                            sender = "Nestora AI",
                            text = "Select the service you want to register from the list below.",
                            timestamp = getCurrentFormattedTime(),
                            isUser = false
                        )
                    )
                }


                LaunchedEffect(authState) {
                    if (authState is TdLibManager.AuthState.Ready) {
                        if (guestMode) {
                            guestMode = false
                            prefs.edit().putBoolean("guest_mode", false).apply()
                        }
                        profile = repository.getUserProfile()

                        // Start list polling as soon as we're logged in, not only
                        // when the Bookings tab is opened — the sticky running-
                        // booking banner needs live data everywhere else too.
                        bookingPolling.startListPolling()

                        // Fetch FCM token and register it
                        try {
                            // Register cached token first if available
                            val cachedToken = prefs.getString("fcm_token", null)
                            if (!cachedToken.isNullOrBlank()) {
                                scope.launch {
                                    try {
                                        repository.registerFcmToken(cachedToken)
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Failed to register cached FCM token with backend", e)
                                    }
                                }
                            }

                            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val token = task.result
                                        if (!token.isNullOrBlank()) {
                                            Log.i("MainActivity", "FCM token retrieved: $token")
                                            prefs.edit().putString("fcm_token", token).apply()
                                            scope.launch {
                                                try {
                                                    repository.registerFcmToken(token)
                                                } catch (e: Exception) {
                                                    Log.e("MainActivity", "Failed to register FCM token with backend", e)
                                                }
                                            }
                                        }
                                    } else {
                                        val exception = task.exception
                                        Log.w("MainActivity", "FCM token fetch failed: ${exception?.message}")
                                        // On registration overflow or stale instance ID, attempt token cleanup
                                        if (exception?.message?.contains("TOO_MANY_REGISTRATIONS", ignoreCase = true) == true) {
                                            try {
                                                com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken()
                                            } catch (delEx: Exception) {
                                                Log.w("MainActivity", "Failed to delete stale FCM token", delEx)
                                            }
                                        }
                                    }
                                }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Failed to initialize Firebase Messaging or get token", e)
                        }

                        // The very first fetch can fail if the backend isn't up yet
                        // when the app launches (e.g. dev server started after the
                        // app) — retry with a short delay a few times instead of
                        // permanently caching an empty list for the rest of the
                        // session (this LaunchedEffect only re-runs when authState
                        // itself changes, so without a retry here, a failed first
                        // attempt would never be corrected).
                        var attempts = 0
                        while (categories.isEmpty() && attempts < 5) {
                            categories = repository.getCategories()
                            if (categories.isEmpty()) {
                                attempts++
                                kotlinx.coroutines.delay(3000)
                            }
                        }

                        // Load initial feed of service providers for HIRE mode
                        scope.launch {
                            try {
                                isLoadingFeed = true
                                val feedResp = repository.getFeedListings(
                                    addressBarLatitude = if (currentLat != 0.0) currentLat else null,
                                    addressBarLongitude = if (currentLon != 0.0) currentLon else null
                                )
                                feedListings = feedResp?.listings?.map { it.toServiceListing() } ?: emptyList()
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Failed to load feed listings", e)
                            } finally {
                                isLoadingFeed = false
                            }
                        }
                    } else {
                        // Clear user data immediately when not logged in to make guest mode consistent
                        profile = null
                        bookingPolling.clear()
                        userPhotoPath = null
                    }
                }

                // Resolve profile photo file_id → local cached file whenever profile changes
                LaunchedEffect(profile?.profilePicUrl) {
                    val picUrl = profile?.profilePicUrl
                    Log.w("MainActivity", "[PhotoResolution] LaunchedEffect triggered for profilePicUrl: $picUrl")
                    if (!picUrl.isNullOrBlank() && picUrl.length >= 20) {
                        val path = repository.getLocalPhotoPath(picUrl, context)
                        Log.w("MainActivity", "[PhotoResolution] Resolved local path: $path")
                        userPhotoPath = path
                    } else {
                        Log.w("MainActivity", "[PhotoResolution] profilePicUrl is blank or invalid, setting userPhotoPath = null")
                        userPhotoPath = null
                    }
                }

                // Lazy retry: covers the backend coming up well after the loop
                // above already gave up — try again whenever the user visits
                // Explore and still has nothing to show.
                LaunchedEffect(selectedTab) {
                    if (selectedTab == 0 && authState is TdLibManager.AuthState.Ready) {
                        if (categories.isEmpty()) {
                            categories = repository.getCategories()
                        }
                        if (feedListings.isEmpty() && !isLoadingFeed) {
                            try {
                                isLoadingFeed = true
                                val feedResp = repository.getFeedListings(
                                    addressBarLatitude = if (currentLat != 0.0) currentLat else null,
                                    addressBarLongitude = if (currentLon != 0.0) currentLon else null
                                )
                                feedListings = feedResp?.listings?.map { it.toServiceListing() } ?: emptyList()
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Failed to refresh feed listings", e)
                            } finally {
                                isLoadingFeed = false
                            }
                        }
                    }
                }

                // Shared by typed free-text search (AIChatScreen.onSendMessage)
                // and category/service-type tile taps (HomeScreen.onCategorySelected)
                // — both append a user bubble, show a typing indicator, run the
                // given bridge call, then render the result (or an error) as a
                // bot bubble with any listings attached, through one code path.
                fun runChatQuery(displayText: String, fetch: suspend () -> com.estatenestora.app.data.model.AndroidBridgeResponse?) {
                    chatMessages.add(
                        TelegramChatMessage(
                            id = "usr-${System.currentTimeMillis()}",
                            sender = "You",
                            text = displayText,
                            timestamp = getCurrentFormattedTime(),
                            isUser = true
                        )
                    )
                    val typingId = "typing-${System.currentTimeMillis()}"
                    chatMessages.add(
                        TelegramChatMessage(
                            id = typingId,
                            sender = "Nestora Bot",
                            text = "⌛ Searching Nestora AI Engine...",
                            timestamp = getCurrentFormattedTime(),
                            isUser = false
                        )
                    )
                    lifecycleScope.launch {
                        val response = fetch()
                        val typingIndex = chatMessages.indexOfFirst { msg: com.estatenestora.app.data.model.TelegramChatMessage -> msg.id == typingId }
                        if (typingIndex >= 0) chatMessages.removeAt(typingIndex)

                        if (response == null) {
                            chatMessages.add(
                                TelegramChatMessage(
                                    id = "err-${System.currentTimeMillis()}",
                                    sender = "Nestora Bot",
                                    text = "❌ Could not get response from Nestora.",
                                    timestamp = getCurrentFormattedTime(),
                                    isUser = false
                                )
                            )
                            return@launch
                        }

                        val listings = response.listings.orEmpty().map { card -> card.toServiceListing() }
                        chatMessages.add(
                            TelegramChatMessage(
                                id = "bot-${System.currentTimeMillis()}",
                                sender = "Nestora Bot",
                                text = response.reply,
                                timestamp = getCurrentFormattedTime(),
                                isUser = false,
                                attachedListings = listings
                            )
                        )
                    }
                }

                when (activeScreen) {
                    "booking_loader" -> {
                        BookingLoaderScreen(
                            listingTitle = loaderListingTitle,
                            addressText = loaderAddressText,
                            onAnimationComplete = {
                                activeScreen = "booking_detail"
                            }
                        )
                    }
                    "map_picker" -> {
                        MapLocationPickerScreen(
                            initialAddress = userLocation,
                            onSearchAddress = { q, lat, lon -> repository.searchAddress(q, lat, lon) },
                            onReverseGeocode = { lat, lon -> repository.reverseGeocode(lat, lon) },
                            onLocationConfirmed = { title, sub, lat, lon ->
                                val loc = "$title, $sub"
                                prefs.edit().putString("user_location", loc).apply()
                                userLocation = loc
                                if (mapPickerSource == "register_choice") {
                                     pendingMapLocationToSend = if (lat != null && lon != null) {
                                         "$loc||$lat,$lon"
                                     } else {
                                         loc
                                     }
                                     activeScreen = "register_choice"
                                 } else if (mapPickerSource == "booking_create") {
                                     pendingBookingAddress = loc
                                     pendingBookingLat = lat
                                     pendingBookingLon = lon
                                     activeScreen = "main"
                                 } else {
                                     activeScreen = "main"
                                 }
                             },
                             onBack = {
                                 if (mapPickerSource == "register_choice") {
                                     activeScreen = "register_choice"
                                 } else {
                                     activeScreen = "main"
                                 }
                             }
                        )
                    }
                    "register_choice" -> {
                        RegisterChoiceScreen(
                            categories = categories,
                            selectedTab = selectedRegisterTab,
                            onTabChange = { selectedRegisterTab = it },
                            onFetchServiceTypes = { categorySlug -> repository.getServiceTypes(categorySlug) },
                            onFetchAllServiceTypes = { repository.getAllServiceTypes() },
                            onFetchServiceAttributes = { serviceTypeSlug -> repository.getServiceAttributes(serviceTypeSlug) },
                            onSubmit = { categorySlug, serviceTypeSlug, basePrice, location, city, description, collectedAttributes ->
                                repository.registerService(
                                    NestoraRepository.RegisterServiceRequest(
                                        categorySlug = categorySlug,
                                        serviceTypeSlug = serviceTypeSlug,
                                        basePrice = basePrice,
                                        locationDisplayName = location,
                                        city = city,
                                        description = description,
                                        collectedAttributes = collectedAttributes
                                    )
                                )
                            },
                            onParse = { text -> repository.aisoParse(text) },
                            onSave = { repository.aisoSave() },
                            onUpdate = { payload -> repository.aisoUpdate(payload) },
                            onReset = { repository.aisoReset() },
                            onBack = { activeScreen = "main" },
                            currentLocation = userLocation,
                            onSelectLocationClick = { mapPickerSource = "register_choice"; activeScreen = "map_picker" },
                            onProfileClick = { selectedTab = 3; activeScreen = "main" },
                            onExploreClick = { selectedTab = 0; activeScreen = "main" },
                            onFindServiceClick = { selectedTab = 1; selectedFinderTab = 0; activeScreen = "main" },
                            onBookingsClick = { selectedTab = 2; activeScreen = "main" },
                            onReverseGeocode = { lat, lon -> repository.reverseGeocode(lat, lon) },
                            onSearchAddress = { q, lat, lon -> repository.searchAddress(q, lat, lon) },
                            autoRegisterMessages = autoRegisterMessages,
                            pendingMapLocationToSend = pendingMapLocationToSend,
                            onClearPendingMapLocation = { pendingMapLocationToSend = null },
                            userPhotoPath = userPhotoPath,
                            profileName = profile?.name,
                            onFetchMyListings = { repository.getMyListings() },
                            isProviderMode = isProviderMode,
                            onModeToggle = onModeToggle,
                            tabsList = activeTabsList,
                            selectedTabId = selectedTabId,
                            onTabSelected = onTabSelected,
                            currentTheme = todayTheme,
                            onClearAutoRegisterChat = {
                                 scope.launch {
                                     repository.aisoReset()
                                     autoRegisterMessages.clear()
                                     autoRegisterMessages.add(
                                         TelegramChatMessage(
                                             id = "welcome",
                                             sender = "Nestora AI",
                                             text = "Select the service you want to register from the list below.",
                                             timestamp = getCurrentFormattedTime(),
                                             isUser = false
                                         )
                                     )
                                 }
                             }
                        )
                    }
                    "booking_detail" -> {
                        BookingDetailScreen(
                            detail = bookingDetail,
                            viewerUserId = profile?.id,
                            onBack = {
                                bookingPolling.closeDetail()
                                selectedBookingId = null
                                activeScreen = "main"
                            },
                            onAccept = {
                                scope.launch {
                                    selectedBookingId?.let { id ->
                                        val resp = repository.acceptBooking(id)
                                        if (resp?.ok == true) {
                                            bookingPolling.openDetail(id, clearCache = false)
                                        } else if (!resp?.reply.isNullOrBlank()) {
                                            Toast.makeText(context, resp?.reply, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            onReject = {
                                scope.launch {
                                    selectedBookingId?.let { id ->
                                        if (repository.rejectBooking(id)) {
                                            bookingPolling.openDetail(id, clearCache = false)
                                        }
                                    }
                                }
                            },
                            onStartTravel = { lat, lon ->
                                scope.launch {
                                    selectedBookingId?.let { id ->
                                        if (repository.startTravel(id, lat, lon)) {
                                            bookingPolling.openDetail(id, clearCache = false)
                                        }
                                    }
                                }
                            },
                            onMarkArrived = {
                                scope.launch {
                                    selectedBookingId?.let { id ->
                                        if (repository.markArrived(id)) {
                                            bookingPolling.openDetail(id, clearCache = false)
                                        }
                                    }
                                }
                            },
                            onVerifyOtp = { otp, onResult ->
                                scope.launch {
                                    selectedBookingId?.let { id ->
                                        val resp = repository.verifyOtp(id, otp)
                                        if (resp?.ok == true) {
                                            onResult(null)
                                            bookingPolling.openDetail(id, clearCache = false)
                                        } else {
                                            onResult(resp?.reply ?: "Incorrect OTP. Please try again.")
                                        }
                                    }
                                }
                            },
                            onStartService = {
                                scope.launch {
                                    selectedBookingId?.let { id ->
                                        if (repository.startService(id)) {
                                            bookingPolling.openDetail(id, clearCache = false)
                                        }
                                    }
                                }
                            },
                            onCompleteService = {
                                scope.launch {
                                    selectedBookingId?.let { id ->
                                        if (repository.completeService(id)) {
                                            bookingPolling.openDetail(id, clearCache = false)
                                        }
                                    }
                                }
                            },
                            onSubmitReview = { stars, comment ->
                                val id = selectedBookingId
                                if (id != null) {
                                    val ok = repository.submitReview(id, stars, comment)
                                    if (ok) {
                                        bookingPolling.openDetail(id, clearCache = false)
                                    }
                                    ok
                                } else {
                                    false
                                }
                            },
                            onGetCancellationPreview = { id ->
                                repository.getCancellationPreview(id)?.cancelPreview
                            },
                            onCancelBooking = { id ->
                                val resp = repository.cancelBooking(id)
                                if (resp?.ok == true) {
                                    bookingPolling.openDetail(id, clearCache = false)
                                }
                                resp
                            },
                            onGetPaymentInfo = { id ->
                                repository.getPaymentInfo(id)?.paymentInfo
                            },
                            onConfirmPayment = { id ->
                                val resp = repository.confirmPayment(id)
                                if (resp?.ok == true) {
                                    bookingPolling.openDetail(id, clearCache = false)
                                }
                                resp
                            },
                            onPushLiveLocation = { id, lat, lon ->
                                val success = repository.updateLiveLocation(id, lat, lon)
                                if (success) {
                                    bookingPolling.openDetail(id, clearCache = false)
                                }
                                success
                            },
                            onPushCustomerLiveLocation = { id, lat, lon ->
                                val success = repository.updateCustomerLiveLocation(id, lat, lon)
                                if (success) {
                                    bookingPolling.openDetail(id, clearCache = false)
                                }
                                success
                            },
                            onRepairCustomerLocation = { id, lat, lon, address ->
                                val success = repository.setInitialBookingLocation(id, lat, lon, address)
                                if (success) {
                                    bookingPolling.openDetail(id, clearCache = false)
                                }
                                success
                            }
                        )
                    }
                    "admin_payments" -> {
                        AdminPaymentsScreen(
                            loadQueue = { repository.getAdminPaymentQueue() },
                            approve = { id -> repository.approveAdminAdvance(id)?.reply },
                            reject = { id -> repository.rejectAdminAdvance(id)?.reply },
                            onBack = { activeScreen = "main"; selectedTab = 3 }
                        )
                    }
                    else -> {
                        Scaffold(
                            bottomBar = {
                                if (selectedTab != 1 && selectedTab != 2 && selectedTab != 3) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color.White,
                                        shadowElevation = 8.dp,
                                        border = BorderStroke(0.8.dp, Color(0xFFE2EAF2))
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
                                                CustomNavItem(0, "Explore", selectedTab == 0) { selectedTab = 0 }
                                                CustomNavItem(4, "Categories", selectedTab == 4) { selectedTab = 4 }
                                                CustomNavItem(5, "Services", selectedTab == 5) { selectedTab = 5 }
                                            }
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                             Box(
                                 modifier = Modifier.padding(
                                     bottom = innerPadding.calculateBottomPadding()
                                 )
                             ) {
                                when (selectedTab) {
                                    0 -> HomeScreen(
                                        categories = categories,
                                        listings = feedListings,
                                        isLoadingFeed = isLoadingFeed,
                                        onRefreshFeed = {
                                            scope.launch {
                                                try {
                                                    isLoadingFeed = true
                                                    val feedResp = repository.getFeedListings(
                                                        addressBarLatitude = if (currentLat != 0.0) currentLat else null,
                                                        addressBarLongitude = if (currentLon != 0.0) currentLon else null
                                                    )
                                                    feedListings = feedResp?.listings?.map { it.toServiceListing() } ?: emptyList()
                                                } catch (e: Exception) {
                                                    Log.e("MainActivity", "Failed to refresh feed listings", e)
                                                } finally {
                                                    isLoadingFeed = false
                                                }
                                            }
                                        },
                                        onListingClick = { listing -> bookingSheetListing = listing },
                                        onSearchClick = { selectedTab = 1; selectedFinderTab = 1 },
                                        onCategorySelected = { cat ->
                                            selectedTab = 1
                                            selectedFinderTab = 1
                                            runChatQuery("${cat.emoji} ${cat.name}") { repository.searchByCategory(cat.id) }
                                        },
                                        onSeeAllCategoriesClick = { selectedTab = 4 },
                                        currentLocation = userLocation,
                                        onSelectLocationClick = { activeScreen = "map_picker" },
                                        onProfileClick = { selectedTab = 3 },
                                        onRegisterServiceClick = { selectedRegisterTab = 0; activeScreen = "register_choice" },
                                        onBookingsClick = { selectedTab = 2 },
                                        onExploreClick = { selectedTab = 0 },
                                        onScrollChanged = { isScrolled = it },
                                        userPhotoPath = userPhotoPath,
                                        onBookViaTelegram = { listing -> bookingSheetListing = listing },
                                         isProviderMode = isProviderMode,
                                         onModeToggle = onModeToggle,
                                         tabsList = activeTabsList,
                                         selectedTabId = selectedTabId,
                                         onTabSelected = onTabSelected,
                                         currentTheme = todayTheme
                                    )

                                    4 -> CategoriesScreen(
                                        categories = categories,
                                        onLoadAllServiceTypes = {
                                            repository.getAllServiceTypes()
                                        },
                                        onServiceTypeClick = { svcType ->
                                            selectedTab = 1
                                            selectedFinderTab = 1
                                            runChatQuery(svcType.name) { repository.searchByServiceType(svcType.slug) }
                                        },
                                        onSearchClick = { selectedTab = 1; selectedFinderTab = 1 },
                                        onBack = { selectedTab = 0 }
                                    )

                                    5 -> ServicesScreen(
                                        categories = categories,
                                        onLoadAllServiceTypes = {
                                            repository.getAllServiceTypes()
                                        },
                                        onServiceTypeClick = { svcType ->
                                            selectedTab = 1
                                            selectedFinderTab = 1
                                            runChatQuery(svcType.name) { repository.searchByServiceType(svcType.slug) }
                                        },
                                        onSearchClick = { selectedTab = 1; selectedFinderTab = 1 },
                                        onBack = { selectedTab = 0 }
                                    )

                                    1 -> FinderChoiceScreen(
                                         categories = categories,
                                         selectedTab = selectedFinderTab,
                                         onTabChange = { selectedFinderTab = it },
                                         chatMessages = chatMessages,
                                         userName = profile?.name,
                                         onSendMessage = { userText: String ->
                                             runChatQuery(userText) { repository.chat(userText) }
                                         },
                                         onClearChat = {
                                             chatMessages.clear()
                                             chatMessages.add(
                                                 TelegramChatMessage(
                                                     id = "welcome-new",
                                                     sender = "Nestora Bot",
                                                     text = "Conversation reset. What are you looking for?",
                                                     timestamp = getCurrentFormattedTime(),
                                                     isUser = false
                                                 )
                                             )
                                         },
                                         onBookListing = { listing -> bookingSheetListing = listing },
                                         userPhotoPath = userPhotoPath,
                                         onExploreClick = { selectedTab = 0 },
                                         onSelectLocationClick = { activeScreen = "map_picker" },
                                         onProfileClick = { selectedTab = 3 },
                                         onRegisterServiceClick = { selectedRegisterTab = 0; activeScreen = "register_choice" },
                                         onBookingsClick = { selectedTab = 2 },
                                         currentLocation = userLocation,
                                          isProviderMode = isProviderMode,
                                          onModeToggle = onModeToggle,
                                          tabsList = activeTabsList,
                                          selectedTabId = selectedTabId,
                                          onTabSelected = onTabSelected,
                                          currentTheme = todayTheme
                                     )

                                    2 -> BookingsScreen(
                                        bookings = bookings,
                                        viewerUserId = profile?.id,
                                        hasProviderListings = profile?.hasProviderListings == true,
                                        onBookingClick = { b ->
                                            selectedBookingId = b.id
                                            bookingPolling.openDetail(b.id)
                                            activeScreen = "booking_detail"
                                        },
                                        onPayClick = { b ->
                                            selectedBookingId = b.id
                                            bookingPolling.openDetail(b.id)
                                            activeScreen = "booking_detail"
                                        },
                                        onCancelClick = { b ->
                                            selectedBookingId = b.id
                                            bookingPolling.openDetail(b.id)
                                            activeScreen = "booking_detail"
                                        },
                                        currentLocation = userLocation,
                                        onSelectLocationClick = { activeScreen = "map_picker" },
                                        onProfileClick = { selectedTab = 3 },
                                        onRegisterServiceClick = { selectedRegisterTab = 0; activeScreen = "register_choice" },
                                        onBookingsClick = { selectedTab = 2 },
                                        onFindServiceClick = { selectedTab = 1; selectedFinderTab = 0 },
                                        onExploreClick = { selectedTab = 0 },
                                        onScrollChanged = { isScrolled = it },
                                        userPhotoPath = userPhotoPath,
                                        onRebookClick = { listing -> bookingSheetListing = listing },
                                         isProviderMode = isProviderMode,
                                         onModeToggle = onModeToggle,
                                         tabsList = activeTabsList,
                                         selectedTabId = selectedTabId,
                                         onTabSelected = onTabSelected,
                                         currentTheme = todayTheme
                                    )

                                    3 -> {
                                        val p = profile
                                        if (p != null && !guestMode) {
                                            ProfileScreen(
                                                profile = p,
                                                onLogout = {
                                                     prefs.edit().putBoolean("guest_mode", false).apply()
                                                     guestMode = false
                                                     profile = null
                                                     bookingPolling.clear()
                                                     TdLibManager.logOut()
                                                 },
                                                onBack = { selectedTab = 0 },
                                                onUpdateProfile = { updated ->
                                                    lifecycleScope.launch {
                                                        val saved = repository.updateUserProfile(updated)
                                                        if (saved != null) {
                                                            profile = saved
                                                        } else {
                                                            profile = updated
                                                        }
                                                    }
                                                },
                                                onUploadPhoto = { uri ->
                                                    val fileId = repository.uploadProfilePhoto(uri, this@MainActivity)
                                                    fileId
                                                },
                                                onResolvePhoto = { fileId ->
                                                    repository.getLocalPhotoPath(fileId, context)
                                                },
                                                onSearchAddress = { q, lat, lon -> repository.searchAddress(q, lat, lon) },
                                                onReverseGeocode = { lat, lon -> repository.reverseGeocode(lat, lon) },
                                                onAdminPayments = { activeScreen = "admin_payments" }
                                            )
                                        } else {
                                            GuestProfileScreen(
                                                onLoginClick = { 
                                                     prefs.edit().putBoolean("guest_mode", false).apply()
                                                     guestMode = false 
                                                 },
                                                onBack = { selectedTab = 0 }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sticky running-booking banner — a bare top-level sibling
                // (same trick as the BookingCreateSheet popup below: the root
                // composition here stacks direct children like an implicit
                // Box), so this overlays on top of *every* screen except the
                // booking-detail screen itself (which already shows full
                // status). Needs bookingPolling's list StateFlow to actually
                // be populated outside the Bookings tab — see the auth-ready
                // startListPolling() call above.
                if (activeScreen == "main" && selectedTab in listOf(0, 4, 5)) {
                    val activeBookings = bookings.filter {
                        it.stage.uppercase() !in listOf("DONE", "ENDED") &&
                        it.id !in dismissedBookingIds
                    }
                    if (activeBookings.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (activeBookings.size == 1) {
                                // Show single notification banner at the bottom
                                val booking = activeBookings.first()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(Alignment.Bottom)
                                        .navigationBarsPadding()
                                        .padding(bottom = if (activeScreen == "main" && selectedTab !in listOf(1, 2, 3)) 76.dp else 16.dp)
                                        .padding(horizontal = 16.dp)
                                ) {
                                    var offsetX by remember(booking.id) { mutableStateOf(0f) }
                                    val density = LocalDensity.current
                                    val swipeThreshold = with(density) { 100.dp.toPx() }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset { IntOffset(offsetX.roundToInt(), 0) }
                                            .pointerInput(booking.id) {
                                                detectHorizontalDragGestures(
                                                    onDragEnd = {
                                                        if (abs(offsetX) > swipeThreshold) {
                                                            dismissedBookingIds = dismissedBookingIds + booking.id
                                                        }
                                                        offsetX = 0f
                                                    },
                                                    onDragCancel = {
                                                        offsetX = 0f
                                                    },
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        offsetX += dragAmount
                                                    }
                                                )
                                            }
                                    ) {
                                        RunningBookingBanner(
                                            booking = booking,
                                            viewerUserId = profile?.id
                                        ) {
                                            selectedBookingId = booking.id
                                            bookingPolling.openDetail(booking.id)
                                            activeScreen = "booking_detail"
                                        }
                                    }
                                }
                            } else {
                                // More than 1 active booking: show floating bubble or expanded stacked cards
                                if (showAllBookingsExpanded) {
                                    // Full screen transparent scrim: tap anywhere to minimize
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.15f))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                showAllBookingsExpanded = false
                                            }
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(Alignment.Bottom)
                                            .navigationBarsPadding()
                                            .padding(bottom = if (activeScreen == "main" && selectedTab !in listOf(1, 2, 3)) 76.dp else 16.dp)
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Active Bookings (${activeBookings.size})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Tap screen to minimize",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }

                                        activeBookings.forEach { booking ->
                                            var offsetX by remember(booking.id) { mutableStateOf(0f) }
                                            val density = LocalDensity.current
                                            val swipeThreshold = with(density) { 100.dp.toPx() }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                                                    .pointerInput(booking.id) {
                                                        detectHorizontalDragGestures(
                                                            onDragEnd = {
                                                                if (abs(offsetX) > swipeThreshold) {
                                                                    dismissedBookingIds = dismissedBookingIds + booking.id
                                                                }
                                                                offsetX = 0f
                                                            },
                                                            onDragCancel = {
                                                                offsetX = 0f
                                                            },
                                                            onHorizontalDrag = { change, dragAmount ->
                                                                change.consume()
                                                                offsetX += dragAmount
                                                            }
                                                        )
                                                    }
                                            ) {
                                                RunningBookingBanner(
                                                    booking = booking,
                                                    viewerUserId = profile?.id
                                                ) {
                                                    selectedBookingId = booking.id
                                                    bookingPolling.openDetail(booking.id)
                                                    activeScreen = "booking_detail"
                                                    showAllBookingsExpanded = false
                                                }
                                            }
                                        }
                                    }
                                } else if (!isBubbleDismissedByUser) {
                                    // Floating bubble minimized state, above bottom menu bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = if (activeScreen == "main" && selectedTab !in listOf(1, 2, 3)) 76.dp else 16.dp)
                                            .padding(end = 16.dp),
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        val configuration = LocalConfiguration.current
                                        val density = LocalDensity.current
                                        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                                        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

                                        FloatingActionButton(
                                            onClick = { 
                                                showAllBookingsExpanded = true 
                                            },
                                            containerColor = Color(0xFF004332),
                                            contentColor = Color.White,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .offset { IntOffset(bubbleDragX.roundToInt(), bubbleDragY.roundToInt()) }
                                                .pointerInput(Unit) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            isDraggingBubble = true
                                                            totalDragDistanceThisSession = 0f
                                                        },
                                                        onDragEnd = {
                                                            isDraggingBubble = false
                                                        },
                                                        onDragCancel = {
                                                            isDraggingBubble = false
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            totalDragDistanceThisSession += kotlin.math.hypot(dragAmount.x, dragAmount.y)
                                                            
                                                            val bottomPadding = if (activeScreen == "main" && selectedTab !in listOf(1, 2, 3)) 76.dp else 16.dp
                                                            val bottomPaddingPx = with(density) { bottomPadding.toPx() }
                                                            val rightPaddingPx = with(density) { 16.dp.toPx() }
                                                            val fabSizePx = with(density) { 56.dp.toPx() }
                                                            val statusBarHeightPx = with(density) { 50.dp.toPx() } // status bar limit

                                                            val minX = - (screenWidthPx - rightPaddingPx - fabSizePx)
                                                            val maxX = rightPaddingPx
                                                            
                                                            val defaultTopY = screenHeightPx - bottomPaddingPx - fabSizePx
                                                            val minY = statusBarHeightPx - defaultTopY
                                                            val maxY = bottomPaddingPx

                                                            bubbleDragX = (bubbleDragX + dragAmount.x).coerceIn(minX, maxX)
                                                            bubbleDragY = (bubbleDragY + dragAmount.y).coerceIn(minY, maxY)
                                                        }
                                                    )
                                                }
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = Color(0xFFDC2626),
                                                        contentColor = Color.White
                                                    ) {
                                                        Text(
                                                            text = activeBookings.size.toString(),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "Active Bookings",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Booking creation sheet — renders via its own Popup layer
                // (ModalBottomSheet), so it's safe as a sibling here regardless
                // of which activeScreen/tab is showing underneath — EXCEPT
                // map_picker: while the user is redirected there to change the
                // booking's location, the sheet must be fully unmounted (not
                // just visually hidden) so it remounts fresh afterward and
                // re-seeds from pendingBookingAddress/Lat/Lon below.
                if (activeScreen != "map_picker") {
                bookingSheetListing?.let { listing ->
                    BookingCreateSheet(
                        listing = listing,
                        initialLocationText = pendingBookingAddress ?: userLocation ?: "Salt Lake, Sector V",
                        initialLat = pendingBookingLat ?: currentLat,
                        initialLon = pendingBookingLon ?: currentLon,
                        onDismiss = {
                            bookingSheetListing = null
                            pendingBookingAddress = null
                            pendingBookingLat = null
                            pendingBookingLon = null
                        },
                        onChangeLocationClick = {
                            mapPickerSource = "booking_create"
                            activeScreen = "map_picker"
                        },
                        onFetchQuote = { listingId -> repository.getBookingQuote(listingId) },
                        onConfirmBooking = { listingId, isHome, lat, lon, addr ->
                            repository.createBooking(listingId, isHome, lat, lon, addr)
                        },
                        onBookingCreated = { bookingId ->
                            loaderListingTitle = listing.title
                            loaderAddressText = if (userLocation?.isNotBlank() == true) userLocation ?: "" else "Center Appointment"
                            bookingSheetListing = null
                            selectedBookingId = bookingId
                            bookingPolling.openDetail(bookingId)
                            activeScreen = "booking_detail"
                        }
                    )
                }
                }
            }
        }
    }
}

@Composable
fun CustomNavItem(
    index: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = NestoraMint
    val inactiveColor = Color(0xFF8FA7A0)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        if (index == 1) {
            Icon(
                painter = painterResource(id = R.drawable.nestora_bottom_logo),
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
             val iconVec = when (index) {
                0 -> Icons.Default.LocationOn
                2 -> Icons.Default.DateRange
                3 -> Icons.Default.Person
                4 -> Icons.Default.List
                5 -> Icons.Default.Build
                else -> Icons.Default.Home
            }
            Icon(
                imageVector = iconVec,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}

private fun getBookingActionTurnText(booking: com.estatenestora.app.data.model.BookingSummary, viewerUserId: String?): String {
    val isProvider = booking.providerUserId == viewerUserId
    return when (booking.status.uppercase()) {
        "REQUESTED" -> {
            if (isProvider) "Your turn: Accept request" else "Provider's turn: Accept request"
        }
        "ACCEPTED" -> {
            if (isProvider) "Customer's turn: Confirm offer" else "Your turn: Confirm offer"
        }
        "PAYMENT_PENDING" -> {
            if (isProvider) "Customer's turn: Pay advance" else "Your turn: Pay advance"
        }
        "PAYMENT_UPLOADED" -> {
            "Admin's turn: Verify payment"
        }
        "CONFIRMED" -> {
            if (booking.isHomeService) {
                if (isProvider) "Your turn: Start travel" else "Provider's turn: Start travel"
            } else {
                if (isProvider) "Customer's turn: Travel to you" else "Your turn: Travel to provider"
            }
        }
        "PROVIDER_EN_ROUTE" -> {
            if (isProvider) "Your turn: Mark arrived" else "Provider's turn: Arriving"
        }
        "CUSTOMER_EN_ROUTE" -> {
            if (isProvider) "Customer's turn: Arriving" else "Your turn: Mark arrived"
        }
        "PROVIDER_ARRIVED", "CUSTOMER_ARRIVED" -> {
            if (isProvider) "Your turn: Verify OTP" else "Your turn: Share OTP"
        }
        "OTP_VERIFIED", "SERVICE_STARTED" -> {
            if (isProvider) "Your turn: Complete service" else "Provider's turn: In progress"
        }
        else -> {
            booking.stageLabel
        }
    }
}

@Composable
private fun RunningBookingBanner(
    booking: com.estatenestora.app.data.model.BookingSummary,
    viewerUserId: String?,
    onClick: () -> Unit
) {
    val isProvider = booking.providerUserId == viewerUserId

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = getRealLifeImageUrl(booking.listingTitle),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isProvider) Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
                ) {
                    Text(
                        text = if (isProvider) "Booking from Customer" else "Booking by You",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProvider) Color(0xFF1D4ED8) else Color(0xFF166534),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = booking.listingTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(Modifier.height(1.dp))

                val turnText = getBookingActionTurnText(booking, viewerUserId)
                val isYourTurn = turnText.startsWith("Your turn")
                Text(
                    text = turnText,
                    fontSize = 11.sp,
                    fontWeight = if (isYourTurn) FontWeight.Bold else FontWeight.Medium,
                    color = if (isYourTurn) Color(0xFFDC2626) else Color(0xFF475569)
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "See details",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "➔",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E)
                )
            }
        }
    }
}
