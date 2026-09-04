package com.estatenestora.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.estatenestora.app.data.model.BookingPolicy
import com.estatenestora.app.data.model.AvailabilitySlot
import com.estatenestora.app.data.model.ServiceListing
import com.estatenestora.app.data.model.ListingServiceCatalog
import com.estatenestora.app.data.model.ProviderServiceOffering
import com.estatenestora.app.data.model.ProviderServicePackage
import com.estatenestora.app.data.repository.NestoraRepository
import com.estatenestora.app.ui.theme.NestoraMint
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add

internal fun providerPackageItemsLabel(pack: ProviderServicePackage): String =
    pack.items.joinToString(", ") { offer ->
        val quantityPrefix = if (offer.quantity > 1) "${offer.quantity} x " else ""
        "$quantityPrefix${offer.title}"
    }

internal fun providerPackageItemsTotal(pack: ProviderServicePackage): Double =
    pack.items.sumOf { it.priceAmount * it.quantity.coerceAtLeast(1) }

internal fun providerPackageSavings(pack: ProviderServicePackage): Double =
    (providerPackageItemsTotal(pack) - pack.packagePriceAmount).coerceAtLeast(0.0)

internal data class CustomerServiceCartSummary(
    val kind: String,
    val title: String,
    val itemCount: Int,
    val providerAmount: Double,
    val durationMinutes: Int
)

/** Builds a provider-scoped cart. One package may be combined with individual
 * extras; custom requests remain exclusive. No provider/price data is
 * accepted from the phone because the backend resolves every opaque id. */
internal fun customerServiceSelectionPayload(
    packageId: String?,
    offeringQuantities: Map<String, Int>,
    useListingPrice: Boolean
): JsonObject? {
    val cleanPackageId = packageId?.trim().orEmpty()
    val cleanItems = offeringQuantities
        .filterKeys { it.isNotBlank() }
        .filterValues { it in 1..10 }
    val hasCatalogSelection = cleanPackageId.isNotBlank() || cleanItems.isNotEmpty()
    if (useListingPrice == hasCatalogSelection || cleanItems.size != offeringQuantities.size || cleanItems.size > 12) return null
    return JsonObject().apply {
        if (useListingPrice) {
            addProperty("use_listing_price", true)
        } else {
            if (cleanPackageId.isNotBlank()) addProperty("package_id", cleanPackageId)
            if (cleanItems.isNotEmpty()) add("items", JsonArray().apply {
                cleanItems.toSortedMap().forEach { (id, quantity) ->
                    add(JsonObject().apply {
                        addProperty("offering_id", id)
                        addProperty("quantity", quantity)
                    })
                }
            })
        }
    }
}

internal fun customerServiceCartSummary(
    catalog: ListingServiceCatalog,
    packageId: String?,
    offeringQuantities: Map<String, Int>,
    useListingPrice: Boolean,
    listingPrice: Double,
    defaultDurationMinutes: Int
): CustomerServiceCartSummary? {
    if (customerServiceSelectionPayload(packageId, offeringQuantities, useListingPrice) == null) return null
    if (useListingPrice) return CustomerServiceCartSummary(
        kind = "LISTING",
        title = "Custom service request",
        itemCount = 0,
        providerAmount = listingPrice.coerceAtLeast(0.0),
        durationMinutes = defaultDurationMinutes.coerceAtLeast(5)
    )
    val pack = packageId?.takeIf { it.isNotBlank() }?.let { id -> catalog.packages.firstOrNull { it.id == id } ?: return null }
    val selected = offeringQuantities.mapNotNull { (id, quantity) ->
        catalog.offerings.firstOrNull { it.id == id }?.let { it to quantity }
    }
    if (selected.size != offeringQuantities.size) return null
    if (pack != null) return CustomerServiceCartSummary(
        kind = if (selected.isEmpty()) "PACKAGE" else "MIXED",
        title = if (selected.isEmpty()) pack.name else "${pack.name} + ${selected.size} extra service(s)",
        itemCount = pack.items.sumOf { it.quantity.coerceAtLeast(1) } + selected.sumOf { it.second },
        providerAmount = pack.packagePriceAmount + selected.sumOf { (offer, quantity) -> offer.priceAmount * quantity },
        durationMinutes = pack.durationMinutes + selected.sumOf { (offer, quantity) -> offer.durationMinutes * quantity }
    )
    return CustomerServiceCartSummary(
        kind = "ITEMS",
        title = if (selected.size == 1) selected.first().first.title else "${selected.first().first.title} + ${selected.size - 1} more",
        itemCount = selected.sumOf { it.second },
        providerAmount = selected.sumOf { (offer, quantity) -> offer.priceAmount * quantity },
        durationMinutes = selected.sumOf { (offer, quantity) -> offer.durationMinutes * quantity }
    )
}

internal fun providerOfferingCustomerDetails(offer: ProviderServiceOffering): List<String> = buildList {
    offer.description.trim().takeIf { it.isNotBlank() }?.let { add("Includes: $it") }
    val attributes = offer.attributeValues?.entrySet()
        ?.mapNotNull { entry ->
            val value = customerReadableAttributeValue(entry.value)
            value.takeIf { it.isNotBlank() }?.let { "${customerReadableAttributeLabel(entry.key)}: $it" }
        }
        .orEmpty()
    if (attributes.isNotEmpty()) add("Details: ${attributes.joinToString(" · ")}")
}

private fun customerReadableAttributeLabel(key: String): String = key.trim()
    .replace('_', ' ')
    .split(' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

private fun customerReadableAttributeValue(value: com.google.gson.JsonElement): String = when {
    value.isJsonNull -> ""
    value.isJsonArray -> value.asJsonArray.map(::customerReadableAttributeValue).filter { it.isNotBlank() }.joinToString(", ")
    value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> if (value.asBoolean) "Yes" else "No"
    value.isJsonPrimitive -> value.asString.trim()
    else -> ""
}

@Composable
internal fun CustomerServiceScopePicker(
    listing: ServiceListing,
    catalog: ListingServiceCatalog,
    defaultDurationMinutes: Int,
    selectedPackageId: String?,
    selectedOfferingQuantities: Map<String, Int>,
    useListingPrice: Boolean,
    saving: Boolean,
    onSelectPackage: (String) -> Unit,
    onChangeOfferingQuantity: (String, Int) -> Unit,
    onSelectListingPrice: () -> Unit,
    onContinue: () -> Unit
) {
    val summary = customerServiceCartSummary(
        catalog, selectedPackageId, selectedOfferingQuantities, useListingPrice,
        listing.price, defaultDurationMinutes
    )
    Text("Choose what you need", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(
        "Add one complete package, individual work items, or combine a package with extra work from this provider. You pay only Nestora's booking fee now and the shown provider amount after the work.",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF60756B)
    )
    if (catalog.packages.isNotEmpty()) {
        Text("Value packages", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        catalog.packages.forEach { pack ->
            val selected = selectedPackageId == pack.id && !useListingPrice
            val savings = providerPackageSavings(pack)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (selected) Color(0xFFE7F7F1) else Color.White,
                border = BorderStroke(1.dp, if (selected) NestoraMint else Color(0xFFD9E3DF))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(selected, onCheckedChange = { onSelectPackage(pack.id) }, enabled = !saving)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pack.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text("₹${"%.0f".format(pack.packagePriceAmount)}", fontWeight = FontWeight.ExtraBold, color = NestoraMint)
                        }
                        if (pack.description.isNotBlank()) Text(pack.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B))
                        Text("${pack.durationMinutes} min · ${pack.items.sumOf { it.quantity.coerceAtLeast(1) }} work item(s)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF486158))
                        Text("Includes: ${providerPackageItemsLabel(pack)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF486158), maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (pack.includedText.isNotBlank()) Text("Package includes: ${pack.includedText}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF486158), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (savings > 0) Text("You save ₹${"%.0f".format(savings)}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF14513D), fontWeight = FontWeight.Bold)
                        if (pack.excludedText.isNotBlank()) Text("Not included: ${pack.excludedText}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A4B00), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        OutlinedButton(
                            onClick = { onSelectPackage(pack.id) },
                            enabled = !saving,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                        ) { Text(if (selected) "Remove package" else "Add package") }
                    }
                }
            }
        }
    }
    if (catalog.offerings.isNotEmpty()) {
        Text("Individual work items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        catalog.offerings.forEach { offer ->
            val quantity = selectedOfferingQuantities[offer.id] ?: 0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (quantity > 0) Color(0xFFE7F7F1) else Color.White,
                border = BorderStroke(1.dp, if (quantity > 0) NestoraMint else Color(0xFFD9E3DF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(offer.title, fontWeight = FontWeight.SemiBold)
                        providerOfferingCustomerDetails(offer).forEach { detail ->
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = Color(0xFF486158), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text("₹${"%.0f".format(offer.priceAmount)} · ${offer.durationMinutes} min each", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B))
                    }
                    if (quantity == 0) {
                        OutlinedButton(
                            onClick = { onChangeOfferingQuantity(offer.id, 1) },
                            enabled = selectedOfferingQuantities.size < 12 && !saving,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("Add") }
                    } else {
                        IconButton(
                            onClick = { onChangeOfferingQuantity(offer.id, quantity - 1) },
                            enabled = !saving
                        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                        Text(quantity.toString(), modifier = Modifier.widthIn(min = 20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { onChangeOfferingQuantity(offer.id, quantity + 1) },
                            enabled = quantity < 10 && !saving
                        ) { Icon(Icons.Default.Add, contentDescription = "Add one ${offer.title}") }
                    }
                }
            }
        }
    }
    if (catalog.packages.isEmpty() && catalog.offerings.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF7F9F8),
            border = BorderStroke(1.dp, Color(0xFFD9E3DF))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("No packages or priced items yet", fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
                Text(
                    "This provider has not published a service catalog for this service type. You can still send a custom request using the listing's starting price.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF60756B)
                )
            }
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !saving, onClick = onSelectListingPrice),
        shape = RoundedCornerShape(12.dp),
        color = if (useListingPrice) Color(0xFFE7F7F1) else Color.White,
        border = BorderStroke(1.dp, if (useListingPrice) NestoraMint else Color(0xFFD9E3DF))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(selected = useListingPrice, onClick = null)
            Column(modifier = Modifier.weight(1f)) {
                Text("Custom service request", fontWeight = FontWeight.SemiBold)
                Text("Starting from ₹${listing.price.toInt()}. The provider may confirm the final work amount before acceptance.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B))
            }
        }
    }
    if (summary != null) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF0F8F4), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Your cart", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF486158))
                Text(summary.title, fontWeight = FontWeight.Bold, color = Color(0xFF15231D))
                Text(
                    if (summary.kind == "LISTING") "Starting from ₹${summary.providerAmount.toInt()} · provider confirms final amount"
                    else "${summary.itemCount} item(s) · ₹${summary.providerAmount.toInt()} provider amount · ${summary.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF486158)
                )
            }
        }
    }
    Button(
        onClick = onContinue,
        enabled = summary != null && !saving,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
    ) {
        if (saving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(
            when (summary?.kind) {
                "PACKAGE" -> "Continue with package"
                "MIXED" -> "Continue with cart"
                "ITEMS" -> "Continue with cart"
                else -> "Continue with custom request"
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SelectedServiceScopeSummary(
    summary: CustomerServiceCartSummary,
    onChange: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF4F8F6),
        border = BorderStroke(1.dp, Color(0xFFD9E8E0))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (summary.kind == "PACKAGE") "Selected package" else "Selected service scope", fontWeight = FontWeight.Bold, color = Color(0xFF486158))
                TextButton(onClick = onChange, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) { Text("Change") }
            }
            Text(summary.title, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15231D))
            Text(
                if (summary.kind == "LISTING") "Starting from ₹${summary.providerAmount.toInt()}; the provider confirms the final amount."
                else "${summary.itemCount} item(s) · ₹${summary.providerAmount.toInt()} provider amount · ${summary.durationMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF486158)
            )
            Text("The provider amount is paid directly after work. Nestora's booking fee is handled separately after provider acceptance.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60756B))
        }
    }
}

// P3's one sheet adapts from catalog policy instead of maintaining a flow per
// service. It writes only P2 app drafts; no legacy CREATE_BOOKING call is used.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdaptiveBookingSheet(
    listing: ServiceListing,
    initialPackageId: String? = null,
    initialOfferingQuantities: Map<String, Int> = emptyMap(),
    initialUseListingPrice: Boolean = false,
    initialLocationText: String,
    initialLat: Double,
    initialLon: Double,
    onDismiss: () -> Unit,
    onFetchPolicy: suspend (String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onFetchAvailability: suspend (String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onFetchServiceCatalog: suspend (String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onFetchDraftAvailability: suspend (String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onCreateDraft: suspend (String, String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onSetLocation: suspend (String, Boolean, Double, Double, String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onSetSchedule: suspend (String, String?, String?, String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onSetTimePreference: suspend (String, String, JsonObject) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onSetServiceSelection: suspend (String, JsonObject) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onSetNote: suspend (String, String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onSetAnswer: suspend (String, String, String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onSubmit: suspend (String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onBookingCreated: (String) -> Unit,
    onChangeLocationClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var policy by remember { mutableStateOf<BookingPolicy?>(null) }
    var draftId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var timing by remember { mutableStateOf("NOW") }
    var flexibleTimeTerm by remember { mutableStateOf<String?>(null) }
    var recurrence by remember { mutableStateOf("ONE_TIME") }
    var note by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf(120) }
    var availabilitySlots by remember { mutableStateOf<List<AvailabilitySlot>>(emptyList()) }
    var serviceCatalog by remember { mutableStateOf<ListingServiceCatalog?>(null) }
    var serviceCatalogUnavailable by remember { mutableStateOf(false) }
    var selectionSaving by remember { mutableStateOf(false) }
    var selectedPackageId by remember(listing.id, initialPackageId) { mutableStateOf(initialPackageId) }
    var useListingPriceSelection by remember(listing.id, initialUseListingPrice) { mutableStateOf(initialUseListingPrice) }
    var serviceSelectionApplied by remember { mutableStateOf(false) }
    var selectionNotice by remember { mutableStateOf<String?>(null) }
    val selectedOfferingQuantities = remember(listing.id, initialOfferingQuantities) {
        mutableStateMapOf<String, Int>().apply {
            initialOfferingQuantities.filterValues { it in 1..10 }.forEach { (id, quantity) -> put(id, quantity) }
        }
    }
    var selectedSlot by remember { mutableStateOf<AvailabilitySlot?>(null) }
    var flexibleStartDate by remember { mutableStateOf(java.time.LocalDate.now().plusDays(1).toString()) }
    var flexibleEndDate by remember { mutableStateOf(java.time.LocalDate.now().plusDays(3).toString()) }
	var flexibleStartAt by remember { mutableStateOf(Instant.now().plusSeconds(86_400).toString()) }
	var flexibleEndAt by remember { mutableStateOf(Instant.now().plusSeconds(93_600).toString()) }
	var selectedFlexibleSlot by remember { mutableStateOf<AvailabilitySlot?>(null) }
    var deadlineAt by remember { mutableStateOf(Instant.now().plusSeconds(7 * 86_400).toString()) }
    val answers = remember { mutableStateMapOf<String, String>() }

    val kolkataZone = remember { java.time.ZoneId.of("Asia/Kolkata") }
    val todayLocalDate = remember { java.time.LocalDate.now(kolkataZone) }
    val nowInstant = remember { java.time.Instant.now() }

    // Find the first upcoming day that has available future slots
    val scheduleTargetDate = remember(availabilitySlots) {
        availabilitySlots.firstOrNull { slot ->
            try {
                java.time.Instant.parse(slot.startAt).isAfter(nowInstant)
            } catch (_: Exception) {
                false
            }
        }?.let { firstSlot ->
            try {
                java.time.Instant.parse(firstSlot.startAt).atZone(kolkataZone).toLocalDate()
            } catch (_: Exception) {
                null
            }
        }
    }

    // Target date's future slots for Schedule section
    val todaySlots = remember(availabilitySlots, scheduleTargetDate) {
        if (scheduleTargetDate == null) {
            emptyList()
        } else {
            availabilitySlots.filter { slot ->
                try {
                    val slotInstant = java.time.Instant.parse(slot.startAt)
                    val slotLocalDate = slotInstant.atZone(kolkataZone).toLocalDate()
                    slotLocalDate.isEqual(scheduleTargetDate) && slotInstant.isAfter(nowInstant)
                } catch (_: Exception) {
                    false
                }
            }
        }
    }

    // Next day to 1 week slots for Flexible section
    val flexibleSlots = remember(availabilitySlots) {
        availabilitySlots.filter { slot ->
            try {
                val slotInstant = java.time.Instant.parse(slot.startAt)
                val slotLocalDate = slotInstant.atZone(kolkataZone).toLocalDate()
                val tomorrow = todayLocalDate.plusDays(1)
                val oneWeekLater = todayLocalDate.plusDays(7)
                !slotLocalDate.isBefore(tomorrow) && !slotLocalDate.isAfter(oneWeekLater)
            } catch (_: Exception) {
                false
            }
        }
    }

    val validLocation = initialLat.isFinite() && initialLon.isFinite() &&
        initialLat in -90.0..90.0 && initialLon in -180.0..180.0 && !(initialLat == 0.0 && initialLon == 0.0)

    suspend fun refreshLiveAvailability(showChangeMessage: Boolean): Boolean {
        try {
            val policyResponse = onFetchPolicy(listing.id)
            val freshPolicy = policyResponse?.bookingPolicy
            if (freshPolicy != null && freshPolicy != policy) {
                policy = freshPolicy
                durationMinutes = freshPolicy.defaultDurationMinutes
                val terms = freshPolicy.timeTerms.ifEmpty {
                    buildList {
                        if (freshPolicy.timingModes.contains("NOW")) add("NOW")
                        if (freshPolicy.timingModes.contains("SCHEDULED")) add("EXACT_SLOT")
                    }
                }
                if (timing !in terms) {
                    timing = if (terms.contains("NOW")) "NOW" else if (terms.contains("EXACT_SLOT") || terms.contains("RECURRENCE")) "SCHEDULED" else "FLEXIBLE"
                }
            }
        } catch (_: Exception) {}

        val activeDraftId = draftId
        val response = if (serviceSelectionApplied && !activeDraftId.isNullOrBlank()) {
            onFetchDraftAvailability(activeDraftId)
        } else {
            onFetchAvailability(listing.id)
        }
        if (response?.ok != true) {
            if (showChangeMessage) error = "Could not refresh live provider availability. Your selected time was not submitted. Please try again."
            return false
        }
        val fresh = response.availabilitySlots
        availabilitySlots = fresh
        var selectedTimeWasRemoved = false
        if (selectedSlot != null && selectedSlot !in fresh) {
            selectedSlot = null
            selectedTimeWasRemoved = true
        }
        if (selectedFlexibleSlot != null && selectedFlexibleSlot !in fresh) {
            selectedFlexibleSlot = null
            selectedTimeWasRemoved = true
        }
        if (selectedTimeWasRemoved && showChangeMessage) {
            error = "Provider availability changed. Your selected time is no longer available; choose a new live time."
        }
        return !selectedTimeWasRemoved
    }

    LaunchedEffect(listing.id) {
        loading = true; error = null
        val policyResponse = onFetchPolicy(listing.id)
        val loadedPolicy = policyResponse?.bookingPolicy
        policy = loadedPolicy
        val catalogResponse = onFetchServiceCatalog(listing.id)
        val loadedCatalog = catalogResponse?.serviceCatalog
        serviceCatalog = loadedCatalog
        serviceCatalogUnavailable = catalogResponse?.ok != true || loadedCatalog == null
        // Even an empty catalogue must show the scope step so the customer
        // explicitly chooses a custom request instead of silently bypassing it.
        serviceSelectionApplied = false
        val cleanInitialPackageId = initialPackageId?.takeIf { id -> loadedCatalog?.packages?.any { it.id == id } == true }
        val cleanInitialItems = initialOfferingQuantities
            .filterValues { it in 1..10 }
            .filterKeys { id -> loadedCatalog?.offerings?.any { it.id == id } == true }
        val cleanInitialListingPrice = initialUseListingPrice && cleanInitialPackageId == null && cleanInitialItems.isEmpty()
        selectedPackageId = cleanInitialPackageId
        selectedOfferingQuantities.clear()
        selectedOfferingQuantities.putAll(cleanInitialItems)
        useListingPriceSelection = cleanInitialListingPrice
        if (serviceCatalogUnavailable) {
            error = catalogResponse?.reply?.ifBlank { null } ?: "Could not load this provider's service options. Try again before continuing."
        }
        loadedPolicy?.let { loaded ->
            val terms = loaded.timeTerms.ifEmpty {
                buildList {
                    if (loaded.timingModes.contains("NOW")) add("NOW")
                    if (loaded.timingModes.contains("SCHEDULED")) add("EXACT_SLOT")
                }
            }
            flexibleTimeTerm = terms.firstOrNull { it in setOf("PREFERRED_TIME_WINDOW", "PREFERRED_DATE_RANGE", "OCCUPANCY_INTERVAL", "SUBSCRIPTION_START", "DEADLINE") }
            timing = if (terms.contains("NOW")) "NOW" else if (terms.contains("EXACT_SLOT") || terms.contains("RECURRENCE")) "SCHEDULED" else "FLEXIBLE"
            durationMinutes = loaded.defaultDurationMinutes
			if (terms.any { it in setOf("EXACT_SLOT", "RECURRENCE", "PREFERRED_TIME_WINDOW") }) {
				availabilitySlots = onFetchAvailability(listing.id)?.availabilitySlots ?: emptyList()
			}
			if (terms.contains("PREFERRED_TIME_WINDOW")) {
				availabilitySlots.firstOrNull()?.let { slot ->
					selectedFlexibleSlot = slot
					flexibleStartAt = slot.startAt
					flexibleEndAt = slot.endAt
				}
			}
        }
        val response = loadedPolicy?.let { onCreateDraft(listing.id, UUID.randomUUID().toString()) }
        draftId = response?.engagementDraft?.id
        if (loadedPolicy == null || draftId == null) {
            error = bookingStartFailureMessage(
                policyLoaded = loadedPolicy != null,
                policyReply = policyResponse?.reply,
                draftReply = response?.reply
            )
        } else if (loadedCatalog != null) {
            val initialSelection = customerServiceSelectionPayload(
                cleanInitialPackageId,
                cleanInitialItems,
                cleanInitialListingPrice
            )
            if (initialSelection != null) {
                selectionSaving = true
                val savedSelection = onSetServiceSelection(draftId!!, initialSelection)
                if (savedSelection?.ok == true) {
                    serviceSelectionApplied = true
                    availabilitySlots = onFetchDraftAvailability(draftId!!)?.availabilitySlots ?: availabilitySlots
                    selectionNotice = "Your cart is ready. Choose when you need the service."
                } else {
                    error = savedSelection?.reply ?: "Could not prepare your selected services. Return to the cart and try again."
                }
                selectionSaving = false
            }
        }
        loading = false
    }

    // Booking availability is short-lived data. Refresh only while this
    // page is visible.
    LaunchedEffect(listing.id) {
        while (isActive) {
            delay(15_000)
            refreshLiveAvailability(showChangeMessage = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Service") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(listing.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("by ${listing.providerName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            if (loading) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator(color = NestoraMint) }
            } else {
                val p = policy
                if (p != null) {
					val catalog = serviceCatalog
					val cartSummary = catalog?.let {
						customerServiceCartSummary(
							it, selectedPackageId, selectedOfferingQuantities, useListingPriceSelection,
							listing.price, p.defaultDurationMinutes
						)
					}
					val serviceSelectionPayload = catalog?.let {
						customerServiceSelectionPayload(
							selectedPackageId, selectedOfferingQuantities, useListingPriceSelection
						)
					}
					val applyCurrentSelection: () -> Unit = {
						val selection = customerServiceSelectionPayload(
							selectedPackageId, selectedOfferingQuantities, useListingPriceSelection
						)
						scope.launch {
							val currentDraftId = draftId
							if (currentDraftId == null || selection == null) {
								error = if (currentDraftId == null) {
									"This booking form is no longer active. Open the service again to choose work."
								} else {
									"Add a package, individual work items, both together, or choose a custom service request."
								}
								return@launch
							}
							selectionSaving = true
							error = null
							selectionNotice = null
							val response = onSetServiceSelection(currentDraftId, selection)
							if (response?.ok == true) {
								selectedSlot = null
								selectedFlexibleSlot = null
								availabilitySlots = onFetchDraftAvailability(currentDraftId)?.availabilitySlots ?: emptyList()
								serviceSelectionApplied = true
								selectionNotice = "Service scope added. Available times now match the expected work duration."
							} else {
								error = response?.reply ?: "Could not add the selected service scope. Please try again."
							}
							selectionSaving = false
						}
					}
					if (serviceCatalogUnavailable) {
						error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
						OutlinedButton(
							onClick = {
								scope.launch {
									selectionSaving = true
									val response = onFetchServiceCatalog(listing.id)
									val refreshedCatalog = response?.serviceCatalog
									if (response?.ok == true && refreshedCatalog != null) {
										serviceCatalog = refreshedCatalog
										serviceCatalogUnavailable = false
										serviceSelectionApplied = false
										error = null
									} else {
										error = response?.reply ?: "Service options are still unavailable. Check your connection and try again."
									}
									selectionSaving = false
								}
							},
							enabled = !selectionSaving,
							modifier = Modifier.fillMaxWidth()
						) { Text(if (selectionSaving) "Loading service options" else "Try again") }
					} else if (catalog != null && !serviceSelectionApplied) {
						CustomerServiceScopePicker(
							listing = listing,
							catalog = catalog,
							selectedPackageId = selectedPackageId,
							selectedOfferingQuantities = selectedOfferingQuantities,
							useListingPrice = useListingPriceSelection,
							defaultDurationMinutes = p.defaultDurationMinutes,
							saving = selectionSaving,
							onSelectPackage = { packageId ->
								selectedPackageId = if (selectedPackageId == packageId) null else packageId
								useListingPriceSelection = false
								error = null
							},
							onChangeOfferingQuantity = { offeringId, quantity ->
								useListingPriceSelection = false
								if (quantity <= 0) selectedOfferingQuantities.remove(offeringId)
								else selectedOfferingQuantities[offeringId] = quantity.coerceAtMost(10)
								error = null
							},
							onSelectListingPrice = {
								selectedPackageId = null
								selectedOfferingQuantities.clear()
								useListingPriceSelection = true
								error = null
							},
							onContinue = applyCurrentSelection
						)
						error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
					} else {
						cartSummary?.let { summary ->
							SelectedServiceScopeSummary(summary = summary, onChange = {
								serviceSelectionApplied = false
								selectionNotice = null
								selectedSlot = null
								selectedFlexibleSlot = null
							})
						}
						selectionNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF14513D)) }
                    val timeTerms = p.timeTerms.ifEmpty {
                        buildList {
                            if (p.timingModes.contains("NOW")) add("NOW")
                            if (p.timingModes.contains("SCHEDULED")) add("EXACT_SLOT")
                        }
                    }
                    val supportsNow = timeTerms.contains("NOW") && p.providerPreset == "ASAP_ONLY"
                    val supportsExactSlot = (timeTerms.contains("EXACT_SLOT") || timeTerms.contains("RECURRENCE")) && p.providerPreset == "CUSTOM" && availabilitySlots.isNotEmpty()
                    val flexibleTerms = if (p.providerPreset == "CUSTOM") timeTerms.filter { it in setOf("PREFERRED_TIME_WINDOW", "PREFERRED_DATE_RANGE", "OCCUPANCY_INTERVAL", "SUBSCRIPTION_START", "DEADLINE") } else emptyList()
                    val activeOptions = buildList {
                        if (supportsNow) add("NOW" to "ASAP")
                        if (supportsExactSlot) add("SCHEDULED" to "Schedule")
                        if (flexibleTerms.isNotEmpty()) add("FLEXIBLE" to "Flexible")
                    }
                    if (timing !in activeOptions.map { it.first }) {
                        timing = activeOptions.firstOrNull()?.first ?: "NOW"
                    }
                    val isQualified = p.commitmentGate != "DIRECT"
                    Text(if (isQualified) "Tell us what needs attention" else "Choose when you need the service", fontWeight = FontWeight.Bold)
                    if (activeOptions.size > 1) {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            activeOptions.forEachIndexed { index, (key, label) ->
                                SegmentedButton(
                                    selected = timing == key,
                                    onClick = { timing = key },
                                    shape = SegmentedButtonDefaults.itemShape(index, activeOptions.size)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                    if (p.fulfillmentModel == "RECURRING") {
                        Text("Repeat", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ONE_TIME" to "Once", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly").forEach { (key, label) ->
                                FilterChip(selected = recurrence == key, onClick = { recurrence = key }, label = { Text(label) })
                            }
                        }
                    }
                    if ((timing == "SCHEDULED" || recurrence != "ONE_TIME") && supportsExactSlot) {
                        Text("Choose an available time", fontWeight = FontWeight.Bold)
                        if (todaySlots.isEmpty()) {
							Text("No available time slots fit the provider's upcoming working hours.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Choose a time slot. The provider will confirm it when accepting.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7C4A03))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .background(Color(0xFFF7F9FC), shape = RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Color(0xFFD0D5DD)), shape = RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    todaySlots.forEach { slot ->
                                        val label = liveAvailabilitySlotLabel(slot)
                                        FilterChip(
                                            selected = selectedSlot == slot,
                                            onClick = { selectedSlot = slot },
                                            label = { Text(label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFC8E6C9),
                                                selectedLabelColor = Color(0xFF1B5E20)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (timing == "FLEXIBLE") {
                        Text("Preferred time", fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            flexibleTerms.forEach { term ->
                                FilterChip(selected = flexibleTimeTerm == term, onClick = { flexibleTimeTerm = term }, label = { Text(term.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) })
                            }
                        }
                        when (flexibleTimeTerm) {
                            "PREFERRED_DATE_RANGE" -> {
                                DatePickerField(label = "Start date", value = flexibleStartDate, onValueChange = { flexibleStartDate = it }, modifier = Modifier.fillMaxWidth())
                                DatePickerField(label = "End date", value = flexibleEndDate, onValueChange = { flexibleEndDate = it }, modifier = Modifier.fillMaxWidth())
                            }
                            "SUBSCRIPTION_START" -> {
                                DatePickerField(label = "Start date", value = flexibleStartDate, onValueChange = { flexibleStartDate = it }, modifier = Modifier.fillMaxWidth())
                            }
                            "DEADLINE" -> {
                                DateTimePickerField(label = "Deadline time", value = deadlineAt, onValueChange = { deadlineAt = it }, onPastTimeSelected = { error = "Choose a future date and time." }, modifier = Modifier.fillMaxWidth())
                            }
                            else -> {
                                val slotsByDate = remember(flexibleSlots) {
                                    flexibleSlots.groupBy { java.time.Instant.parse(it.startAt).atZone(kolkataZone).toLocalDate() }
                                }
                                val availableDates = remember(slotsByDate) {
                                    slotsByDate.keys.sorted()
                                }
                                var selectedFlexibleDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
                                
                                LaunchedEffect(availableDates) {
                                    if (selectedFlexibleDate == null || selectedFlexibleDate !in availableDates) {
                                        selectedFlexibleDate = availableDates.firstOrNull()
                                    }
                                }

                                if (availableDates.isEmpty()) {
                                    Text("No available time slots fit the provider's upcoming working hours.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("Choose a time slot. The provider will confirm it when accepting.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7C4A03))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // 1. Render date row
                                    Text("Available Dates", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        availableDates.forEach { date ->
                                            val dateLabel = date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
                                            FilterChip(
                                                selected = selectedFlexibleDate == date,
                                                onClick = { selectedFlexibleDate = date },
                                                label = { Text(dateLabel) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFFC8E6C9),
                                                    selectedLabelColor = Color(0xFF1B5E20)
                                                )
                                            )
                                        }
                                    }

                                    // 2. Render slots for the selected date
                                    val slotsForDate = selectedFlexibleDate?.let { slotsByDate[it] } ?: emptyList()
                                    if (slotsForDate.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Available Times", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 160.dp)
                                                .background(Color(0xFFF7F9FC), shape = RoundedCornerShape(12.dp))
                                                .border(BorderStroke(1.dp, Color(0xFFD0D5DD)), shape = RoundedCornerShape(12.dp))
                                                .padding(8.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                slotsForDate.forEach { slot ->
                                                    val timeLabel = java.time.Instant.parse(slot.startAt)
                                                        .atZone(kolkataZone)
                                                        .toLocalTime()
                                                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                                    FilterChip(
                                                        selected = selectedFlexibleSlot == slot,
                                                        onClick = {
                                                            selectedFlexibleSlot = slot
                                                            flexibleStartAt = slot.startAt
                                                            flexibleEndAt = slot.endAt
                                                            error = null
                                                        },
                                                        label = { Text(timeLabel) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = Color(0xFFC8E6C9),
                                                            selectedLabelColor = Color(0xFF1B5E20)
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (p.fulfillmentModel == "RECURRING") {
                        Text("Visit duration", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(60, 120, 180, 240).forEach { minutes ->
                                FilterChip(selected = durationMinutes == minutes, onClick = { durationMinutes = minutes }, label = { Text("${minutes / 60} hr") })
                            }
                        }
                    }
                    p.requestSchema?.forEach { schemaItem ->
                        val field = schemaItem.asJsonObject
                        val key = field.get("key")?.asString ?: return@forEach
                        val label = field.get("label")?.asString ?: key
                        val required = field.get("required")?.asBoolean == true
                        Text(if (required) "$label *" else label, fontWeight = FontWeight.Bold)
                        if (field.get("input_type")?.asString == "SINGLE_SELECT") {
                            field.getAsJsonArray("options")?.forEach { optionItem ->
                                val option = optionItem.asString
                                FilterChip(selected = answers[key] == option, onClick = { answers[key] = option }, label = { Text(option) })
                            }
                        } else {
                            OutlinedTextField(value = answers[key] ?: "", onValueChange = { if (it.length <= 180) answers[key] = it }, modifier = Modifier.fillMaxWidth(), label = { Text(label) })
                        }
                    }
                    OutlinedTextField(value = note, onValueChange = { if (it.length <= 150) note = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (isQualified) "Issue or task summary" else "Optional note") }, minLines = if (isQualified) 3 else 1)
                    Text("Service location", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(initialLocationText.ifBlank { "No location selected" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onChangeLocationClick) { Text("Change") }
                    }
                    if (isQualified) Text("The provider reviews your structured request before confirming final scope or price.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7C4A03))
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Button(
                        onClick = {
                            scope.launch {
                                val id = draftId ?: return@launch
                                if (!validLocation) { error = "Choose a valid service location first."; return@launch }
                                val missing = p.requestSchema?.firstOrNull { item ->
                                    val field = item.asJsonObject
                                    field.get("required")?.asBoolean == true && answers[field.get("key")?.asString].isNullOrBlank()
                                }
                                if (missing != null) { error = "Complete ${missing.asJsonObject.get("label")?.asString ?: "the required details"}."; return@launch }
                                val unsafeInput = (answers.values + note).firstOrNull { quoteScopeSafetyError(it) != null }
                                if (unsafeInput != null) {
                                    error = quoteScopeSafetyError(unsafeInput)
                                    return@launch
                                }
                                val scheduled = (timing == "SCHEDULED" || recurrence != "ONE_TIME") && supportsExactSlot
								val needsLiveAvailability = scheduled || (timing == "FLEXIBLE" && flexibleTimeTerm == "PREFERRED_TIME_WINDOW")
								if (needsLiveAvailability && !refreshLiveAvailability(showChangeMessage = true)) {
									if (error == null) error = "Provider availability changed. Choose a new live time before sending your request."
									return@launch
								}
                                if (scheduled && selectedSlot == null) { error = "Choose one of the provider's available times."; return@launch }
                                val term = if (timing == "FLEXIBLE") flexibleTimeTerm else null
                                if (timing == "FLEXIBLE" && term == null) { error = "Choose your preferred time."; return@launch }
								if (timing == "FLEXIBLE" && term == "PREFERRED_TIME_WINDOW" && selectedFlexibleSlot == null) {
									error = "Choose a live provider time. This provider cannot be requested outside their published working hours."
									return@launch
								}

                                var failedStep = "request submission"
                                suspend fun saveAndSubmit(activeDraftId: String, replayServiceSelection: Boolean): com.estatenestora.app.data.model.AndroidBridgeResponse? {
                                    if (replayServiceSelection && serviceSelectionPayload != null) {
                                        val savedScope = onSetServiceSelection(activeDraftId, serviceSelectionPayload)
                                        if (savedScope?.ok != true) { failedStep = "selected services"; return savedScope }
                                    }
                                    val location = onSetLocation(activeDraftId, true, initialLat, initialLon, initialLocationText)
                                    if (location?.ok != true) { failedStep = "service location"; return location }
                                    if (term != null) {
                                        val preference = JsonObject().apply {
                                            when (term) {
                                                "PREFERRED_DATE_RANGE" -> { addProperty("start_date", flexibleStartDate); addProperty("end_date", flexibleEndDate) }
                                                "SUBSCRIPTION_START" -> addProperty("start_date", flexibleStartDate)
                                                "DEADLINE" -> addProperty("deadline_at", deadlineAt)
                                                else -> { addProperty("start_at", flexibleStartAt); addProperty("end_at", flexibleEndAt) }
                                            }
                                        }
                                        val savedPreference = onSetTimePreference(activeDraftId, term, preference)
                                        if (savedPreference?.ok != true) { failedStep = "preferred time"; return savedPreference }
                                    } else {
                                        val start = if (scheduled) selectedSlot!!.startAt else null
                                        val end = if (scheduled) selectedSlot!!.endAt else null
                                        val schedule = onSetSchedule(activeDraftId, start, end, recurrence)
                                        if (schedule?.ok != true) { failedStep = "service time"; return schedule }
                                    }
                                    for ((key, value) in answers) {
                                        val savedAnswer = onSetAnswer(activeDraftId, key, value)
                                        if (savedAnswer?.ok != true) { failedStep = "request details"; return savedAnswer }
                                    }
                                    val savedNote = onSetNote(activeDraftId, note)
                                    if (savedNote?.ok != true) { failedStep = "request summary"; return savedNote }
                                    failedStep = "request submission"
                                    return onSubmit(activeDraftId)
                                }

                                submitting = true; error = null
                                var submitted = saveAndSubmit(id, replayServiceSelection = false)
                                // A draft can become stale after the sheet opened (for
                                // example after an app reconnect). Preserve every typed
                                // value, create one fresh draft, and replay the form once.
                                if (isEngagementDraftNoLongerActive(submitted)) {
                                    val refreshed = onCreateDraft(listing.id, UUID.randomUUID().toString())
                                    val refreshedId = refreshed?.engagementDraft?.id
                                    if (refreshed?.ok == true && !refreshedId.isNullOrBlank()) {
                                        draftId = refreshedId
                                        submitted = saveAndSubmit(refreshedId, replayServiceSelection = true)
                                    } else {
                                        submitted = refreshed
                                    }
                                }
                                submitting = false
                                submitted?.bookingId?.let(onBookingCreated) ?: run {
                                    error = bookingSubmissionFailureMessage(failedStep, submitted?.reply)
                                }
                            }
                        },
                        enabled = !submitting && validLocation,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                    ) { if (submitting) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text(if (p.commitmentGate == "DIRECT") "Request service" else "Send request", fontWeight = FontWeight.Bold) }
					}
                }
            }
        }
    }
}

internal fun liveAvailabilitySlotLabel(slot: AvailabilitySlot): String = try {
    Instant.parse(slot.startAt).atZone(java.time.ZoneId.of("Asia/Kolkata")).let {
        it.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d HH:mm"))
    }
} catch (_: Exception) {
    "Available time"
}

internal fun availabilityDurationLabel(durationMinutes: Int): String {
    val minutes = durationMinutes.coerceAtLeast(15)
    return when {
        minutes % 60 == 0 -> "${minutes / 60} hour" + if (minutes == 60) "" else "s"
        else -> "$minutes minutes"
    }
}

private fun isEngagementDraftNoLongerActive(response: com.estatenestora.app.data.model.AndroidBridgeResponse?): Boolean {
    if (response?.ok != false) return false
    val message = response.reply.lowercase()
    return message.contains("booking form is no longer active") ||
        message.contains("draft is unavailable") ||
        message.contains("draft is expired") ||
        message.contains("draft is expired or already closed")
}

@Composable
fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedDate = try {
        java.time.LocalDate.parse(value)
    } catch (e: Exception) {
        java.time.LocalDate.now().plusDays(1)
    }
    
    val displayValue = parsedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select Date"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    val dialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selected = java.time.LocalDate.of(year, month + 1, dayOfMonth)
                            onValueChange(selected.toString())
                        },
                        parsedDate.year,
                        parsedDate.monthValue - 1,
                        parsedDate.dayOfMonth
                    )
                    dialog.datePicker.minDate = System.currentTimeMillis()
                    dialog.show()
                }
        )
    }
}

@Composable
fun DateTimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPastTimeSelected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedDateTime = try {
        java.time.ZonedDateTime.parse(value)
    } catch (e: Exception) {
        try {
            java.time.Instant.parse(value).atZone(java.time.ZoneId.of("Asia/Kolkata"))
        } catch (ex: Exception) {
            java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).plusDays(1)
        }
    }

    val displayValue = parsedDateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a"))

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select Date and Time"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val localDate = java.time.LocalDate.of(year, month + 1, dayOfMonth)
                                    val localTime = java.time.LocalTime.of(hourOfDay, minute)
                                    val zonedDateTime = java.time.ZonedDateTime.of(
                                        localDate,
                                        localTime,
                                        java.time.ZoneId.of("Asia/Kolkata")
                                    )
                                    if (zonedDateTime.toInstant().isAfter(java.time.Instant.now())) {
                                        onValueChange(zonedDateTime.toOffsetDateTime().toString())
                                    } else {
                                        onPastTimeSelected()
                                    }
                                },
                                parsedDateTime.hour,
                                parsedDateTime.minute,
                                false
                            ).show()
                        },
                        parsedDateTime.year,
                        parsedDateTime.monthValue - 1,
                        parsedDateTime.dayOfMonth
                    ).also { dialog ->
                        dialog.datePicker.minDate = System.currentTimeMillis()
                    }.show()
                }
        )
    }
}
