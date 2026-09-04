package com.estatenestora.app.ui.screens

import com.estatenestora.app.ui.components.ProjectFooter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.Category
import com.estatenestora.app.data.model.ServiceType
import com.estatenestora.app.ui.theme.*

data class AttributeInfo(val label: String, val type: String, val options: List<String> = emptyList())

private val serviceAttributesMap = mapOf(
    "plumber" to listOf(
        AttributeInfo("Emergency Available", "Boolean"),
        AttributeInfo("Pipe Materials", "Select", listOf("PVC", "CPVC", "GI", "PPR")),
        AttributeInfo("Specializations", "Select", listOf("Bathroom Fitting", "Leak Repair", "Water Tank")),
        AttributeInfo("Carries Own Tools", "Boolean"),
        AttributeInfo("Visiting Charge (₹)", "Number")
    ),
    "electrician" to listOf(
        AttributeInfo("Emergency Available", "Boolean"),
        AttributeInfo("Specializations", "Select", listOf("Wiring", "MCB Work", "Fan Setup", "Solar")),
        AttributeInfo("Industrial Exp", "Boolean"),
        AttributeInfo("Visiting Charge (₹)", "Number")
    ),
    "ac_technician" to listOf(
        AttributeInfo("Brands Serviced", "Select", listOf("Daikin", "Voltas", "LG", "Samsung")),
        AttributeInfo("AC Types", "Select", listOf("Split", "Window", "Cassette")),
        AttributeInfo("Gas Refilling", "Boolean"),
        AttributeInfo("AMC Available", "Boolean")
    ),
    "photographer" to listOf(
        AttributeInfo("Shooting Styles", "Select", listOf("Wedding", "Portrait", "Product", "Event")),
        AttributeInfo("Drone Available", "Boolean"),
        AttributeInfo("Editing Included", "Boolean"),
        AttributeInfo("Delivery Days", "Number")
    ),
    "video_editor" to listOf(
        AttributeInfo("Software Used", "Select", listOf("Premiere", "Resolve", "FCP")),
        AttributeInfo("Content Types", "Select", listOf("YouTube", "Reels", "Corporate")),
        AttributeInfo("VFX / Motion Graphics", "Boolean")
    ),
    "broker" to listOf(
        AttributeInfo("RERA Registered", "Boolean"),
        AttributeInfo("Deal Types", "Select", listOf("Buy", "Sell", "Rent", "PG")),
        AttributeInfo("Property Types", "Select", listOf("Flat", "Villa", "Plot", "Office"))
    ),
    "maid_service" to listOf(
        AttributeInfo("Full/Part Time", "Select", listOf("Full Time", "Part Time", "Live-in")),
        AttributeInfo("Languages", "Select", listOf("Hindi", "English", "Local")),
        AttributeInfo("Police Verified", "Boolean")
    ),
    "cook" to listOf(
        AttributeInfo("Cuisine", "Select", listOf("North Indian", "South Indian", "Chinese")),
        AttributeInfo("Meals", "Select", listOf("Breakfast", "Lunch", "Dinner")),
        AttributeInfo("Veg Only Option", "Boolean")
    )
)

private val genericAttributes = listOf(
    AttributeInfo("Experience", "Years"),
    AttributeInfo("Availability", "Select", listOf("On-Demand", "Scheduled")),
    AttributeInfo("Verified Provider", "Boolean")
)

@Composable
fun ServicesScreen(
    categories: List<Category>,
    onLoadAllServiceTypes: suspend () -> List<ServiceType>,
    onServiceTypeClick: (ServiceType) -> Unit,
    onSearchClick: () -> Unit,
    onBack: () -> Unit,
    managedMedia: List<com.estatenestora.app.data.model.MediaAsset> = emptyList(),
    onResolveMedia: suspend (String) -> String? = { null }
) {
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var allServiceTypes by remember { mutableStateOf<List<ServiceType>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val rightPanelListState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            rightPanelListState.firstVisibleItemIndex > 0 || rightPanelListState.firstVisibleItemScrollOffset > 0
        }
    }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val activeCategories = remember(categories) { categories.filter { it.isActive } }
    val currentCategory = if (activeCategories.isNotEmpty() && selectedCategoryIndex < activeCategories.size) {
        activeCategories[selectedCategoryIndex]
    } else null

    // Load service types dynamically once at screen mount
    LaunchedEffect(activeCategories) {
        if (activeCategories.isNotEmpty() && allServiceTypes.isEmpty()) {
            isLoading = true
            try {
                allServiceTypes = onLoadAllServiceTypes()
            } catch (e: Throwable) {
                // silent fallback
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- DYNAMIC WHITE HEADER ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = if (isScrolled || isSearchExpanded) 3.dp else 0.dp
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0D1A13)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Services",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1A13),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchExpanded) NestoraMint else Color(0xFF0D1A13)
                        )
                    }
                }

                // Show search box only if user clicked search icon OR scrolled the page
                if (isSearchExpanded || isScrolled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OnboardingSearchBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            isScrolled = true, // Force white background style with thin border
                            hasCarouselBelow = false
                        )
                    }
                }
            }
        }

        if (activeCategories.isEmpty() || (isLoading && allServiceTypes.isEmpty())) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00382B))
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // --- LEFT PANEL: Categories Selector ---
                Column(
                    modifier = Modifier
                        .width(105.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFF7F9F7))
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(activeCategories.size) { idx ->
                            val cat = activeCategories[idx]
                            val isSelected = idx == selectedCategoryIndex
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF00382B) else Color.Transparent)
                                    .clickable { selectedCategoryIndex = idx }
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF555555),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // --- RIGHT PANEL: Services and Attributes list ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFEDF2EE))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    val catId = currentCategory?.id ?: ""
                    val list = remember(allServiceTypes, catId, searchQuery) {
                        allServiceTypes.filter {
                            it.isActive && it.categorySlug == catId &&
                            (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
                        }
                    }

                    if (list.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "No service definitions found here." else "No services matching \"$searchQuery\"",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = rightPanelListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(list) { svc ->
                                val managedAsset = managedMedia.firstOrNull { it.scope == "SERVICE_TYPE" && it.scopeId == svc.backendId }
                                val managedFileId = remember(managedAsset?.id) { managedAsset?.fileIdFor("THUMBNAIL") }
                                var managedPath by remember(managedFileId) { mutableStateOf<String?>(null) }
                                LaunchedEffect(managedFileId) { managedPath = managedFileId?.let { onResolveMedia(it) } }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onServiceTypeClick(svc) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    shadowElevation = 1.dp
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = managedPath ?: getRealLifeImageUrl(svc.slug),
                                                contentDescription = svc.name,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = svc.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF0D1A13)
                                                )
                                                if (svc.description.isNotBlank()) {
                                                    Text(
                                                        text = svc.description,
                                                        fontSize = 10.sp,
                                                        color = Color.Gray,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        // Attributes Section
                                        Text(
                                            text = "Service Attributes (Form Fields):",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00382B),
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(Modifier.height(6.dp))

                                        val attrs = serviceAttributesMap[svc.slug] ?: genericAttributes

                                        // Attributes Chips Row with Horizontal Scroll
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            attrs.forEach { attr ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFEDF2EE),
                                                    modifier = Modifier.padding(vertical = 1.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = attr.label,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF3E5C50)
                                                        )
                                                        Spacer(Modifier.width(3.dp))
                                                        Text(
                                                            text = "(${attr.type})",
                                                            fontSize = 7.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item { ProjectFooter() }
                        }
                    }
                }
            }
        }
    }
}
