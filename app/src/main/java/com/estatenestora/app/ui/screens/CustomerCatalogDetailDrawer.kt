package com.estatenestora.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.*
import java.util.Locale

/** Custom package selections are ordinary active offerings, priced by the server. */
internal fun customerPackageItemSelection(
    catalog: ListingServiceCatalog,
    existing: Map<String, Int>,
    additions: Map<String, Int>
): Map<String, Int> = existing.toMutableMap().apply {
    additions.forEach { (id, quantity) ->
        if (quantity > 0 && catalog.offerings.any { it.id == id && it.isActive }) {
            this[id] = 1
        }
    }
}

private fun detailPrice(amount: Double): String = String.format(Locale.US, "INR %,.2f", amount)

@Composable
internal fun CustomerCatalogDetailDrawer(
    listing: ServiceListing,
    catalog: ListingServiceCatalog,
    offering: ProviderServiceOffering? = null,
    pack: ProviderServicePackage? = null,
    quantity: Int = 0,
    packageSelected: Boolean = false,
    existingQuantities: Map<String, Int> = emptyMap(),
    onResolveMedia: suspend (String) -> String?,
    onDismiss: () -> Unit,
    onOfferingQuantity: (Int) -> Unit = {},
    onCompletePackage: () -> Unit = {},
    onPackageItems: (Map<String, Int>) -> Unit = {}
) {
    val identity = offering?.id ?: pack?.id
    var custom by remember(identity) { mutableStateOf(false) }
    val chosen = remember(identity) { mutableStateMapOf<String, Int>() }
    val media = offering?.media ?: pack?.media
    val fileId = media?.fileIdFor("CARD")
    var photo by remember(fileId) { mutableStateOf<String?>(null) }
    LaunchedEffect(fileId) { photo = fileId?.let { onResolveMedia(it) } }
    val available = remember(catalog, pack) {
        pack?.items.orEmpty().mapNotNull { item ->
            catalog.offerings.firstOrNull { it.id == item.id && it.isActive }
        }.distinctBy { it.id }
    }
    val amount = if (custom) available.sumOf { it.priceAmount * (chosen[it.id] ?: 0) }
        else pack?.packagePriceAmount ?: (offering?.priceAmount ?: 0.0)

    FilterOverlaySheet(
        title = if (pack != null) "Package details" else "Service details",
        onDismissRequest = onDismiss
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                photo?.let { AsyncImage(it, contentDescription = offering?.title ?: pack?.name, modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop) }
                Text(offering?.title ?: pack?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Provided by ${listing.providerName}")
                Text(if (listing.rating > 0f) String.format(Locale.US, "Provider rating: %.1f / 5", listing.rating) else "Provider has no ratings yet")
                Text("Ratings for this specific ${if (pack != null) "package" else "service"} are not available.", style = MaterialTheme.typography.bodySmall)
                Text("${detailPrice(pack?.packagePriceAmount ?: offering?.priceAmount ?: 0.0)} · ${pack?.durationMinutes ?: offering?.durationMinutes ?: 0} min", fontWeight = FontWeight.Bold)
                val description = offering?.description ?: pack?.description.orEmpty()
                Text(description.ifBlank { "No additional description provided." })
                offering?.let { offer ->
                    offer.attributeValues?.entrySet()?.filterNot { it.key in setOf("photo_url", "image_url", "media_url") || it.value.isJsonNull }?.forEach { (key, value) ->
                        val rendered = if (value.isJsonPrimitive) value.asString else value.toString()
                        Text("${key.replace('_', ' ')}: $rendered")
                    }
                }
                pack?.let { bundle ->
                    if (bundle.includedText.isNotBlank()) { Text("Included", fontWeight = FontWeight.Bold); Text(bundle.includedText) }
                    if (bundle.excludedText.isNotBlank()) { Text("Not included", fontWeight = FontWeight.Bold); Text(bundle.excludedText) }
                    Text("Package contents", fontWeight = FontWeight.Bold)
                    bundle.items.forEach { item ->
                        Text("${item.quantity} × ${item.title}")
                        providerOfferingCustomerDetails(item).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    HorizontalDivider()
                    FilterChip(selected = !custom, onClick = { custom = false }, label = { Text("Complete package") })
                    FilterChip(selected = custom, onClick = { custom = true }, label = { Text("Choose individual services") })
                    if (custom) {
                        Text("Selected services use individual prices. Adding them replaces this complete package if it is already in your cart.")
                        if (available.size < bundle.items.distinctBy { it.id }.size) Text("Some package contents are available only with the complete package.")
                        available.forEach { item ->
                            val alreadySelected = existingQuantities[item.id] == 1
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = (chosen[item.id] ?: 0) > 0, enabled = !alreadySelected, onCheckedChange = { checked ->
                                    if (checked) chosen[item.id] = 1 else chosen.remove(item.id)
                                })
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.Bold)
                                    Text("${detailPrice(item.priceAmount)} each · ${item.durationMinutes} min")
                                    if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall)
                                    if (alreadySelected) Text("Already in cart")
                                }
                            }
                        }
                        if (available.isEmpty()) Text("No services in this package are currently sold individually.")
                    } else if (packageSelected) Text("This complete package is already in your cart.")
                }
            }
            HorizontalDivider()
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${if (custom) "Selected services" else "Service estimate"}: ${detailPrice(amount)}", fontWeight = FontWeight.Bold)
                Text("Provider estimate; Nestora's platform fee is shown separately at checkout.", style = MaterialTheme.typography.bodySmall)
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = if (custom) chosen.isNotEmpty() else if (pack != null) !packageSelected else offering?.isActive == true,
                    onClick = {
                        if (custom) onPackageItems(chosen.toMap()) else if (pack != null) onCompletePackage() else onOfferingQuantity(1)
                        onDismiss()
                    }
                ) { Text(if (custom) "Add selected services" else if (pack != null) { if (packageSelected) "Already in cart" else "Add complete package" } else if (quantity > 0) "Update cart" else "Add to cart") }
            }
        }
    }
}
