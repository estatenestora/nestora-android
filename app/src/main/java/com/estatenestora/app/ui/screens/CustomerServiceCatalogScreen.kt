package com.estatenestora.app.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.AndroidBridgeResponse
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
    val summary = customerServiceCartSummary(
        catalog = catalog,
        packageId = packageId,
        offeringQuantities = offeringQuantities,
        useListingPrice = useListingPrice,
        listingPrice = listing.price,
        defaultDurationMinutes = defaultDurationMinutes
    ) ?: return null
    return CustomerProviderCart(
        listing = listing,
        providerId = catalog.providerId,
        serviceTypeId = catalog.serviceTypeId,
        packageId = packageId,
        offeringQuantities = offeringQuantities.toMap(),
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
                    .forEach { (id, quantity) -> quantities[id] = quantity.coerceIn(1, 10) }
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
            TopAppBar(
                title = { Text(if (cartOnly) "Your cart" else "Provider services", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (!cartOnly) {
                        IconButton(onClick = {
                            if (currentCart == null) {
                                scope.launch { snackbarHostState.showSnackbar("Your cart is empty. Add a package or service first.") }
                            } else {
                                onOpenCart()
                            }
                        }) {
                            BadgedBox(
                                badge = {
                                    currentCart?.let { cart ->
                                        Badge { Text(cart.itemCount.coerceAtLeast(1).toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Open cart", tint = NestoraMint)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
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
        containerColor = Color(0xFFF6F8F7)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                    StorefrontProviderSummary(listing)
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NestoraMint)
                    }
                }
                error != null -> Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                    StorefrontProviderSummary(listing)
                    HorizontalDivider(thickness = 8.dp, color = Color(0xFFEFF2F1))
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
                catalog != null && !switchConflict -> {
                    val loaded = catalog ?: return@Box
                    if (cartOnly) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                            StorefrontProviderSummary(listing)
                            HorizontalDivider(thickness = 8.dp, color = Color(0xFFEFF2F1))
                            CustomerCartReview(
                            listing = listing,
                            catalog = loaded,
                            selectedPackageId = selectedPackageId,
                            quantities = quantities,
                            useListingPrice = useListingPrice,
                            onRemovePackage = {
                                selectedPackageId = null
                                updateCart(nextPackageId = null)
                            },
                            onChangeQuantity = { id, quantity ->
                                if (quantity <= 0) quantities.remove(id) else quantities[id] = quantity.coerceAtMost(10)
                                updateCart(nextQuantities = quantities.toMap())
                            },
                            onClear = {
                                selectedPackageId = null
                                quantities.clear()
                                useListingPrice = false
                                onCartChanged(null)
                            }
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    } else {
                        CustomerProviderStorefront(
                            listing = listing,
                            catalog = loaded,
                            selectedPackageId = selectedPackageId,
                            quantities = quantities,
                            useListingPrice = useListingPrice,
                            onResolveMedia = onResolveMedia,
                            onSelectPackage = { id ->
                                val next = if (selectedPackageId == id) null else id
                                selectedPackageId = next
                                useListingPrice = false
                                updateCart(nextPackageId = next, nextUseListingPrice = false)
                            },
                            onChangeQuantity = { id, quantity ->
                                if (quantity <= 0) quantities.remove(id) else quantities[id] = quantity.coerceAtMost(10)
                                useListingPrice = false
                                updateCart(nextQuantities = quantities.toMap(), nextUseListingPrice = false)
                            },
                            onSelectCustom = {
                                selectedPackageId = null
                                quantities.clear()
                                useListingPrice = true
                                updateCart(nextPackageId = null, nextQuantities = emptyMap(), nextUseListingPrice = true)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomerProviderStorefront(
    listing: ServiceListing,
    catalog: ListingServiceCatalog,
    selectedPackageId: String?,
    quantities: Map<String, Int>,
    useListingPrice: Boolean,
    onResolveMedia: suspend (String) -> String?,
    onSelectPackage: (String) -> Unit,
    onChangeQuantity: (String, Int) -> Unit,
    onSelectCustom: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var query by remember(catalog.listingId) { mutableStateOf("") }
    var mode by remember(catalog.listingId) { mutableStateOf("ALL") }
    var affordableOnly by remember(catalog.listingId) { mutableStateOf(false) }
    var showItemsNavigator by remember(catalog.listingId) { mutableStateOf(false) }
    val result = remember(catalog, listing, query, mode, affordableOnly) {
        customerCatalogSearch(
            catalog = catalog,
            query = query,
            mode = mode,
            maximumPrice = if (affordableOnly) 500.0 else null,
            providerContext = "${listing.title} ${listing.serviceType} ${listing.providerName} ${listing.location}"
        )
    }
    val blocks = remember(result) {
        buildList<StorefrontBlock> {
            if (result.packages.isNotEmpty()) {
                add(StorefrontBlock.Section("section-packages", "Packages", result.packages.size, "Complete combinations selected by the provider"))
                result.packages.forEach { add(StorefrontBlock.Package("package-${it.id}", it)) }
            }
            result.offeringGroups.forEach { (group, offers) ->
                add(StorefrontBlock.Section("section-$group", group, offers.size, "Choose only the work you need"))
                offers.forEach { add(StorefrontBlock.Offering("offering-${it.id}", it)) }
            }
            if (query.isBlank() && mode != "PACKAGES" && !affordableOnly) add(StorefrontBlock.CustomService)
        }
    }
    val sectionDestinations = remember(blocks) {
        blocks.mapIndexedNotNull { index, block ->
            (block as? StorefrontBlock.Section)?.let { it to (index + 2) }
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
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "provider-summary") { StorefrontProviderSummary(listing, catalog.listingMedia, onResolveMedia) }
            stickyHeader(key = "catalog-search") {
                StorefrontSearchPanel(
                    query = query,
                    onQueryChange = { query = it },
                    mode = mode,
                    onModeChange = { mode = it },
                    affordableOnly = affordableOnly,
                    onAffordableChange = { affordableOnly = it },
                    currentSection = currentSection,
                    onSearchDone = { focusManager.clearFocus() }
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
                        Text("No matching services", fontWeight = FontWeight.ExtraBold)
                        Text("Try another service name or clear a filter.", color = Color(0xFF60756B), textAlign = TextAlign.Center)
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
                            onToggle = { onSelectPackage(block.value.id) }
                        )
                        StorefrontItemDivider()
                    }
                    is StorefrontBlock.Offering -> item(key = block.key) {
                        StorefrontOfferingRow(
                            offer = block.value,
                            photoUrl = offeringPhotoUrl(block.value) ?: listing.photoUrl,
                            media = block.value.media,
                            onResolveMedia = onResolveMedia,
                            quantity = quantities[block.value.id] ?: 0,
                            onQuantityChange = { onChangeQuantity(block.value.id, it) }
                        )
                        StorefrontItemDivider()
                    }
                    StorefrontBlock.CustomService -> item(key = block.key) {
                        StorefrontCustomService(listing = listing, selected = useListingPrice, onSelect = onSelectCustom)
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
                    Text("ITEMS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
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

@Composable
private fun StorefrontProviderSummary(
    listing: ServiceListing,
    managedMedia: MediaAsset? = null,
    onResolveMedia: suspend (String) -> String? = { null }
) {
    val fileId = remember(managedMedia?.id) { managedMedia?.fileIdFor("HERO") }
    var managedPath by remember(fileId) { mutableStateOf<String?>(null) }
    LaunchedEffect(fileId) { managedPath = fileId?.let { onResolveMedia(it) } }
    val heroImage = managedPath ?: listing.photoUrl?.takeIf(String::isNotBlank)
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF07100D)).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        heroImage?.let { photo ->
            AsyncImage(
                model = photo,
                contentDescription = listing.title,
                modifier = Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 5.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(listing.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111A16))
                        Text("by ${listing.providerName}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF52675E))
                    }
                    if (listing.rating > 0f) {
                        Surface(color = Color(0xFF146B4A), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                "${String.format(Locale.US, "%.1f", listing.rating)} rating",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Text(listing.location, style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B), maxLines = 2, overflow = TextOverflow.Ellipsis)
                HorizontalDivider(color = Color(0xFFE7EBE9))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (listing.isVerified) "Verified provider" else "Service provider", color = Color(0xFF146B4A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Services from ₹${listing.price.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StorefrontSearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    mode: String,
    onModeChange: (String) -> Unit,
    affordableOnly: Boolean,
    onAffordableChange: (Boolean) -> Unit,
    currentSection: StorefrontBlock.Section?,
    onSearchDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { onQueryChange(it.take(80)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search this provider's services") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { if (query.isBlank()) onSearchDone() else onQueryChange("") }) {
                        Icon(if (query.isBlank()) Icons.Default.Search else Icons.Default.Close, contentDescription = if (query.isBlank()) "Search" else "Clear search")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F7F6),
                    unfocusedContainerColor = Color(0xFFF5F7F6),
                    focusedIndicatorColor = NestoraMint,
                    unfocusedIndicatorColor = Color(0xFFDCE3E0)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "All", "PACKAGES" to "Packages", "SERVICES" to "Services").forEach { (value, label) ->
                    FilterChip(selected = mode == value, onClick = { onModeChange(value) }, label = { Text(label) })
                }
                FilterChip(selected = affordableOnly, onClick = { onAffordableChange(!affordableOnly) }, label = { Text("Under ₹500") })
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
private fun StorefrontCustomService(listing: ServiceListing, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable(onClick = onSelect),
        color = if (selected) Color(0xFFE8F6F1) else Color(0xFFF8FAF9)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Need something different?", fontWeight = FontWeight.ExtraBold)
                Text("Describe custom work from ₹${listing.price.toInt()}. The provider confirms the final scope.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B))
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
    onToggle: () -> Unit
) {
    val savings = providerPackageSavings(pack)
    Row(
        modifier = Modifier.fillMaxWidth().background(if (selected) Color(0xFFF0FAF6) else Color.White).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(color = Color(0xFFE8F6F1), shape = RoundedCornerShape(5.dp)) {
                Text("PACKAGE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color(0xFF146B4A), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(pack.name, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("₹${pack.packagePriceAmount.toInt()}", fontWeight = FontWeight.ExtraBold, color = Color(0xFF17221D))
            if (pack.description.isNotBlank()) {
                Text(pack.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${pack.durationMinutes} min · ${pack.items.sumOf { it.quantity.coerceAtLeast(1) }} services",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF486158)
            )
            Text("Includes ${providerPackageItemsLabel(pack)}", style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (savings > 0) {
                Text("Save ₹${savings.toInt()}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF146B4A), fontWeight = FontWeight.Bold)
            }
        }
        StorefrontAddVisual(
            photoUrl = photoUrl,
            media = media,
            onResolveMedia = onResolveMedia,
            title = pack.name,
            selected = selected,
            quantity = if (selected) 1 else 0,
            onAdd = onToggle,
            onQuantityChange = { onToggle() },
            quantityEnabled = false
        )
    }
}

@Composable
private fun StorefrontOfferingRow(
    offer: ProviderServiceOffering,
    photoUrl: String? = null,
    media: MediaAsset? = null,
    onResolveMedia: suspend (String) -> String? = { null },
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(offer.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            providerOfferingCustomerDetails(offer).forEach { detail ->
                Text(detail, style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("₹${offer.priceAmount.toInt()} · ${offer.durationMinutes} min", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        StorefrontAddVisual(
            photoUrl = photoUrl,
            media = media,
            onResolveMedia = onResolveMedia,
            title = offer.title,
            selected = quantity > 0,
            quantity = quantity,
            onAdd = { onQuantityChange(1) },
            onQuantityChange = onQuantityChange,
            quantityEnabled = true
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
    quantity: Int,
    onAdd: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    quantityEnabled: Boolean
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
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            if (quantityEnabled && quantity > 0) {
                QuantityControl(quantity = quantity, onQuantityChange = onQuantityChange)
            } else {
                OutlinedButton(
                    onClick = onAdd,
                    modifier = Modifier.width(112.dp).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NestoraMint),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) { Text(if (selected) "REMOVE" else "ADD", color = NestoraMint, fontWeight = FontWeight.ExtraBold) }
            }
        }
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
    selectedPackageId: String?,
    quantities: Map<String, Int>,
    useListingPrice: Boolean,
    onRemovePackage: () -> Unit,
    onChangeQuantity: (String, Int) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Text("Review selected services", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        selectedPackageId?.let { id ->
            catalog.packages.firstOrNull { it.id == id }?.let { pack ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(pack.name, fontWeight = FontWeight.Bold)
                        Text("Complete package · ₹${pack.packagePriceAmount.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B))
                    }
                    TextButton(onClick = onRemovePackage) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = Color(0xFFE5E9E7))
            }
        }
        quantities.forEach { (id, quantity) ->
            catalog.offerings.firstOrNull { it.id == id }?.let { offer ->
                StorefrontOfferingRow(offer = offer, quantity = quantity, onQuantityChange = { onChangeQuantity(id, it) })
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = Color(0xFFE5E9E7))
            }
        }
        if (useListingPrice) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Custom service request", fontWeight = FontWeight.Bold)
                Text("From ₹${listing.price.toInt()}", fontWeight = FontWeight.Bold)
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
    onOpenCart: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = NestoraMint)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (cart == null) "Cart is empty" else "${cart.itemCount.coerceAtLeast(1)} item(s) · ₹${cart.providerAmount.toInt()}",
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (cart == null) "Add services to continue" else "Provider amount paid after work",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF60756B)
                )
            }
            Button(
                onClick = onOpenCart,
                enabled = cart != null,
                colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
            ) { Text(if (cartOnly) "Choose slot" else "View cart", fontWeight = FontWeight.Bold) }
        }
    }
}
