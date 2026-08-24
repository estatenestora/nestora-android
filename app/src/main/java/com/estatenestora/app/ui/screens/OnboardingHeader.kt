package com.estatenestora.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
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
    currentTheme: RoyalTheme = remember { RoyalThemeRepository.getThemeForToday() }
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Header, status area and resting search container deliberately
            // share one base color. A separate gradient in each composable
            // creates visible stripes where those sections meet.
            .background(currentTheme.backgroundGradient.first())
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Column {
            // ── TOP BAR: NESTORA LOGO & ADDRESS WITH ALWAYS-VISIBLE DROPDOWN & GREY PROFILE ICON ──
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                // Global role toggle: Glassmorphic design with pulsating glow dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                        .clickable { onModeToggle() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    if (isProviderMode) {
                        Text(
                            text = "SERVE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        GlowingPulseIndicator(color = Color(0xFF00FFB2))
                    } else {
                        GlowingPulseIndicator(color = Color.White)
                        Text(
                            text = "HIRE",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

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

            Spacer(Modifier.height(16.dp))

            // Every visible top-menu item shares the available screen width.
            // Adding another enabled tab later therefore reduces each card
            // proportionally instead of leaving a gap or requiring horizontal
            // scrolling.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    // A pale version of the active header color: it highlights the current
    // section without looking like an unrelated white card.
    val lightThemeHighlight = remember(theme) {
        lerp(theme.backgroundGradient.last(), Color.White, 0.86f)
    }
    // Hardware-accelerated UI state interpolation
    val scaleFactor by animateFloatAsState(
        targetValue = if (isActive) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "TabScaleAnimation"
    )

    val cardBgColor by animateColorAsState(
        targetValue = if (isActive) lightThemeHighlight else theme.inactiveTabCardBg,
        animationSpec = tween(durationMillis = 250),
        label = "TabColorAnimation"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF111111) else Color(0xCCFFFFFF),
        animationSpec = tween(durationMillis = 250),
        label = "TabTextColorAnimation"
    )

    Column(
        modifier = modifier
            .scale(scaleFactor)
            .height(if (compact) 64.dp else 68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBgColor)
            .clickable { onClick() }
            .padding(vertical = if (compact) 6.dp else 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = tab.iconEmoji,
            fontSize = if (compact) 19.sp else 22.sp,
            modifier = Modifier.padding(bottom = if (compact) 3.dp else 6.dp)
        )
        Text(
            text = tab.label.uppercase(),
            color = contentColor,
            fontSize = if (compact) 8.sp else 9.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = if (compact) 0.2.sp else 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    currentTheme: RoyalTheme = remember { RoyalThemeRepository.getThemeForToday() }
) {
    val focusRequester = remember { FocusRequester() }
    // Keep the search field visually connected to the selected tab while the
    // header is resting. It returns to white only when pinned on scroll.
    val lightThemeHighlight = remember(currentTheme) {
        lerp(currentTheme.backgroundGradient.last(), Color.White, 0.86f)
    }
    val searchContentColor = currentTheme.backgroundGradient.first()
    // Only apply status-bar top padding when the search bar is pinned to the very top of
    // the screen (isScrolled=true). When it sits below OnboardingTopBar (isScrolled=false)
    // no extra padding is needed — the top bar already occupies the status bar area.
    val statusBarTopPadding = if (isScrolled) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isScrolled) Color.White else currentTheme.backgroundGradient.first())
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = if (isScrolled) 12.dp else (if (hasCarouselBelow) 8.dp else 24.dp),
                top = statusBarTopPadding + (if (isScrolled) 8.dp else 0.dp)
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .border(1.2.dp, Color(0xFFE2EAF2), RoundedCornerShape(12.dp))
                .clickable {
                    if (onClick != null) {
                        onClick()
                    } else {
                        focusRequester.requestFocus()
                    }
                },
            shape = RoundedCornerShape(12.dp),
            color = if (isScrolled) Color.White else lightThemeHighlight
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
                    contentAlignment = if (onClick != null) Alignment.Center else Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search for flats, plumbers, maids...",
                                color = searchContentColor.copy(alpha = 0.56f),
                            fontSize = 13.sp,
                            textAlign = if (onClick != null) TextAlign.Center else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
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
