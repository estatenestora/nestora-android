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
    currentLocation: String? = null
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
                        // Finder Tab Button
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
                                imageVector = Icons.Default.Search,
                                contentDescription = "Finder",
                                tint = if (selectedTab == 0) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Finder",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) NestoraMint else Color(0xFF888888)
                            )
                        }

                        // Assistant Tab Button
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
                                painter = painterResource(id = R.drawable.nestora_bottom_logo),
                                contentDescription = "Assistant",
                                tint = if (selectedTab == 1) NestoraMint else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Assistant",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) NestoraMint else Color(0xFF888888)
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
                                onRegisterServiceClick = onRegisterServiceClick,
                                onBookingsClick = onBookingsClick,
                                onFindServiceClick = {},
                                onExploreClick = onExploreClick,
                                activeMenu = "finder",
                                userPhotoPath = userPhotoPath
                            )
                        }

                        stickyHeader {
                            OnboardingSearchBar(
                                searchQuery = "",
                                onSearchQueryChange = {},
                                isScrolled = isScrolled,
                                hasCarouselBelow = true,
                                onClick = { onTabChange(1) } // Navigate to Assistant search query when search bar clicked
                            )
                        }

                        // Swiggy-style Hero Carousel
                        item {
                            HeroCarousel(theme = "finder")
                        }

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
                    AIChatScreen(
                        messages = chatMessages,
                        userName = userName,
                        onSendMessage = onSendMessage,
                        onSendSupportPayload = {},
                        onBookListing = onBookListing,
                        onExploreClick = { onTabChange(0) }, // Go back to Finder landing tab first!
                        onClearChat = onClearChat,
                        userPhotoPath = userPhotoPath
                    )
                }
            }
        }
    }
}
