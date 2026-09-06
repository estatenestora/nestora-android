package com.estatenestora.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.R
import com.estatenestora.app.ui.theme.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@Composable
fun MicIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Mic capsule (rounded rectangle)
        val capsuleWidth = w * 0.32f
        val capsuleHeight = h * 0.46f
        val capsuleLeft = (w - capsuleWidth) / 2
        val capsuleTop = h * 0.16f
        
        val capsuleRoundRect = RoundRect(
            left = capsuleLeft,
            top = capsuleTop,
            right = capsuleLeft + capsuleWidth,
            bottom = capsuleTop + capsuleHeight,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleWidth / 2, capsuleWidth / 2)
        )
        val capsulePath = Path().apply {
            addRoundRect(capsuleRoundRect)
        }
        drawPath(
            path = capsulePath,
            color = color,
            style = Stroke(width = 1.8.dp.toPx())
        )

        // 2. Cradle (U-shaped cup around bottom half of the capsule)
        val cradlePath = Path().apply {
            val cradleRadius = capsuleWidth / 2 + 2.5.dp.toPx()
            addArc(
                oval = androidx.compose.ui.geometry.Rect(
                    left = w / 2 - cradleRadius,
                    top = capsuleTop + capsuleHeight * 0.22f,
                    right = w / 2 + cradleRadius,
                    bottom = capsuleTop + capsuleHeight + 2.5.dp.toPx()
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f
            )
        }
        drawPath(
            path = cradlePath,
            color = color,
            style = Stroke(width = 1.8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // 3. Stem line
        val stemTop = capsuleTop + capsuleHeight + 2.5.dp.toPx()
        val stemBottom = h * 0.8f
        drawLine(
            color = color,
            start = Offset(w / 2, stemTop),
            end = Offset(w / 2, stemBottom),
            strokeWidth = 1.8.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // 4. Flat base stand
        val baseWidth = w * 0.3f
        drawLine(
            color = color,
            start = Offset(w / 2 - baseWidth / 2, stemBottom),
            end = Offset(w / 2 + baseWidth / 2, stemBottom),
            strokeWidth = 1.8.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun DynamicCalendarIcon(isSelected: Boolean, modifier: Modifier = Modifier) {
    val calendar = java.util.Calendar.getInstance()
    val today = calendar.get(java.util.Calendar.DAY_OF_MONTH).toString()
    val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.US)
    val month = monthFormat.format(calendar.time).uppercase()

    Card(
        modifier = modifier.size(32.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().background(Color.White)
        ) {
            // Red header of calendar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0xFFFF5252))
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = today,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00382B),
                    lineHeight = 11.sp
                )
                Text(
                    text = month,
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8FA7A0),
                    lineHeight = 7.sp
                )
            }
        }
    }
}

@Composable
fun GlowingPulseIndicator(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(14.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .background(color.copy(alpha = alpha), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
fun OnboardingTopBar(
    currentLocation: String?,
    onSelectLocationClick: () -> Unit,
    onProfileClick: () -> Unit,
    userPhotoPath: String?,
    isProviderMode: Boolean,
    onModeToggle: () -> Unit,
    tabsList: List<NestoraTab>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    showPrimaryNavigationTabs: Boolean = false,
    transparentBackground: Boolean = false,
    currentTheme: RoyalTheme = remember { RoyalThemeRepository.getThemeForToday() }
) {
    val headerView = LocalView.current
    if (transparentBackground) {
        SideEffect {
            val window = (headerView.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, headerView).isAppearanceLightStatusBars = false
        }
        DisposableEffect(headerView) {
            onDispose {
                val window = (headerView.context as? android.app.Activity)?.window ?: return@onDispose
                WindowCompat.getInsetsController(window, headerView).isAppearanceLightStatusBars = true
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Header, status area and resting search container deliberately
            // share one base color. A separate gradient in each composable
            // creates visible stripes where those sections meet.
            .background(
                if (transparentBackground) Color.Transparent
                else currentTheme.backgroundGradient.first()
            )
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 0.dp)
    ) {
        Column {
            // ── TOP BAR: NESTORA LOGO & ADDRESS WITH ALWAYS-VISIBLE DROPDOWN & GREY PROFILE ICON ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable address block wrapping Nestora Logo, Address text, and static dropdown ▼
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectLocationClick() }
                        .padding(vertical = 4.dp)
                ) {
                    // Nestora Logo symbol with metallic border and glow
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFD700))
                                ),
                                shape = CircleShape
                            )
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.nestora_logo_symbol),
                            contentDescription = "Nestora Premium Logo",
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                        )
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    // Display full address (auto-truncates with ellipsis before touching dropdown ▼)
                    Text(
                        text = currentLocation ?: "Salt Lake, Sector V, Kolkata",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Spacer(Modifier.width(4.dp))
                    
                    // Location dropdown arrow (always visible, static design)
                    Text(
                        text = "▼",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // A compact Material-style status chip remains readable over
                // transparent carousel headers without introducing another
                // heavy block of theme colour.
                Surface(
                    onClick = onModeToggle,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, Color(0xFFD8E3DF)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(start = 7.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)
                    ) {
                        GlowingPulseIndicator(
                            color = if (isProviderMode) Color(0xFF00A878) else Color(0xFF356AE6),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isProviderMode) "SERVE" else "HIRE",
                            color = Color(0xFF123D32),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                // Right side: Dummy white profile icon with soft grey background (same size as Nestora Logo)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD4D8D9)) // Soft grey background
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (!userPhotoPath.isNullOrBlank()) {
                        AsyncImage(
                            model = File(userPhotoPath),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White, // Dummy white profile icon
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (showPrimaryNavigationTabs) {
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                // 1. Draw the baseline selected-colour rail first (so it is at the very bottom and drawn underneath)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(selectedMenuSurface(currentTheme))
                )

                // 2. Draw the tabs on top of the rail
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    // A narrow seam plus the rounded top corners creates the
                    // Swiggy-style V separation while every tab still meets the
                    // same lower colour rail.
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    tabsList.forEach { tab ->
                        val isActive = tab.id == selectedTabId
                        MenuTabCard(
                            tab = tab,
                            isActive = isActive,
                            theme = currentTheme,
                            compact = tabsList.size >= 4,
                            modifier = Modifier.weight(1f),
                            onClick = { onTabSelected(tab.id) }
                        )
                    }
                }
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MenuTabCard(
    tab: NestoraTab,
    isActive: Boolean,
    theme: RoyalTheme,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val layer = topMenuLayer(isActive)
    // The active destination is an inverted Swiggy-style tab. Its flat lower
    // edge joins the selected canvas immediately below this header.
    val tabShape = if (isActive) {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    }
    val scaleFactor by animateFloatAsState(
        targetValue = layer.scale,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "TabScaleAnimation"
    )
    val alpha by animateFloatAsState(
        targetValue = layer.alpha,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "TabLayerAlpha"
    )

    val cardBgColor by animateColorAsState(
        targetValue = topMenuTabBackground(theme, isActive),
        animationSpec = tween(durationMillis = 250),
        label = "TabColorAnimation"
    )

    val contentColor by animateColorAsState(
        targetValue = Color.White.copy(alpha = if (isActive) 1f else 0.68f),
        animationSpec = tween(durationMillis = 250),
        label = "TabTextColorAnimation"
    )

    Column(
        modifier = modifier
            .zIndex(layer.zIndex)
            .scale(scaleFactor)
            .height(
                when {
                    isActive && compact -> 64.dp
                    isActive -> 68.dp
                    compact -> 62.dp
                    else -> 64.dp
                }
            )
            .shadow(if (isActive) 2.dp else 0.dp, tabShape, clip = false)
            .clip(tabShape)
            .background(cardBgColor)
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) Color.White.copy(alpha = 0.58f) else Color.Transparent,
                shape = tabShape
            )
            .graphicsLayer { this.alpha = alpha }
            .selectable(
                selected = isActive,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = if (compact) 6.dp else 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = tab.iconEmoji,
            fontSize = if (compact) 20.sp else 23.sp,
            modifier = Modifier
                .scale(if (isActive) 1.08f else 1f)
                .offset(y = if (isActive) (-1).dp else 0.dp)
                .padding(bottom = if (compact) 2.5.dp else 5.dp)
        )
        Text(
            text = tab.label.uppercase(),
            color = contentColor,
            fontSize = if (compact) 8.5.sp else 10.sp,
            fontWeight = if (isActive) FontWeight.Black else FontWeight.Medium,
            letterSpacing = if (compact) 0.2.sp else 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(if (isActive) 32.dp else 20.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(topMenuIndicatorColor(isActive))
        )
    }
}

@Composable
fun OnboardingSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isScrolled: Boolean = false,
    hasCarouselBelow: Boolean = false,
    onClick: (() -> Unit)? = null,
    cartItemCount: Int = 0,
    onCartClick: (() -> Unit)? = null,
    currentTheme: RoyalTheme = remember { RoyalThemeRepository.getThemeForToday() }
) {
    val focusRequester = remember { FocusRequester() }
    // The resting field sits on the same selected-menu surface that begins
    // below the raised top tab. It returns to a plain white sticky header on
    // scroll, where it is no longer part of that navigation composition.
    val lightThemeHighlight = remember(currentTheme) { selectedMenuSurface(currentTheme) }
    // The field deliberately stays white across every theme. Keeping its
    // contents in the header colour preserves contrast without introducing a
    // second coloured strip below the navigation.
    val searchContentColor = currentTheme.backgroundGradient.first()
    val searchContext = androidx.compose.ui.platform.LocalContext.current
    // Only apply status-bar top padding when the search bar is pinned to the very top of
    // the screen (isScrolled=true). When it sits below OnboardingTopBar (isScrolled=false)
    // no extra padding is needed — the top bar already occupies the status bar area.
    val statusBarTopPadding = if (isScrolled) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }
    val rotatingServices = remember {
        listOf("Plumber", "Maid", "Electrician", "Broker", "Flat Owner", "Laundry")
    }
    var rotatingServiceIndex by remember { mutableStateOf(0) }
    var typedServiceLength by remember { mutableStateOf(0) }
    LaunchedEffect(rotatingServices) {
        while (true) {
            val currentService = rotatingServices[rotatingServiceIndex]
            typedServiceLength = 0
            currentService.indices.forEach {
                delay(70)
                typedServiceLength = it + 1
            }
            delay(900)
            currentService.indices.reversed().forEach {
                delay(38)
                typedServiceLength = it
            }
            rotatingServiceIndex = (rotatingServiceIndex + 1) % rotatingServices.size
        }
    }

    DisposableEffect(isScrolled, searchContext) {
        if (!isScrolled) return@DisposableEffect onDispose {}
        val window = (searchContext as? android.app.Activity)?.window
        if (window == null) return@DisposableEffect onDispose {}
        val previousColor = window.statusBarColor
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        val previousLightStatusBars = insetsController.isAppearanceLightStatusBars
        window.statusBarColor = android.graphics.Color.WHITE
        insetsController.isAppearanceLightStatusBars = true
        onDispose {
            window.statusBarColor = previousColor
            insetsController.isAppearanceLightStatusBars = previousLightStatusBars
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isScrolled) Color.White else lightThemeHighlight)
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = if (isScrolled) 12.dp else (if (hasCarouselBelow) 8.dp else 24.dp),
                top = statusBarTopPadding + (if (isScrolled) 8.dp else 12.dp)
            )
        ,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .border(1.dp, Color(0xFFE1E6E4), RoundedCornerShape(12.dp))
                .clickable {
                    if (onClick != null) {
                        onClick()
                    } else {
                        focusRequester.requestFocus()
                    }
                },
            shape = RoundedCornerShape(12.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = searchContentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Search for ",
                                color = searchContentColor.copy(alpha = 0.56f),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Text(
                                text = rotatingServices[rotatingServiceIndex].take(typedServiceLength),
                                color = searchContentColor.copy(alpha = 0.56f),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                    // A real text field consumes the first touch before the
                    // parent Surface sees it. When an active-search callback
                    // is supplied (Home), make this area a touch-forwarding
                    // preview; the floating overlay owns the actual input.
                    if (onClick == null) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 13.sp,
                                color = searchContentColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onClick() }
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                MicIcon(
                    color = searchContentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (onCartClick != null) {
            Surface(
                onClick = onCartClick,
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE1E6E4)),
                shadowElevation = if (isScrolled) 2.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge(containerColor = Color(0xFFB42318)) {
                                    Text(cartItemCount.coerceAtMost(99).toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Open cart",
                            tint = searchContentColor,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared floating search state for every top menu. The calling screen keeps
 * ownership of [query], so its own filtering/search flow receives the same
 * text after this card closes.
 */
@Composable
fun FloatingSearchOverlay(
    visible: Boolean,
    query: String,
    title: String = "Search for services & providers",
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit = {}
) {
    if (!visible) return
    val requester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val overlayContext = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(visible) {
        val window = (overlayContext as? android.app.Activity)?.window
        if (window != null && visible) {
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
    fun dismiss(submit: Boolean = false) {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        onDismiss()
        if (submit && query.isNotBlank()) onSubmit()
    }
    LaunchedEffect(Unit) { requester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { dismiss() }
    )
    // The Surface starts at y=0 so it paints a clean status-bar background;
    // only its content is inset below device-specific status icons/notches.
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp)) {
            Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = { dismiss() }, modifier = Modifier.align(Alignment.CenterStart).size(32.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close search", tint = Color(0xFF2A2A2A), modifier = Modifier.size(20.dp))
                }
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2A2A2A), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE0E0E0))
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (query.isBlank()) Text("Try 'Plumber' or 'Maid'", color = Color(0xFF9E9E9E), fontSize = 13.sp)
                        BasicTextField(
                            value = query, onValueChange = onQueryChange, singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = Color(0xFF0D1A13)),
                            modifier = Modifier.fillMaxWidth().focusRequester(requester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { dismiss(submit = true) })
                        )
                    }
                    Spacer(Modifier.width(8.dp)); Box(Modifier.fillMaxHeight().width(1.dp).background(Color(0xFFEAEAEA))); Spacer(Modifier.width(8.dp))
                    MicIcon(color = Color(0xFFFF5722), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
