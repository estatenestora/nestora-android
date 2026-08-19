package com.estatenestora.app.ui.screens

import coil.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.estatenestora.app.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.data.model.QuickPromptChip
import com.estatenestora.app.data.model.ServiceListing
import com.estatenestora.app.data.model.TelegramChatMessage
import com.estatenestora.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AIChatScreen(
    messages: List<TelegramChatMessage>,
    onSendMessage: (String) -> Unit,
    onSendSupportPayload: () -> Unit,
    onBookListing: (ServiceListing) -> Unit,
    onClearChat: () -> Unit,
    currentLocation: String? = null,
    onSelectLocationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onRegisterServiceClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onFindServiceClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onScrollChanged: (Boolean) -> Unit = {},
    userPhotoPath: String? = null,
    userName: String? = null
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    LaunchedEffect(isScrolled) {
        onScrollChanged(isScrolled)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch { listState.animateScrollToItem(messages.size) }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9F9F9) // Clean light-gray background matching brand theme
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Modern Deep Teal Brand Header matching dashboard style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF004D40))
                    .statusBarsPadding()
                    .padding(top = 8.dp, bottom = 8.dp, start = 4.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onExploreClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "Nestora AI",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(onClick = onClearChat) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                // Welcoming Dashboard at the top of the conversation
                item {
                    WelcomeDashboard(
                        userName = userName ?: "Nestora User",
                        isRegisterFlow = false,
                        onCardClick = { text ->
                            onSendMessage(text)
                        }
                    )
                }

                // Chat Message bubbles
                items(messages) { msg ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ChatMessageItem(
                            message = msg,
                            onBookListing = onBookListing,
                            userPhotoPath = userPhotoPath
                        )
                    }
                }
            }

            // ── INPUT BAR (Light Brand Theme style) ───────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                HorizontalDivider(color = Color(0xFFE2EAF2), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Ask Nestora to find any service...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                color = Color(0xFF2C2C2C)
                            ),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) Color(0xFF004D40) else Color(0xFFE2EAF2))
                            .clickable {
                                if (inputText.isNotBlank()) {
                                    val text = inputText
                                    inputText = ""
                                    onSendMessage(text)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── WELCOME DASHBOARD ────────────────────────────────────────────────────────

@Composable
fun WelcomeDashboard(
    userName: String,
    isRegisterFlow: Boolean,
    onCardClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Welcome Header
        Text(
            text = "Hello, $userName",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF004D40), // Bold brand deep teal
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Welcome to Nestora!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF555555) // High-contrast medium gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Responsive Orientation Configuration
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1.2f)) {
                    PrimaryActionCard(isRegisterFlow, onCardClick)
                }
                Box(modifier = Modifier.weight(1f)) {
                    SecondaryCardsColumn(isRegisterFlow, onCardClick)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PrimaryActionCard(isRegisterFlow, onCardClick)
                SecondaryCardsColumn(isRegisterFlow, onCardClick)
            }
        }
    }
}

@Composable
fun PrimaryActionCard(
    isRegisterFlow: Boolean,
    onCardClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, Color(0xFF004D40)), // Deep teal border outline
        colors = CardDefaults.cardColors(containerColor = Color.White), // Clean white background
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isRegisterFlow) {
                    onCardClick("I want to register a service")
                } else {
                    onCardClick("Find a service")
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFE8F5E9), CircleShape), // Soft mint green circle badge
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.nestora_bottom_logo),
                    contentDescription = "Logo",
                    tint = Color(0xFF004D40), // Deep teal brand icon tint
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isRegisterFlow) "Get started by setting up a service profile" else "Get started by setting up a search query",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF004D40), // Deep teal title
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isRegisterFlow) "Integrate your skills and location parameters to super-charge your business matching" else "Integrate plain-words description to instantly match with top local service providers",
                fontSize = 12.sp,
                color = Color(0xFF2C2C2C), // High-contrast dark text
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun SecondaryCardsColumn(
    isRegisterFlow: Boolean,
    onCardClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isRegisterFlow) "Try describing these options" else "Try out a search example",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF004D40) // Deep teal section header
        )

        if (isRegisterFlow) {
            SecondaryCard(
                title = "Try describing Yoga instruction",
                description = "Register a Yoga Trainer service with details like rate, availability, and specific skills.",
                iconRes = R.drawable.nestora_bottom_logo,
                iconTint = Color(0xFF004D40),
                onClick = { onCardClick("I want to register as a Yoga Trainer. I charge 800 per session, available weekends.") }
            )
            SecondaryCard(
                title = "Try describing AC maintenance",
                description = "Register an AC Technician service listing with coordinates and experience level.",
                iconRes = R.drawable.nestora_bottom_logo,
                iconTint = Color(0xFF004D40),
                onClick = { onCardClick("I want to register as an AC Technician. Rates start at 500, home services available.") }
            )
        } else {
            SecondaryCard(
                title = "Try an AI-powered flat search",
                description = "Search 2 BHK Luxury Flat in Ecospace Newtown with high-quality interior.",
                iconRes = R.drawable.nestora_bottom_logo,
                iconTint = Color(0xFF004D40),
                onClick = { onCardClick("Search 2 BHK Luxury Flat in Ecospace Newtown") }
            )
            SecondaryCard(
                title = "Try booking AC repair",
                description = "Find an expert technician for AC repair and gas filling in Salt Lake area.",
                iconRes = R.drawable.nestora_bottom_logo,
                iconTint = Color(0xFF004D40),
                onClick = { onCardClick("I need an AC repair technician in Salt Lake") }
            )
        }
    }
}

@Composable
fun SecondaryCard(
    title: String,
    description: String,
    iconRes: Int,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2EAF2)), // Clean light border
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Soft mint background
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape), // White circle badge background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF004D40) // Deep teal title
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF2C2C2C), // Dark charcoal description text
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ─── CHAT MESSAGE BUBBLE (Dark mode adaptation) ──────────────────────────────

@Composable
fun ChatMessageItem(
    message: TelegramChatMessage,
    onBookListing: (ServiceListing) -> Unit = {},
    showLocationButton: Boolean = false,
    onLocationButtonClick: () -> Unit = {},
    onSelectLocationClick: () -> Unit = {},
    showExploreButton: Boolean = false,
    onExploreButtonClick: () -> Unit = {},
    userPhotoPath: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)), // Soft mint avatar background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.nestora_bottom_logo),
                    contentDescription = "Nestora",
                    tint = Color(0xFF004D40), // Deep teal icon tint
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isUser) Color(0xFFE8F5E9) else Color.White, // Soft mint user bubble, clean white AI bubble
            border = BorderStroke(
                width = 1.dp,
                color = if (message.isUser) Color(0xFFC8E6C9) else Color(0xFFE2EAF2) // Subtle green border for user, soft gray for AI
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    color = if (message.isUser) Color(0xFF0D1A13) else Color(0xFF2C2C2C), // High-contrast text
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )

                if (showLocationButton) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onLocationButtonClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40)), // Deep teal brand button
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "GPS",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onSelectLocationClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40)), // Deep teal brand button
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Choose on Map",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (showExploreButton) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onExploreButtonClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40)), // Deep teal brand button
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Go to Explore",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (message.attachedListings.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    message.attachedListings.forEach { listing ->
                        MarketplaceListingCard(
                            listing = listing,
                            onClick = { onBookListing(listing) },
                            onBookViaTelegram = { onBookListing(listing) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = message.timestamp,
                    fontSize = 10.sp,
                    color = if (message.isUser) Color(0xFF5D7A68) else Color.Gray, // Mint-tinted gray for user, standard gray for AI
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        if (message.isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2EAF2)), // Soft light gray backdrop
                contentAlignment = Alignment.Center
            ) {
                if (!userPhotoPath.isNullOrBlank()) {
                    AsyncImage(
                        model = File(userPhotoPath),
                        contentDescription = "User",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = Color(0xFF004D40), // Deep teal brand icon tint
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
