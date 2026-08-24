package com.estatenestora.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.data.model.ServiceListing
import com.estatenestora.app.data.model.ServiceType
import com.estatenestora.app.ui.components.ProjectFooter
import com.estatenestora.app.ui.theme.NestoraAmber
import com.estatenestora.app.ui.theme.NestoraMint
import com.estatenestora.app.ui.theme.NestoraTextMuted

// ── Filter State Model ───────────────────────────────────────────────────────
data class ServiceFilterState(
    val sortBy: String = "Relevance", // "Relevance", "Distance: Nearby To Far", "Popularity: High to Low", "Cost: Low to High", "Cost: High to Low", "Rating: High to Low"
    val availableToday: Boolean = false,
    val availableTomorrow: Boolean = false,
    val distanceOption: String = "Any", // "Any", "Within 2 km", "Within 5 km", "Within 10 km"
    val ratingMin: Float = 0f, // 0f, 3.5f, 4.0f, 4.5f
    val verifiedOnly: Boolean = false,
    val budgetOption: String = "Any" // "Any", "Under ₹500", "₹500 - ₹2000", "Above ₹2000"
) {
    val activeFilterCount: Int
        get() = (if (sortBy != "Relevance") 1 else 0) +
                (if (availableToday) 1 else 0) +
                (if (availableTomorrow) 1 else 0) +
                (if (distanceOption != "Any") 1 else 0) +
                (if (ratingMin > 0f) 1 else 0) +
                (if (verifiedOnly) 1 else 0) +
                (if (budgetOption != "Any") 1 else 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceListingsScreen(
    serviceType: ServiceType,
    allListings: List<ServiceListing>,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onListingClick: (ServiceListing) -> Unit,
    onBookListing: (ServiceListing) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    var filterState by remember { mutableStateOf(ServiceFilterState()) }
    var isFilterSheetOpen by remember { mutableStateOf(false) }
    var isSortDropdownOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }

    // Filter and sort listings client-side
    val filteredListings = remember(allListings, filterState, serviceType, searchQuery) {
        var result = allListings.filter { listing ->
            if (!listing.isActive) return@filter false

            val matchesType = listing.serviceType.contains(serviceType.name, ignoreCase = true) ||
                    listing.title.contains(serviceType.name, ignoreCase = true) ||
                    listing.categoryName.contains(serviceType.categorySlug, ignoreCase = true) ||
                    serviceType.slug.isBlank() ||
                    listing.serviceType.contains(serviceType.slug, ignoreCase = true)
            
            val matchesQuery = searchQuery.isBlank() ||
                    listing.title.contains(searchQuery, ignoreCase = true) ||
                    listing.providerName.contains(searchQuery, ignoreCase = true) ||
                    listing.location.contains(searchQuery, ignoreCase = true)

            matchesType && matchesQuery
        }

        // Quick fallback: if exact serviceType matching returns empty, show all active listings filtered by searchQuery & filters
        if (result.isEmpty() && searchQuery.isBlank()) {
            result = allListings.filter { it.isActive }
        }

        // Apply filters
        if (filterState.verifiedOnly) {
            result = result.filter { it.isVerified }
        }
        if (filterState.ratingMin > 0f) {
            result = result.filter { it.rating >= filterState.ratingMin }
        }
        if (filterState.budgetOption == "Under ₹500") {
            result = result.filter { it.price <= 500 }
        } else if (filterState.budgetOption == "₹500 - ₹2000") {
            result = result.filter { it.price in 500.0..2000.0 }
        } else if (filterState.budgetOption == "Above ₹2000") {
            result = result.filter { it.price > 2000 }
        }

        // Apply Sort
        when (filterState.sortBy) {
            "Rating: High to Low" -> result.sortedByDescending { it.rating }
            "Cost: Low to High" -> result.sortedBy { it.price }
            "Cost: High to Low" -> result.sortedByDescending { it.price }
            "Popularity: High to Low" -> result.sortedByDescending { it.matchScore }
            else -> result
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
    ) {
        // ── TOP HEADER BAR ───────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = if (isScrolled) 4.dp else 1.dp
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
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0D1A13)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = serviceType.name.ifBlank { "Service Listings" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1A13),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${filteredListings.size} professionals available near you",
                            fontSize = 11.sp,
                            color = NestoraTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) NestoraMint else Color(0xFF0D1A13)
                        )
                    }
                }

                if (isSearchActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name, area, or service...", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NestoraMint,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    }
                }

                // ── SS2 STICKY QUICK FILTER BAR ──────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Pill Button with Badge Count
                    Surface(
                        onClick = { isFilterSheetOpen = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (filterState.activeFilterCount > 0) Color(0xFFE8F5E9) else Color.White,
                        border = BorderStroke(1.dp, if (filterState.activeFilterCount > 0) NestoraMint else Color(0xFFE0E0E0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Filter",
                                tint = if (filterState.activeFilterCount > 0) NestoraMint else Color(0xFF555555),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (filterState.activeFilterCount > 0) "Filter (${filterState.activeFilterCount})" else "Filter",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filterState.activeFilterCount > 0) NestoraMint else Color(0xFF333333)
                            )
                        }
                    }

                    // Sort By Dropdown Pill
                    Box {
                        Surface(
                            onClick = { isSortDropdownOpen = true },
                            shape = RoundedCornerShape(20.dp),
                            color = if (filterState.sortBy != "Relevance") Color(0xFFFFF8E8) else Color.White,
                            border = BorderStroke(1.dp, if (filterState.sortBy != "Relevance") NestoraAmber else Color(0xFFE0E0E0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sort by ▾",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF333333)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isSortDropdownOpen,
                            onDismissRequest = { isSortDropdownOpen = false }
                        ) {
                            listOf(
                                "Relevance",
                                "Rating: High to Low",
                                "Cost: Low to High",
                                "Cost: High to Low",
                                "Popularity: High to Low"
                            ).forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            fontSize = 13.sp,
                                            fontWeight = if (filterState.sortBy == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (filterState.sortBy == option) NestoraMint else Color.Unspecified
                                        )
                                    },
                                    onClick = {
                                        filterState = filterState.copy(sortBy = option)
                                        isSortDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Quick Chip: Available Today
                    FilterChip(
                        selected = filterState.availableToday,
                        onClick = { filterState = filterState.copy(availableToday = !filterState.availableToday) },
                        label = { Text("Available Today", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NestoraMint,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF444444)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filterState.availableToday,
                            borderColor = Color(0xFFE0E0E0),
                            selectedBorderColor = NestoraMint
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    // Quick Chip: Verified
                    FilterChip(
                        selected = filterState.verifiedOnly,
                        onClick = { filterState = filterState.copy(verifiedOnly = !filterState.verifiedOnly) },
                        label = { Text("✅ Verified", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NestoraMint,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF444444)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filterState.verifiedOnly,
                            borderColor = Color(0xFFE0E0E0),
                            selectedBorderColor = NestoraMint
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    // Quick Chip: 4★ & above
                    FilterChip(
                        selected = filterState.ratingMin >= 4f,
                        onClick = {
                            filterState = filterState.copy(ratingMin = if (filterState.ratingMin >= 4f) 0f else 4f)
                        },
                        label = { Text("⭐ 4.0+", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NestoraAmber,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF444444)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filterState.ratingMin >= 4f,
                            borderColor = Color(0xFFE0E0E0),
                            selectedBorderColor = NestoraAmber
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    // Quick Chip: Under ₹2000
                    FilterChip(
                        selected = filterState.budgetOption == "Under ₹500" || filterState.budgetOption == "₹500 - ₹2000",
                        onClick = {
                            filterState = filterState.copy(
                                budgetOption = if (filterState.budgetOption != "Any") "Any" else "₹500 - ₹2000"
                            )
                        },
                        label = { Text("💰 Under ₹2000", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NestoraMint,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF444444)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filterState.budgetOption != "Any",
                            borderColor = Color(0xFFE0E0E0),
                            selectedBorderColor = NestoraMint
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // ── LISTINGS FEED ────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NestoraMint)
                    }
                }
            } else if (filteredListings.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2EBE5))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔎", fontSize = 42.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No service providers found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D1A13)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Try clearing active filters or searching for another location.",
                                fontSize = 12.sp,
                                color = NestoraTextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { filterState = ServiceFilterState(); searchQuery = "" },
                                colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Clear Filters", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredListings) { listing ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        MarketplaceListingCard(
                            listing = listing,
                            onClick = { onListingClick(listing) },
                            onBookViaTelegram = { onBookListing(listing) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { ProjectFooter() }
        }
    }

    // ── SS3 TWO-COLUMN FILTER BOTTOM SHEET ───────────────────────────────────
    if (isFilterSheetOpen) {
        FilterBottomSheet(
            currentFilterState = filterState,
            onDismiss = { isFilterSheetOpen = false },
            onApply = { updatedFilter ->
                filterState = updatedFilter
                isFilterSheetOpen = false
            }
        )
    }
}

// ─── SS3 FILTER BOTTOM SHEET COMPOSABLE ──────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilterState: ServiceFilterState,
    onDismiss: () -> Unit,
    onApply: (ServiceFilterState) -> Unit
) {
    var draftFilter by remember { mutableStateOf(currentFilterState) }
    var selectedCategoryIndex by remember { mutableStateOf(0) }

    val categories = listOf(
        "Sort",
        "Available Today",
        "Available Tomorrow",
        "Distance",
        "Ratings",
        "Restaurant Category",
        "Dietary Preferences",
        "Discount",
        "Amenities",
        "Cost for two"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D1A13)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF2F2F2))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // Two-Column Body
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // LEFT SIDEBAR (35% width, light gray background)
                Column(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .background(Color(0xFFF7F8FA))
                        .verticalScroll(rememberScrollState())
                ) {
                    categories.forEachIndexed { index, cat ->
                        val isSelected = selectedCategoryIndex == index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategoryIndex = index }
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .padding(vertical = 16.dp, horizontal = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(20.dp)
                                            .background(Color(0xFFFF5722), shape = RoundedCornerShape(2.dp))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFFF5722) else Color(0xFF444444),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                VerticalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                // RIGHT OPTIONS PANEL (65% width, white background)
                Column(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .background(Color.White)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    when (selectedCategoryIndex) {
                        0 -> { // Sort
                            Text(
                                text = "SORT BY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            listOf(
                                "Relevance",
                                "Distance: Nearby To Far",
                                "Popularity: High to Low",
                                "Cost for two: Low to High",
                                "Cost for two: High to Low",
                                "Rating: High to Low"
                            ).forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { draftFilter = draftFilter.copy(sortBy = option) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = draftFilter.sortBy == option,
                                        onClick = { draftFilter = draftFilter.copy(sortBy = option) },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF5722))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = option,
                                        fontSize = 13.sp,
                                        fontWeight = if (draftFilter.sortBy == option) FontWeight.Bold else FontWeight.Normal,
                                        color = Color(0xFF222222)
                                    )
                                }
                            }
                        }
                        1 -> { // Available Today
                            Text("AVAILABILITY TODAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { draftFilter = draftFilter.copy(availableToday = !draftFilter.availableToday) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = draftFilter.availableToday,
                                    onCheckedChange = { draftFilter = draftFilter.copy(availableToday = it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF5722))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Available Today Only", fontSize = 13.sp, color = Color(0xFF222222))
                            }
                        }
                        2 -> { // Available Tomorrow
                            Text("AVAILABILITY TOMORROW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { draftFilter = draftFilter.copy(availableTomorrow = !draftFilter.availableTomorrow) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = draftFilter.availableTomorrow,
                                    onCheckedChange = { draftFilter = draftFilter.copy(availableTomorrow = it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF5722))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Available Tomorrow", fontSize = 13.sp, color = Color(0xFF222222))
                            }
                        }
                        3 -> { // Distance
                            Text("MAXIMUM DISTANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Spacer(Modifier.height(14.dp))
                            listOf("Any", "Within 2 km", "Within 5 km", "Within 10 km").forEach { dist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { draftFilter = draftFilter.copy(distanceOption = dist) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = draftFilter.distanceOption == dist,
                                        onClick = { draftFilter = draftFilter.copy(distanceOption = dist) },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF5722))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(dist, fontSize = 13.sp, color = Color(0xFF222222))
                                }
                            }
                        }
                        4 -> { // Ratings
                            Text("MINIMUM RATING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Spacer(Modifier.height(14.dp))
                            listOf(
                                0f to "Any Rating",
                                3.5f to "3.5★ & above",
                                4.0f to "4.0★ & above",
                                4.5f to "4.5★ & above"
                            ).forEach { (rating, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { draftFilter = draftFilter.copy(ratingMin = rating) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = draftFilter.ratingMin == rating,
                                        onClick = { draftFilter = draftFilter.copy(ratingMin = rating) },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF5722))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, fontSize = 13.sp, color = Color(0xFF222222))
                                }
                            }
                        }
                        9 -> { // Cost / Budget
                            Text("COST FOR TWO / BUDGET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Spacer(Modifier.height(14.dp))
                            listOf("Any", "Under ₹500", "₹500 - ₹2000", "Above ₹2000").forEach { budget ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { draftFilter = draftFilter.copy(budgetOption = budget) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = draftFilter.budgetOption == budget,
                                        onClick = { draftFilter = draftFilter.copy(budgetOption = budget) },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFF5722))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(budget, fontSize = 13.sp, color = Color(0xFF222222))
                                }
                            }
                        }
                        else -> {
                            Text("FILTER OPTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { draftFilter = draftFilter.copy(verifiedOnly = !draftFilter.verifiedOnly) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = draftFilter.verifiedOnly,
                                    onCheckedChange = { draftFilter = draftFilter.copy(verifiedOnly = it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF5722))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Verified Providers Only", fontSize = 13.sp, color = Color(0xFF222222))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // Sheet Footer Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { draftFilter = ServiceFilterState() }
                ) {
                    Text(
                        text = "Clear Filters",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { onApply(draftFilter) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAEAEA)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 36.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Apply",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF444444)
                    )
                }
            }
        }
    }
}
