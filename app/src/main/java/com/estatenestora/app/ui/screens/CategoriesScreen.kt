package com.estatenestora.app.ui.screens

import com.estatenestora.app.ui.components.ProjectFooter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.Category
import com.estatenestora.app.data.model.ServiceType
import com.estatenestora.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onLoadAllServiceTypes: suspend () -> List<ServiceType>,
    onServiceTypeClick: (ServiceType) -> Unit,
    onSearchClick: () -> Unit,
    onBack: () -> Unit,
    managedMedia: List<com.estatenestora.app.data.model.MediaAsset> = emptyList(),
    onResolveMedia: suspend (String) -> String? = { null }
) {
    var allServiceTypes by remember { mutableStateOf<List<ServiceType>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val scrollState = rememberScrollState()
    val isScrolled by remember { derivedStateOf { scrollState.value > 0 } }
    var isSearchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && allServiceTypes.isEmpty()) {
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
                        text = "Categories",
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

        // --- CONTENT ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .verticalScroll(scrollState)
        ) {
            if (categories.isEmpty() || (isLoading && allServiceTypes.isEmpty())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00382B))
                }
            } else {
                val filteredList = categories.filter { it.isActive }.mapNotNull { category ->
                    val services = allServiceTypes.filter { it.isActive && it.categorySlug == category.id }
                    val matchedServices = services.filter {
                        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || category.name.contains(searchQuery, ignoreCase = true)
                    }
                    if (searchQuery.isBlank() || matchedServices.isNotEmpty() || category.name.contains(searchQuery, ignoreCase = true)) {
                        category to matchedServices
                    } else {
                        null
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No categories found for \"$searchQuery\"",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    filteredList.forEachIndexed { index, (category, services) ->
                        val categoryAsset = managedMedia.firstOrNull { it.scope == "CATEGORY" && it.scopeId == category.backendId }
                        val categoryFileId = remember(categoryAsset?.id) { categoryAsset?.fileIdFor("THUMBNAIL") }
                        var categoryImage by remember(categoryFileId) { mutableStateOf<String?>(null) }
                        LaunchedEffect(categoryFileId) { categoryImage = categoryFileId?.let { onResolveMedia(it) } }
                        // Divider between sections (not before the first)
                        if (index > 0) {
                            HorizontalDivider(
                                color = Color(0xFFEDEDED),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp, bottom = 20.dp)
                        ) {
                            // Bold category title
                            Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!categoryImage.isNullOrBlank()) {
                                    AsyncImage(categoryImage, category.name, Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)), contentScale = ContentScale.Crop)
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(text = category.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D1A13))
                            }

                            Spacer(Modifier.height(14.dp))

                            if (services.isEmpty() && !isLoading) {
                                Text(
                                    text = "No services under this category",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            } else if (services.isEmpty() && isLoading) {
                                Text(
                                    text = "Loading services...",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Spacer(Modifier.width(6.dp))
                                    services.forEach { svc ->
                                        SwiggyStyleCard(
                                            label = svc.name,
                                            imageUrl = getRealLifeImageUrl(svc.slug.ifBlank { svc.name }),
                                            onClick = { onServiceTypeClick(svc) },
                                            imageSize = 76.dp,
                                            managedMedia = managedMedia.firstOrNull { it.scope == "SERVICE_TYPE" && it.scopeId == svc.backendId },
                                            onResolveMedia = onResolveMedia
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                }
                            }
                        }
                    }
                }
            }
            ProjectFooter()
        }
    }
}
