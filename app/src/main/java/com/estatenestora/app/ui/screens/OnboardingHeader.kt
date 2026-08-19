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
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.R
import com.estatenestora.app.ui.theme.*

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
                    color = Color(0xFF004332),
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
fun OnboardingTopBar(
    currentLocation: String?,
    onSelectLocationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRegisterServiceClick: () -> Unit,
    onBookingsClick: () -> Unit,
    onFindServiceClick: () -> Unit,
    onExploreClick: () -> Unit,
    activeMenu: String = "none",
    userPhotoPath: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF005E46), Color(0xFF004332))
                )
            )
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
                    // Nestora Logo symbol on the left (perfectly cropped in circle)
                    Image(
                        painter = painterResource(id = R.drawable.nestora_logo_symbol),
                        contentDescription = "Nestora Logo",
                        modifier = Modifier.size(38.dp)
                    )
                    
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

                Spacer(Modifier.width(16.dp))

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

            Spacer(Modifier.height(12.dp))

            // ── MAIN MENU CARDS: Swiggy-style tab strip ──────────────────
            // Selected tab is filled with the app's own page-background color
            // (so it visually "pops" and reads as connected to the content
            // below, the same job Swiggy's orange pill does against its
            // maroon header). Unselected tabs get a light, translucent-white
            // version of that same idea, per the reference screenshot.
            // Icons are emoji glyphs, not hot-linked stock photos — they
            // render instantly offline and actually depict the object
            // (magnifier, compass, toolbox, receipt) instead of an unrelated
            // photograph, sized to sit comfortably inside a uniform rounded
            // rectangle rather than the old lopsided card shape.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                @Composable
                fun MenuTabCard(
                    label: String,
                    emoji: String,
                    isSelected: Boolean,
                    onClick: () -> Unit
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(78.dp)
                            .clickable { onClick() },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFFEDF2EE) else Color.White.copy(alpha = 0.14f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = emoji, fontSize = 26.sp)
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color(0xFF004332) else Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 1. EXPLORE
                MenuTabCard(
                    label = "EXPLORE",
                    emoji = "🧭",
                    isSelected = activeMenu == "explore",
                    onClick = onExploreClick
                )

                // 2. FINDER
                MenuTabCard(
                    label = "FINDER",
                    emoji = "🔍",
                    isSelected = activeMenu == "finder",
                    onClick = onFindServiceClick
                )

                // 3. REGISTER
                MenuTabCard(
                    label = "REGISTER",
                    emoji = "🧰",
                    isSelected = activeMenu == "register",
                    onClick = onRegisterServiceClick
                )

                // 4. BOOKINGS
                MenuTabCard(
                    label = "BOOKINGS",
                    emoji = "🧾",
                    isSelected = activeMenu == "bookings",
                    onClick = onBookingsClick
                )
            }
        }
    }
}

@Composable
fun OnboardingSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isScrolled: Boolean = false, // If scrolled, changes background to white and adds statusBarsPadding to sit below camera notch
    hasCarouselBelow: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .let { modifier ->
                if (!isScrolled && !hasCarouselBelow) {
                    modifier.clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                } else {
                    modifier
                }
            }
            .background(if (isScrolled) Color.White else Color(0xFF004332))
            .let { modifier ->
                if (isScrolled) {
                    modifier.statusBarsPadding()
                } else {
                    modifier
                }
            }
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = if (isScrolled) 12.dp else (if (hasCarouselBelow) 8.dp else 24.dp),
                top = if (isScrolled) 8.dp else 0.dp
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                // Thin grey border only shown on white background to stand out elegantly
                .let { modifier ->
                    if (isScrolled) {
                        modifier.border(1.2.dp, Color(0xFFE2EAF2), RoundedCornerShape(12.dp))
                    } else {
                        modifier
                    }
                }
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
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF004332), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search for flats, plumbers, maids...", color = NestoraTextMuted, fontSize = 13.sp)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            color = Color(0xFF0D1A13)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Smooth line-art microphone voice search button placed directly inside search bar
                MicIcon(
                    color = NestoraMint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
