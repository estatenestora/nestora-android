package com.estatenestora.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Share
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.AndroidBridgeResponse
import com.estatenestora.app.data.model.CustomerCatalogPresentation
import com.estatenestora.app.data.model.ListingServiceCatalog
import com.estatenestora.app.data.model.ProviderServiceOffering
import com.estatenestora.app.data.model.ProviderServicePackage
import com.estatenestora.app.data.model.MediaAsset
import com.estatenestora.app.data.model.ServiceListing
import com.estatenestora.app.ui.theme.NestoraMint
import kotlinx.coroutines.launch
import java.util.Locale

/** One cart is deliberately locked to one provider and one service type. The
 * backend remains authoritative for all prices, durations, and ownership. */
internal data class CustomerProviderCart(
    val listing: ServiceListing,
    val providerId: String,
    val serviceTypeId: String,
    val packageId: String? = null,
    val offeringQuantities: Map<String, Int> = emptyMap(),
    val useListingPrice: Boolean = false,
    val itemCount: Int,
    val providerAmount: Double,
    val durationMinutes: Int
)

internal fun customerCartMatchesCatalog(cart: CustomerProviderCart?, catalog: ListingServiceCatalog): Boolean =
    cart != null && cart.providerId == catalog.providerId && cart.serviceTypeId == catalog.serviceTypeId

internal fun customerCartFromSelection(
    listing: ServiceListing,
    catalog: ListingServiceCatalog,
    packageId: String?,
    offeringQuantities: Map<String, Int>,
    useListingPrice: Boolean,
    defaultDurationMinutes: Int = 60
): CustomerProviderCart? {
    val singleUnitQuantities = offeringQuantities
        .filterKeys { it.isNotBlank() }
        .filterValues { it > 0 }
        .mapValues { 1 }
    val summary = customerServiceCartSummary(
        catalog = catalog,
        packageId = packageId,
        offeringQuantities = singleUnitQuantities,
        useListingPrice = useListingPrice,
        listingPrice = listing.price,
        defaultDurationMinutes = defaultDurationMinutes
    ) ?: return null
    return CustomerProviderCart(
        listing = listing,
        providerId = catalog.providerId,
        serviceTypeId = catalog.serviceTypeId,
        packageId = packageId,
        offeringQuantities = singleUnitQuantities,
        useListingPrice = useListingPrice,
        itemCount = summary.itemCount.coerceAtLeast(if (useListingPrice) 1 else 0),
        providerAmount = summary.providerAmount,
        durationMinutes = summary.durationMinutes
    )
}

internal data class CustomerCatalogSearchResult(
    val packages: List<ProviderServicePackage>,
    val offeringGroups: Map<String, List<ProviderServiceOffering>>
)

internal fun customerOfferingGroupLabel(offer: ProviderServiceOffering): String {
    val attributes = offer.attributeValues
    val preferredKeys = listOf("service_group", "work_category", "category", "work_type", "room_type")
    val stored = preferredKeys.firstNotNullOfOrNull { key ->
        attributes?.get(key)?.takeUnless { it.isJsonNull }?.let { value ->
            when {
                value.isJsonPrimitive -> value.asString
                value.isJsonArray -> value.asJsonArray.firstOrNull()?.takeIf { it.isJsonPrimitive }?.asString
                else -> null
            }
        }?.trim()?.takeIf(String::isNotBlank)
    }
    return stored?.replace('_', ' ')?.lowercase(Locale.getDefault())
        ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        ?: "Individual services"
}

private fun ProviderServiceOffering.matchesCatalogQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val attributeText = attributeValues?.entrySet()?.joinToString(" ") { (key, value) -> "$key $value" }.orEmpty()
    return listOf(title, description, attributeText).joinToString(" ")
        .contains(query, ignoreCase = true)
}

private fun ProviderServicePackage.matchesCatalogQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return listOf(name, description, includedText, excludedText, items.joinToString(" ") { it.title })
        .joinToString(" ")
        .contains(query, ignoreCase = true)
}

internal fun customerCatalogSearch(
    catalog: ListingServiceCatalog,
    query: String,
    mode: String = "ALL",
    maximumPrice: Double? = null,
    providerContext: String = ""
): CustomerCatalogSearchResult {
    val cleanQuery = query.trim()
    val itemQuery = cleanQuery.takeUnless { providerContext.contains(it, ignoreCase = true) }.orEmpty()
    val packages = if (mode == "SERVICES") emptyList() else catalog.packages.filter { pack ->
        pack.matchesCatalogQuery(itemQuery) && (maximumPrice == null || pack.packagePriceAmount <= maximumPrice)
    }
    val offerings = if (mode == "PACKAGES") emptyList() else catalog.offerings.filter { offer ->
        offer.matchesCatalogQuery(itemQuery) && (maximumPrice == null || offer.priceAmount <= maximumPrice)
    }
    return CustomerCatalogSearchResult(
        packages = packages,
        offeringGroups = offerings.groupBy(::customerOfferingGroupLabel)
    )
}

private sealed interface StorefrontBlock {
    val key: String

    data class Section(
        override val key: String,
        val title: String,
        val count: Int,
        val subtitle: String = ""
    ) : StorefrontBlock

    data class Package(override val key: String, val value: ProviderServicePackage) : StorefrontBlock
    data class Offering(override val key: String, val value: ProviderServiceOffering) : StorefrontBlock
    data object CustomService : StorefrontBlock { override val key: String = "custom-service" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerServiceCatalogScreen(
    listing: ServiceListing,
    currentCart: CustomerProviderCart?,
    cartOnly: Boolean,
    onBack: () -> Unit,
    onFetchCatalog: suspend (String) -> AndroidBridgeResponse?,
    onResolveMedia: suspend (String) -> String?,
    onCartChanged: (CustomerProviderCart?) -> Unit,
    onOpenCart: () -> Unit,
    onCheckout: (CustomerProviderCart) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var catalog by remember(listing.id) { mutableStateOf<ListingServiceCatalog?>(null) }
    var loading by remember(listing.id) { mutableStateOf(true) }
    var error by remember(listing.id) { mutableStateOf<String?>(null) }
    var switchConflict by remember(listing.id) { mutableStateOf(false) }
    var selectedPackageId by remember(listing.id) { mutableStateOf<String?>(null) }
    var useListingPrice by remember(listing.id) { mutableStateOf(false) }
    val quantities = remember(listing.id) { mutableStateMapOf<String, Int>() }

    fun updateCart(
        nextPackageId: String? = selectedPackageId,
        nextQuantities: Map<String, Int> = quantities,
        nextUseListingPrice: Boolean = useListingPrice
    ) {
        val loaded = catalog ?: return
        onCartChanged(
            customerCartFromSelection(
                listing = listing,
                catalog = loaded,
                packageId = nextPackageId,
                offeringQuantities = nextQuantities,
                useListingPrice = nextUseListingPrice
            )
        )
    }

    LaunchedEffect(listing.id) {
        loading = true
        error = null
        val response = onFetchCatalog(listing.id)
        val loaded = response?.serviceCatalog
        if (response?.ok == true && loaded != null) {
            catalog = loaded
            if (currentCart != null && !customerCartMatchesCatalog(currentCart, loaded)) {
                switchConflict = true
            } else if (currentCart != null) {
                selectedPackageId = currentCart.packageId?.takeIf { id -> loaded.packages.any { it.id == id } }
                quantities.clear()
                currentCart.offeringQuantities
                    .filterKeys { id -> loaded.offerings.any { it.id == id } }
                    .forEach { (id, _) -> quantities[id] = 1 }
                useListingPrice = currentCart.useListingPrice
            }
        } else {
            error = response?.reply ?: "Could not load this provider's services. Check your connection and try again."
        }
        loading = false
    }

    if (switchConflict) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text("Start a new service cart?") },
            text = {
                val existing = currentCart
                Text(if (existing?.providerId == catalog?.providerId) {
                    "Your cart contains another service type from ${existing?.listing?.providerName ?: "this provider"}. Clear it before starting this service booking."
                } else {
                    "Your cart contains services from ${existing?.listing?.providerName ?: "another provider"}. Clear it before adding services from ${listing.providerName}."
                })
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCartChanged(null)
                        selectedPackageId = null
                        quantities.clear()
                        useListingPrice = false
                        switchConflict = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                ) { Text("Clear and continue") }
            },
            dismissButton = { TextButton(onClick = onBack) { Text("Keep current cart") } }
        )
    }

    Scaffold(
        topBar = {
            if (cartOnly) {
                StorefrontUrbanCompanyTopBar(
                    title = "Your cart",
                    subtitle = listing.providerName,
                    isScrolled = true,
                    onBack = onBack,
                    showSearch = false,
                    showShare = false,
                    showCart = false
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val loaded = catalog
            val selectedCart = loaded?.let {
                customerCartFromSelection(
                    listing = listing,
                    catalog = it,
                    packageId = selectedPackageId,
                    offeringQuantities = quantities,
                    useListingPrice = useListingPrice
                )
            }
            StorefrontCartBar(
                cart = selectedCart,
                cartOnly = cartOnly,
                presentation = loaded?.customerPresentation,
                onOpenCart = {
                    if (selectedCart == null) {
                        scope.launch { snackbarHostState.showSnackbar("Add at least one package or service to continue.") }
                    } else if (cartOnly) {
                        onCheckout(selectedCart)
                    } else {
                        onOpenCart()
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color(0xFFF6F8F7)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> {
                    if (cartOnly) {
                        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NestoraMint)
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                            StorefrontUrbanCompanyHero(
                                listing = listing,
                                managedMedia = emptyList(),
                                onResolveMedia = onResolveMedia
                            )
                            Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = NestoraMint)
                            }
                        }
                        StorefrontUrbanCompanyTopBar(
                            title = listing.title,
                            subtitle = "by ${listing.providerName}",
                            isScrolled = false,
                            cartItemCount = currentCart?.itemCount ?: 0,
                            onBack = onBack,
                            onSearch = {},
                            onShare = {},
                            onCart = {
                                if (currentCart == null) {
                                    scope.launch { snackbarHostState.showSnackbar("Your cart is empty. Add a package or service first.") }
                                } else {
                                    onOpenCart()
                                }
                            }
                        )
                    }
                }
                error != null -> {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        if (!cartOnly) {
                            StorefrontUrbanCompanyHero(
                                listing = listing,
                                managedMedia = emptyList(),
                                onResolveMedia = onResolveMedia
                            )
                            HorizontalDivider(thickness = 8.dp, color = Color(0xFFEFF2F1))
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                            Button(
                                onClick = {
                                    loading = true
                                    error = null
                                    scope.launch {
                                        val response = onFetchCatalog(listing.id)
                                        catalog = response?.serviceCatalog
                                        error = if (response?.ok == true && catalog != null) null else response?.reply ?: "Could not load provider services."
                                        loading = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                            ) { Text("Try again") }
                        }
                    }
                    if (!cartOnly) {
                        StorefrontUrbanCompanyTopBar(
                            title = listing.title,
                            subtitle = "by ${listing.providerName}",
                            isScrolled = false,
                            cartItemCount = currentCart?.itemCount ?: 0,
                            onBack = onBack,
                            onSearch = {},
                            onShare = {},
                            onCart = { onOpenCart() }
                        )
                    }
                }
                catalog != null && !switchConflict -> {
                    val loaded = requireNotNull(catalog)
                    if (cartOnly) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                            CustomerCartReview(
                                listing = listing,
                                catalog = loaded,
                                selectedPackageId = selectedPackageId,
                                quantities = quantities,
                                useListingPrice = useListingPrice,
                                presentation = loaded.customerPresentationOrDefault(),
                                onRemovePackage = {
                                    selectedPackageId = null
                                    updateCart(nextPackageId = null)
                                },
                                onChangeQuantity = { id, quantity ->
                                    if (quantity <= 0) quantities.remove(id) else quantities[id] = 1
                                    updateCart(nextQuantities = quantities.toMap())
                                },
                                onClear = {
                                    selectedPackageId = null
                                    quantities.clear()
                                    useListingPrice = false
                                    onCartChanged(null)
                                }
                            )
                            Spacer(Modifier.height(100.dp))
                        }
                    } else {
                        CustomerProviderStorefront(
                            listing = listing,
                            catalog = loaded,
                            presentation = loaded.customerPresentationOrDefault(),
                            selectedPackageId = selectedPackageId,
                            quantities = quantities,
                            useListingPrice = useListingPrice,
                            currentCart = currentCart,
                            onBack = onBack,
                            onOpenCart = {
                                if (currentCart == null) {
                                    scope.launch { snackbarHostState.showSnackbar("Your cart is empty. Add a package or service first.") }
                                } else {
                                    onOpenCart()
                                }
                            },
                            onResolveMedia = onResolveMedia,
                            onSelectPackage = { id ->
                                val next = if (selectedPackageId == id) null else id
                                selectedPackageId = next
                                useListingPrice = false
                                updateCart(nextPackageId = next, nextUseListingPrice = false)
                            },
                            onChangeQuantity = { id, quantity ->
                                if (quantity <= 0) quantities.remove(id) else quantities[id] = 1
                                useListingPrice = false
                                updateCart(nextQuantities = quantities.toMap(), nextUseListingPrice = false)
                            },
                            onSelectCustom = {
                                selectedPackageId = null
                                quantities.clear()
                                useListingPrice = true
                                updateCart(nextPackageId = null, nextQuantities = emptyMap(), nextUseListingPrice = true)
                            },
                            onAddPackageItems = { packageId, additions ->
                                val next = customerPackageItemSelection(loaded, quantities, additions)
                                quantities.clear()
                                quantities.putAll(next)
                                if (selectedPackageId == packageId) selectedPackageId = null
                                useListingPrice = false
                                updateCart(nextQuantities = next, nextUseListingPrice = false)
                            }
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CustomerProviderStorefront(
    listing: ServiceListing,
    catalog: ListingServiceCatalog,
    presentation: CustomerCatalogPresentation,
    selectedPackageId: String?,
    quantities: Map<String, Int>,
    useListingPrice: Boolean,
    currentCart: CustomerProviderCart?,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    onResolveMedia: suspend (String) -> String?,
    onSelectPackage: (String) -> Unit,
    onChangeQuantity: (String, Int) -> Unit,
    onSelectCustom: () -> Unit,
    onAddPackageItems: (String, Map<String, Int>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var query by remember(catalog.listingId) { mutableStateOf("") }
    var mode by remember(catalog.listingId) { mutableStateOf("ALL") }
    var affordableOnly by remember(catalog.listingId) { mutableStateOf(false) }
    var isSearchExpanded by remember(catalog.listingId) { mutableStateOf(false) }
    var showItemsNavigator by remember(catalog.listingId) { mutableStateOf(false) }
    var detailOffering by remember(catalog.listingId) { mutableStateOf<ProviderServiceOffering?>(null) }
    var detailPackage by remember(catalog.listingId) { mutableStateOf<ProviderServicePackage?>(null) }
    val isProperty = presentation.mode == "PROPERTY"
    val result = remember(catalog, listing, query, mode, affordableOnly, isProperty) {
        customerCatalogSearch(
            catalog = catalog,
            query = query,
            mode = mode,
            maximumPrice = if (affordableOnly && !isProperty) 500.0 else null,
            providerContext = "${listing.title} ${listing.serviceType} ${listing.providerName} ${listing.location}"
        )
    }
    val blocks = remember(result, isProperty) {
        buildList<StorefrontBlock> {
            if (result.packages.isNotEmpty()) {
                add(StorefrontBlock.Section("section-packages", if (isProperty) "Property collections" else "Packages", result.packages.size, if (isProperty) "Property options selected by the provider" else "Complete combinations selected by the provider"))
                result.packages.forEach { add(StorefrontBlock.Package("package-${it.id}", it)) }
            }
            result.offeringGroups.forEach { (group, offers) ->
                add(StorefrontBlock.Section("section-$group", group, offers.size, if (isProperty) "Choose the property you want to enquire about" else "Choose only the work you need"))
                offers.forEach { add(StorefrontBlock.Offering("offering-${it.id}", it)) }
            }
            if (query.isBlank() && mode != "PACKAGES" && !affordableOnly) add(StorefrontBlock.CustomService)
        }
    }
    val sectionDestinations = remember(blocks) {
        blocks.mapIndexedNotNull { index, block ->
            (block as? StorefrontBlock.Section)?.let { it to (index + 3) }
        }
    }
    val currentSection by remember(listState, sectionDestinations) {
        derivedStateOf {
            val visibleIndex = listState.firstVisibleItemIndex
            sectionDestinations.lastOrNull { (_, index) -> index <= visibleIndex }?.first
                ?: sectionDestinations.firstOrNull()?.first
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "provider-hero-info") {
                StorefrontUrbanCompanyHero(
                    listing = listing,
                    managedMedia = catalog.listingGallery.orEmpty().ifEmpty { listOfNotNull(catalog.listingMedia) },
                    onResolveMedia = onResolveMedia
                )
            }
            item(key = "provider-categories") {
                StorefrontCategoryGrid(
                    packages = result.packages,
                    offeringGroups = result.offeringGroups,
                    fallbackPhotoUrl = listing.photoUrl,
                    onResolveMedia = onResolveMedia,
                    heading = if (isProperty) "Explore properties" else "Explore services",
                    propertyMode = isProperty
                ) { sectionKey ->
                    val index = sectionDestinations.firstOrNull { it.first.key == sectionKey }?.second
                    if (index != null) scope.launch { listState.animateScrollToItem(index) }
                }
            }
            item(key = "catalog-filters") {
                StorefrontSearchPanel(
                    mode = mode,
                    onModeChange = { mode = it },
                    affordableOnly = affordableOnly,
                    onAffordableChange = { affordableOnly = it },
                    showPriceFilter = !isProperty,
                    propertyMode = isProperty,
                    currentSection = currentSection
                )
            }
            if (blocks.isEmpty()) {
                item(key = "empty-results") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8A9891), modifier = Modifier.size(34.dp))
                        Text(if (isProperty) "No matching properties" else "No matching services", fontWeight = FontWeight.ExtraBold)
                        Text(if (isProperty) "Try another property type or clear a filter." else "Try another service name or clear a filter.", color = Color(0xFF60756B), textAlign = TextAlign.Center)
                        TextButton(onClick = { query = ""; mode = "ALL"; affordableOnly = false }) { Text("Clear filters", color = NestoraMint) }
                    }
                }
            }
            blocks.forEach { block ->
                when (block) {
                    is StorefrontBlock.Section -> item(key = block.key) { StorefrontSectionHeader(block) }
                    is StorefrontBlock.Package -> item(key = block.key) {
                        StorefrontPackageCard(
                            pack = block.value,
                            selected = selectedPackageId == block.value.id && !useListingPrice,
                            photoUrl = block.value.items.firstNotNullOfOrNull(::offeringPhotoUrl) ?: listing.photoUrl,
                            media = block.value.media,
                            onResolveMedia = onResolveMedia,
                            showDuration = presentation.showDuration,
                            propertyMode = isProperty,
                            onToggle = { onSelectPackage(block.value.id) },
                            onDetails = { detailPackage = block.value }
                        )
                        StorefrontItemDivider()
                    }
                    is StorefrontBlock.Offering -> item(key = block.key) {
                        StorefrontOfferingRow(
                            offer = block.value,
                            photoUrl = offeringPhotoUrl(block.value) ?: listing.photoUrl,
                            media = block.value.media,
                            onResolveMedia = onResolveMedia,
                            showDuration = presentation.showDuration,
                            quantity = quantities[block.value.id] ?: 0,
                            onToggle = { onChangeQuantity(block.value.id, if ((quantities[block.value.id] ?: 0) > 0) 0 else 1) },
                            onDetails = { detailOffering = block.value }
                        )
                        StorefrontItemDivider()
                    }
                    StorefrontBlock.CustomService -> item(key = block.key) {
                        StorefrontCustomService(listing = listing, selected = useListingPrice, propertyMode = isProperty, onSelect = onSelectCustom)
                    }
                }
            }
            item(key = "catalog-end-space") { Spacer(Modifier.height(96.dp)) }
        }

        if (sectionDestinations.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showItemsNavigator = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp),
                containerColor = Color(0xFF07100D),
                contentColor = Color.White,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 14.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(18.dp))
                    if (isProperty) Text("OPTIONS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    else Text("ITEMS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        StorefrontUrbanCompanyTopBar(
            title = listing.title,
            subtitle = "by ${listing.providerName}",
            isScrolled = isScrolled,
            isSearchExpanded = isSearchExpanded,
            searchQuery = query,
            cartItemCount = currentCart?.itemCount ?: 0,
            onBack = onBack,
            onSearch = { isSearchExpanded = true },
            onSearchQueryChange = { query = it.take(80) },
            onCloseSearch = {
                focusManager.clearFocus()
                query = ""
                isSearchExpanded = false
            },
            onShare = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${listing.title} by ${listing.providerName} on Nestora")
                }
                context.startActivity(Intent.createChooser(share, "Share provider"))
            },
            onCart = onOpenCart
        )
    }

    detailOffering?.let { offer ->
        CustomerCatalogExperienceDrawer(
            listing = listing, catalog = catalog, offering = offer,
            quantity = quantities[offer.id] ?: 0, onResolveMedia = onResolveMedia,
            showDuration = presentation.showDuration,
            propertyMode = isProperty,
            onDismiss = { detailOffering = null },
            onOfferingQuantity = { onChangeQuantity(offer.id, it) }
        )
    }
    detailPackage?.let { pack ->
        CustomerCatalogExperienceDrawer(
            listing = listing, catalog = catalog, pack = pack,
            existingQuantities = quantities,
            packageSelected = selectedPackageId == pack.id, onResolveMedia = onResolveMedia,
            showDuration = presentation.showDuration,
            propertyMode = isProperty,
            onDismiss = { detailPackage = null },
            onCompletePackage = { onSelectPackage(pack.id) },
            onPackageItems = { onAddPackageItems(pack.id, it) }
        )
    }
    if (showItemsNavigator) {
        StorefrontItemsNavigator(
            sections = sectionDestinations.map { it.first },
            selectedKey = currentSection?.key,
            onDismiss = { showItemsNavigator = false },
            onSelect = { selected ->
                val index = sectionDestinations.firstOrNull { it.first.key == selected.key }?.second
                showItemsNavigator = false
                if (index != null) scope.launch { listState.animateScrollToItem(index) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StorefrontUrbanCompanyHero(
    listing: ServiceListing,
    managedMedia: List<MediaAsset> = emptyList(),
    onResolveMedia: suspend (String) -> String? = { null }
) {
    val carousel = remember(managedMedia, listing.photoUrl) { managedMedia.distinctBy { it.id } }
    val pageCount = carousel.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Box(modifier = Modifier.fillMaxWidth().height(270.dp)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val asset = carousel.getOrNull(page)
                StorefrontCarouselImage(
                    media = asset,
                    fallbackPhotoUrl = listing.photoUrl,
                    title = listing.title,
                    onResolveMedia = onResolveMedia
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).height(110.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent)))
            )
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(110.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))))
            )
            carousel.getOrNull(pagerState.currentPage)?.let { asset ->
                if (asset.title.isNotBlank() || asset.subtitle.isNotBlank()) {
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 80.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (asset.title.isNotBlank()) Text(asset.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        if (asset.subtitle.isNotBlank()) Text(asset.subtitle, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (carousel.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                    color = Color.Black.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(carousel.size) { pageIndex ->
                            val isSelected = pagerState.currentPage == pageIndex
                            val animatedWidth by animateDpAsState(
                                targetValue = if (isSelected) 18.dp else 6.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "carousel_dot_width"
                            )
                            val animatedColor by animateColorAsState(
                                targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f),
                                animationSpec = tween(durationMillis = 250),
                                label = "carousel_dot_color"
                            )
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(animatedWidth)
                                    .clip(CircleShape)
                                    .background(animatedColor)
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(thickness = 8.dp, color = Color(0xFFF3F5F4))
    }
}

@Composable
internal fun StorefrontCarouselImage(
    media: MediaAsset?,
    fallbackPhotoUrl: String?,
    title: String,
    onResolveMedia: suspend (String) -> String?
) {
    val fileId = remember(media?.id, fallbackPhotoUrl) {
        media?.fileIdFor("HERO") ?: fallbackPhotoUrl?.takeIf { !it.startsWith("http") && !it.startsWith("/") && !it.startsWith("file:") }
    }
    var resolvedPath by remember(fileId) { mutableStateOf<String?>(null) }
    LaunchedEffect(fileId) { resolvedPath = fileId?.let { onResolveMedia(it) } }
    val image = resolvedPath ?: fallbackPhotoUrl?.takeIf(String::isNotBlank)
    if (image != null) {
        AsyncImage(model = image, contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.fillMaxSize().background(Color(0xFFE6ECE9)), contentAlignment = Alignment.Center) {
            Text(title.take(2).uppercase(Locale.getDefault()), color = Color(0xFF2D6551), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
@Composable
private fun StorefrontCategoryGrid(
    packages: List<ProviderServicePackage>,
    offeringGroups: Map<String, List<ProviderServiceOffering>>,
    fallbackPhotoUrl: String?,
    onResolveMedia: suspend (String) -> String?,
    heading: String,
    propertyMode: Boolean,
    onCategoryClick: (String) -> Unit
) {
    val shortcuts = remember(packages, offeringGroups, propertyMode) {
        buildList {
            if (packages.isNotEmpty()) {
                val packageMedia = packages.firstNotNullOfOrNull { pack ->
                    pack.mediaGallery.orEmpty().firstOrNull() ?: pack.media ?: pack.items.firstNotNullOfOrNull { it.mediaGallery.orEmpty().firstOrNull() ?: it.media }
                }
                add(StorefrontShortcut("section-packages", if (propertyMode) "Collections" else "Packages", packageMedia))
            }
            offeringGroups.forEach { (group, offers) ->
                add(StorefrontShortcut("section-$group", group, offers.firstNotNullOfOrNull { it.mediaGallery.orEmpty().firstOrNull() ?: it.media }))
            }
        }
    }
    if (shortcuts.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(top = 16.dp)) {
        Text(
            heading,
            modifier = Modifier.padding(horizontal = 18.dp),
            color = Color(0xFF111A16),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shortcuts, key = { it.sectionKey }) { shortcut ->
                Column(
                    modifier = Modifier.width(82.dp).clickable { onCategoryClick(shortcut.sectionKey) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    StorefrontShortcutImage(
                        shortcut = shortcut,
                        fallbackPhotoUrl = fallbackPhotoUrl,
                        onResolveMedia = onResolveMedia
                    )
                    Text(
                        shortcut.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        lineHeight = 14.sp,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF26332D)
                    )
                }
            }
        }
            HorizontalDivider(modifier = Modifier.padding(top = 18.dp), thickness = 8.dp, color = Color(0xFFF3F5F4))
        }
    }
}

private data class StorefrontShortcut(val sectionKey: String, val label: String, val media: MediaAsset?)

@Composable
private fun StorefrontShortcutImage(
    shortcut: StorefrontShortcut,
    fallbackPhotoUrl: String?,
    onResolveMedia: suspend (String) -> String?
) {
    val fileId = remember(shortcut.media?.id) { shortcut.media?.fileIdFor("CARD") }
    var resolvedPath by remember(fileId) { mutableStateOf<String?>(null) }
    LaunchedEffect(fileId) { resolvedPath = fileId?.let { onResolveMedia(it) } }
    val image = resolvedPath ?: fallbackPhotoUrl?.takeIf(String::isNotBlank)
    Surface(modifier = Modifier.size(82.dp), shape = RoundedCornerShape(14.dp), color = Color(0xFFF0F3F1)) {
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = shortcut.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    shortcut.label.take(2).uppercase(Locale.getDefault()),
                    color = NestoraMint,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StorefrontUrbanCompanyTopBar(
    title: String,
    subtitle: String,
    isScrolled: Boolean,
    isSearchExpanded: Boolean = false,
    searchQuery: String = "",
    cartItemCount: Int = 0,
    onBack: () -> Unit,
    onSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onCloseSearch: () -> Unit = {},
    onShare: () -> Unit = {},
    onCart: () -> Unit = {},
    showSearch: Boolean = true,
    showShare: Boolean = true,
    showCart: Boolean = true
) {
    val searchFocusRequester = remember { FocusRequester() }
    val backgroundColor by animateColorAsState(if (isScrolled || isSearchExpanded) Color.White else Color.Transparent, label = "bg")
    val dividerColor by animateColorAsState(if (isScrolled || isSearchExpanded) Color(0xFFE3E8E5) else Color.Transparent, label = "divider")

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) searchFocusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorefrontHeaderAction(onClick = onBack, contentDescription = "Back") {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF14201B), modifier = Modifier.size(20.dp))
                }
                if (isSearchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .padding(start = 8.dp)
                            .focusRequester(searchFocusRequester),
                        placeholder = { Text("Search services", fontSize = 13.sp) },
                        trailingIcon = {
                            IconButton(onClick = onCloseSearch) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F7F6),
                            unfocusedContainerColor = Color(0xFFF5F7F6),
                            focusedIndicatorColor = NestoraMint,
                            unfocusedIndicatorColor = Color(0xFFDCE3E0)
                        )
                    )
                } else {
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isScrolled) Color(0xFF101814) else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        subtitle.takeIf(String::isNotBlank)?.let {
                            Text(
                                it,
                                color = if (isScrolled) Color(0xFF66736D) else Color.White.copy(alpha = 0.92f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (!isSearchExpanded) {
                    if (showSearch) {
                        StorefrontHeaderAction(onClick = onSearch, contentDescription = "Search") {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF14201B), modifier = Modifier.size(20.dp))
                        }
                    }
                    if (showShare) {
                        if (showSearch) Spacer(Modifier.width(7.dp))
                        StorefrontHeaderAction(onClick = onShare, contentDescription = "Share") {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF14201B), modifier = Modifier.size(19.dp))
                        }
                    }
                    if (showCart && cartItemCount > 0) {
                        if (showSearch || showShare) Spacer(Modifier.width(7.dp))
                        StorefrontHeaderAction(onClick = onCart, contentDescription = "Cart") {
                            BadgedBox(badge = { Badge { Text(cartItemCount.toString()) } }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF14201B), modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = dividerColor)
        }
    }
}

@Composable
internal fun StorefrontHeaderAction(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.size(38.dp).semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}
@Composable
private fun StorefrontSearchPanel(
    mode: String,
    onModeChange: (String) -> Unit,
    affordableOnly: Boolean,
    onAffordableChange: (Boolean) -> Unit,
    showPriceFilter: Boolean,
    propertyMode: Boolean,
    currentSection: StorefrontBlock.Section?
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (if (propertyMode) listOf("ALL" to "All", "PACKAGES" to "Collections", "SERVICES" to "Properties")
                else listOf("ALL" to "All", "PACKAGES" to "Packages", "SERVICES" to "Services")).forEach { (value, label) ->
                    FilterChip(selected = mode == value, onClick = { onModeChange(value) }, label = { Text(label) })
                }
                if (showPriceFilter) {
                    FilterChip(selected = affordableOnly, onClick = { onAffordableChange(!affordableOnly) }, label = { Text("Under ₹500") })
                }
            }
        }
        HorizontalDivider(color = Color(0xFFE4E9E6))
        currentSection?.let { section ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(section.title, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Text("${section.count} available", color = Color(0xFF60756B), fontSize = 12.sp)
            }
            HorizontalDivider(color = Color(0xFFE4E9E6))
        }
    }
}

@Composable
private fun StorefrontSectionHeader(section: StorefrontBlock.Section) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("${section.title} (${section.count})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        if (section.subtitle.isNotBlank()) Text(section.subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B))
    }
}

private fun offeringPhotoUrl(offer: ProviderServiceOffering): String? {
    val attributes = offer.attributeValues ?: return null
    return listOf("photo_url", "image_url", "media_url").firstNotNullOfOrNull { key ->
        attributes.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf(String::isNotBlank)
    }
}

@Composable
private fun StorefrontItemDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFE8ECEA))
}

@Composable
private fun StorefrontCustomService(listing: ServiceListing, selected: Boolean, propertyMode: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable(onClick = onSelect),
        color = if (selected) Color(0xFFE8F6F1) else Color(0xFFF8FAF9)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (propertyMode) "Looking for another property?" else "Need something different?", fontWeight = FontWeight.ExtraBold)
                Text(
                    if (propertyMode) "Describe the property you need. Price and terms are agreed directly with the provider."
                    else "Describe custom work from ₹${listing.price.toInt()}. The provider confirms the final scope.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF60756B)
                )
            }
            OutlinedButton(onClick = onSelect, border = BorderStroke(1.dp, NestoraMint)) {
                Text(if (selected) "Selected" else "Select", color = NestoraMint, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StorefrontItemsNavigator(
    sections: List<StorefrontBlock.Section>,
    selectedKey: String?,
    onDismiss: () -> Unit,
    onSelect: (StorefrontBlock.Section) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 340.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF080D0B),
            shadowElevation = 18.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Browse services", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                }
                sections.forEach { section ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(section) }.padding(horizontal = 20.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(section.title, modifier = Modifier.weight(1f), color = Color.White, fontWeight = if (section.key == selectedKey) FontWeight.ExtraBold else FontWeight.Medium)
                        Text(section.count.toString(), color = if (section.key == selectedKey) Color(0xFF6BE5B4) else Color(0xFFC6CFCA), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StorefrontPackageCard(
    pack: ProviderServicePackage,
    selected: Boolean,
    photoUrl: String?,
    media: MediaAsset?,
    onResolveMedia: suspend (String) -> String?,
    showDuration: Boolean,
    propertyMode: Boolean,
    onToggle: () -> Unit,
    onDetails: () -> Unit
) {
    val savings = providerPackageSavings(pack)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onDetails).background(if (selected) Color(0xFFF0FAF6) else Color.White).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(color = Color(0xFFE8F6F1), shape = RoundedCornerShape(5.dp)) {
                Text(if (propertyMode) "COLLECTION" else "PACKAGE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color(0xFF146B4A), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(pack.name, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("₹${pack.packagePriceAmount.toInt()}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF17221D))
            if (pack.description.isNotBlank()) {
                Text(pack.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (showDuration) "${pack.durationMinutes} min · ${pack.items.size} services" else "${pack.items.size} property option(s)",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF486158)
            )
            Text(if (propertyMode) "Contains ${providerPackageItemsLabel(pack)}" else "Includes ${providerPackageItemsLabel(pack)}", style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (savings > 0 && !propertyMode) {
                Text("Save ₹${savings.toInt()}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF146B4A), fontWeight = FontWeight.Bold)
            }
        }
        StorefrontAddVisual(
            photoUrl = photoUrl,
            media = media,
            onResolveMedia = onResolveMedia,
            title = pack.name,
            selected = selected,
            onAdd = onToggle
        )
    }
}

@Composable
private fun StorefrontOfferingRow(
    offer: ProviderServiceOffering,
    photoUrl: String? = null,
    media: MediaAsset? = null,
    onResolveMedia: suspend (String) -> String? = { null },
    showDuration: Boolean = true,
    showPrice: Boolean = true,
    quantity: Int,
    onToggle: () -> Unit,
    onDetails: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onDetails != null) Modifier.clickable(onClick = onDetails) else Modifier).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(offer.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            providerOfferingCustomerDetails(offer).forEach { detail ->
                Text(detail, style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (showPrice) {
                Text(
                    if (showDuration) "₹${offer.priceAmount.toInt()} · ${offer.durationMinutes} min" else "₹${offer.priceAmount.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        StorefrontAddVisual(
            photoUrl = photoUrl,
            media = media,
            onResolveMedia = onResolveMedia,
            title = offer.title,
            selected = quantity > 0,
            onAdd = onToggle
        )
    }
}

@Composable
private fun StorefrontAddVisual(
    photoUrl: String?,
    media: MediaAsset?,
    onResolveMedia: suspend (String) -> String?,
    title: String,
    selected: Boolean,
    onAdd: () -> Unit
) {
    val mediaFileId = remember(media?.id) { media?.fileIdFor("CARD") }
    var resolvedMedia by remember(mediaFileId) { mutableStateOf<String?>(null) }
    LaunchedEffect(mediaFileId) {
        resolvedMedia = mediaFileId?.let { onResolveMedia(it) }
    }
    val imageModel = resolvedMedia ?: photoUrl
    Box(modifier = Modifier.width(132.dp).height(132.dp), contentAlignment = Alignment.TopCenter) {
        if (!imageModel.isNullOrBlank()) {
            AsyncImage(
                model = imageModel,
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE7F1ED)),
                contentAlignment = Alignment.Center
            ) {
                Text(title.take(2).uppercase(Locale.getDefault()), color = Color(0xFF28624D), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
        }
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomCenter).width(112.dp).height(44.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, NestoraMint),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
        ) { Text(if (selected) "REMOVE" else "ADD", color = NestoraMint, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
private fun QuantityControl(quantity: Int, onQuantityChange: (Int) -> Unit) {
    if (quantity <= 0) {
        OutlinedButton(
            onClick = { onQuantityChange(1) },
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 7.dp),
            border = BorderStroke(1.dp, NestoraMint)
        ) { Text("Add", color = NestoraMint, fontWeight = FontWeight.Bold) }
    } else {
        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFE8F6F1), border = BorderStroke(1.dp, NestoraMint)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onQuantityChange(quantity - 1) }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text("−", color = NestoraMint, style = MaterialTheme.typography.titleMedium)
                }
                Text(quantity.toString(), modifier = Modifier.width(20.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { onQuantityChange((quantity + 1).coerceAtMost(10)) },
                    enabled = quantity < 10,
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) { Text("+", color = NestoraMint, style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun CustomerCartReview(
    listing: ServiceListing,
    catalog: ListingServiceCatalog,
    presentation: CustomerCatalogPresentation,
    selectedPackageId: String?,
    quantities: Map<String, Int>,
    useListingPrice: Boolean,
    onRemovePackage: () -> Unit,
    onChangeQuantity: (String, Int) -> Unit,
    onClear: () -> Unit
) {
    val propertyCheckout = presentation.checkoutAmountMode == "PLATFORM_FEE_ONLY"
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Text(if (propertyCheckout) "Review selected properties" else "Review selected services", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        selectedPackageId?.let { id ->
            catalog.packages.firstOrNull { it.id == id }?.let { pack ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(pack.name, fontWeight = FontWeight.Bold)
                        Text(if (propertyCheckout) "Selected property collection" else "Complete package · ₹${pack.packagePriceAmount.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B))
                    }
                    TextButton(onClick = onRemovePackage) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = Color(0xFFE5E9E7))
            }
        }
        quantities.forEach { (id, quantity) ->
            catalog.offerings.firstOrNull { it.id == id }?.let { offer ->
                StorefrontOfferingRow(
                    offer = offer,
                    showDuration = presentation.showDuration,
                    showPrice = !propertyCheckout,
                    quantity = quantity,
                    onToggle = { onChangeQuantity(id, 0) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = Color(0xFFE5E9E7))
            }
        }
        if (useListingPrice) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (propertyCheckout) "Custom property enquiry" else "Custom service request", fontWeight = FontWeight.Bold)
                if (!propertyCheckout) Text("From ₹${listing.price.toInt()}", fontWeight = FontWeight.Bold)
            }
        }
        if (selectedPackageId == null && quantities.isEmpty() && !useListingPrice) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF98A39E), modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(10.dp))
                Text("Your cart is empty", fontWeight = FontWeight.Bold)
                Text("Go back and add a package or individual service.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B), textAlign = TextAlign.Center)
            }
        } else {
            if (propertyCheckout) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0F8F4)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Nestora platform fee · ₹${presentation.platformFeeAmount.toInt()}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF14513D))
                        Text("Property price, rent, deposit, and brokerage are agreed directly with the provider and are not charged by Nestora.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B))
                    }
                }
            }
            TextButton(onClick = onClear, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text("Clear cart", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StorefrontCartBar(
    cart: CustomerProviderCart?,
    cartOnly: Boolean,
    presentation: CustomerCatalogPresentation?,
    onOpenCart: () -> Unit
) {
    val propertyCheckout = presentation?.checkoutAmountMode == "PLATFORM_FEE_ONLY"
    val platformFeeLabel = presentation?.platformFeeAmount?.takeIf { it > 0.0 }
        ?.let { "Nestora platform fee · ₹${it.toInt()}" }
        ?: "Nestora platform fee shown at confirmation"
    Surface(color = Color.White, shadowElevation = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = NestoraMint)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        cart == null -> "Cart is empty"
                        propertyCheckout -> platformFeeLabel
                        else -> "${cart.itemCount.coerceAtLeast(1)} item(s) · ₹${cart.providerAmount.toInt()}"
                    },
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    when {
                        cart == null -> "Add services to continue"
                        propertyCheckout -> "Property amount is not charged by Nestora"
                        else -> "Provider amount paid after work"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF60756B)
                )
            }
            Button(
                onClick = onOpenCart,
                enabled = cart != null,
                colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
            ) { Text(if (cartOnly) if (propertyCheckout) "Proceed" else "Choose slot" else "View cart", fontWeight = FontWeight.Bold) }
        }
    }
}
