package com.estatenestora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.estatenestora.app.data.model.ListingServiceCatalog
import com.estatenestora.app.data.model.MediaAsset
import com.estatenestora.app.data.model.ProviderServiceOffering
import com.estatenestora.app.data.model.ProviderServicePackage
import com.estatenestora.app.data.model.ServiceListing
import com.google.gson.JsonElement
import java.util.Locale

private val ExperienceInk = Color(0xFF171B19)
private val ExperienceMuted = Color(0xFF66716C)
private val ExperienceCanvas = Color(0xFFF5F6F4)
private val ExperienceTint = Color(0xFFF0F8F4)
private val ExperienceAccent = Color(0xFF176B50)

private fun experiencePrice(amount: Double): String = String.format(Locale.US, "₹%,.0f", amount)

private fun experienceAttributeLabel(key: String): String = key
    .replace('_', ' ')
    .trim()
    .split(' ')
    .filter(String::isNotBlank)
    .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

private fun experienceAttributeValue(value: JsonElement): String = when {
    value.isJsonNull -> ""
    value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> if (value.asBoolean) "Yes" else "No"
    value.isJsonArray -> value.asJsonArray.joinToString(", ") { experienceAttributeValue(it) }
    value.isJsonPrimitive -> value.asString
    else -> value.toString()
}

@Composable
internal fun CustomerCatalogExperienceDrawer(
    listing: ServiceListing,
    catalog: ListingServiceCatalog,
    offering: ProviderServiceOffering? = null,
    pack: ProviderServicePackage? = null,
    quantity: Int = 0,
    packageSelected: Boolean = false,
    existingQuantities: Map<String, Int> = emptyMap(),
    showDuration: Boolean = true,
    propertyMode: Boolean = false,
    onResolveMedia: suspend (String) -> String?,
    onDismiss: () -> Unit,
    onOfferingQuantity: (Int) -> Unit = {},
    onCompletePackage: () -> Unit = {},
    onPackageItems: (Map<String, Int>) -> Unit = {}
) {
    val identity = offering?.id ?: pack?.id
    var custom by remember(identity) { mutableStateOf(false) }
    val chosen = remember(identity) { mutableStateMapOf<String, Int>() }
    val available = remember(catalog, pack) {
        pack?.items.orEmpty().mapNotNull { item ->
            catalog.offerings.firstOrNull { it.id == item.id && it.isActive }
        }.distinctBy { it.id }
    }
    val amount = if (custom) available.filter { chosen[it.id] == 1 }.sumOf { it.priceAmount }
        else pack?.packagePriceAmount ?: offering?.priceAmount ?: 0.0

    FilterOverlaySheet(
        title = if (propertyMode) "Property details" else if (pack != null) "Package options" else "Service details",
        onDismissRequest = onDismiss
    ) {
        Column(Modifier.fillMaxSize().background(ExperienceCanvas)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when {
                    pack != null -> PackageExperience(
                        listing = listing,
                        pack = pack,
                        available = available,
                        custom = custom,
                        chosen = chosen,
                        existingQuantities = existingQuantities,
                        propertyMode = propertyMode,
                        onCustomChanged = { custom = it },
                        onResolveMedia = onResolveMedia
                    )
                    offering != null -> OfferingExperience(listing, offering, showDuration, propertyMode, onResolveMedia)
                }
                Spacer(Modifier.height(14.dp))
            }

            Surface(shadowElevation = 12.dp, color = Color.White) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (custom) "Selected total" else if (propertyMode) "Listed property price" else if (pack != null) "Package total" else "Service price",
                            color = ExperienceMuted,
                            fontSize = 12.sp
                        )
                        Text(experiencePrice(amount), color = ExperienceInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        modifier = Modifier.height(52.dp).widthIn(min = 168.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = if (custom) chosen.isNotEmpty() else if (pack != null) !packageSelected else offering?.isActive == true,
                        colors = ButtonDefaults.buttonColors(containerColor = ExperienceAccent),
                        onClick = {
                            when {
                                custom -> onPackageItems(chosen.toMap())
                                pack != null -> onCompletePackage()
                                else -> onOfferingQuantity(if (quantity > 0) 0 else 1)
                            }
                            onDismiss()
                        }
                    ) {
                        Text(
                            when {
                                custom -> "Add ${chosen.size} ${if (propertyMode) "option" else "service"}${if (chosen.size == 1) "" else "s"}"
                                pack != null && packageSelected -> "Already in cart"
                                pack != null -> if (propertyMode) "Add collection" else "Add package"
                                quantity > 0 -> "Remove"
                                else -> if (propertyMode) "Add to enquiry" else "Add to cart"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageExperience(
    listing: ServiceListing,
    pack: ProviderServicePackage,
    available: List<ProviderServiceOffering>,
    custom: Boolean,
    chosen: MutableMap<String, Int>,
    existingQuantities: Map<String, Int>,
    propertyMode: Boolean,
    onCustomChanged: (Boolean) -> Unit,
    onResolveMedia: suspend (String) -> String?
) {
    ExperienceHero(
        title = pack.name,
        subtitle = pack.description,
        price = pack.packagePriceAmount,
        rating = listing.rating,
        providerName = listing.providerName,
        media = pack.mediaGallery.orEmpty().ifEmpty { listOfNotNull(pack.media) },
        onResolveMedia = onResolveMedia
    )
    ExperienceSection(if (propertyMode) "Choose property options" else "Choose your package") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PackageModeCard(Modifier.weight(1f), if (propertyMode) "Full collection" else "Complete package", "Everything included", !custom) { onCustomChanged(false) }
            PackageModeCard(Modifier.weight(1f), if (propertyMode) "Pick properties" else "Pick services", "Create your combination", custom) { onCustomChanged(true) }
        }
    }
    ExperienceSection(if (custom) if (propertyMode) "Select properties" else "Select services" else if (propertyMode) "Included properties" else "Included in this package") {
        if (custom) {
            Text(if (propertyMode) "Choose any combination. Each property can be selected once." else "Choose any combination. Each service can be selected once.", color = ExperienceMuted, style = MaterialTheme.typography.bodySmall)
            available.forEach { item ->
                val alreadyInCart = existingQuantities[item.id] == 1
                val selected = chosen[item.id] == 1
                PackageServiceChoice(
                    item = item,
                    selected = selected,
                    enabled = !alreadyInCart,
                    supportingText = if (alreadyInCart) "Already in cart" else null
                ) {
                    if (selected) chosen.remove(item.id) else chosen[item.id] = 1
                }
            }
            if (available.isEmpty()) Text("These services are currently available only as the complete package.", color = ExperienceMuted)
        } else {
            pack.items.forEach { PackageServiceChoice(it, selected = true, enabled = false) {} }
        }
    }
    if (pack.includedText.isNotBlank()) {
        ExperienceSection("Good to know") { ExperienceCallout("Included", pack.includedText, true) }
    }
    if (pack.excludedText.isNotBlank()) {
        ExperienceSection("Not included") { ExperienceCallout("Plan separately", pack.excludedText, false) }
    }
    ProviderConfidenceCard(listing)
}

@Composable
private fun OfferingExperience(
    listing: ServiceListing,
    offering: ProviderServiceOffering,
    showDuration: Boolean,
    propertyMode: Boolean,
    onResolveMedia: suspend (String) -> String?
) {
    ExperienceHero(
        title = offering.title,
        subtitle = offering.description,
        price = offering.priceAmount,
        durationMinutes = offering.durationMinutes.takeIf { showDuration },
        rating = listing.rating,
        providerName = listing.providerName,
        media = offering.mediaGallery.orEmpty().ifEmpty { listOfNotNull(offering.media) },
        onResolveMedia = onResolveMedia
    )
    ExperienceSection(if (propertyMode) "Property enquiry details" else "Designed for a clear booking") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ExperienceBenefit(Modifier.weight(1f), "01", if (propertyMode) "Listed" else "Upfront", if (propertyMode) "Provider's property price" else "Clear service price")
            ExperienceBenefit(Modifier.weight(1f), "02", if (propertyMode) "One option" else "Single unit", "No quantity confusion")
            ExperienceBenefit(Modifier.weight(1f), "03", "Protected", if (propertyMode) "Validated enquiry" else "Validated at checkout")
        }
    }
    if (offering.description.isNotBlank()) {
        ExperienceSection(if (propertyMode) "About this property" else "About this service") { Text(offering.description, color = ExperienceInk, lineHeight = 23.sp) }
    }
    val attributes = offering.attributeValues?.entrySet().orEmpty().mapNotNull { entry ->
        if (entry.key in setOf("photo_url", "image_url", "media_url") || entry.value.isJsonNull) null
        else experienceAttributeValue(entry.value).takeIf(String::isNotBlank)?.let { experienceAttributeLabel(entry.key) to it }
    }
    if (attributes.isNotEmpty()) {
        ExperienceSection(if (propertyMode) "Property specifications" else "Service specifications") {
            attributes.forEachIndexed { index, (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(label, Modifier.weight(1f), color = ExperienceMuted)
                    Text(value, Modifier.weight(1f), color = ExperienceInk, fontWeight = FontWeight.SemiBold)
                }
                if (index != attributes.lastIndex) HorizontalDivider(color = Color(0xFFE8ECEA))
            }
        }
    }
    ProviderConfidenceCard(listing)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExperienceHero(
    title: String,
    subtitle: String,
    price: Double,
    rating: Float,
    providerName: String,
    media: List<MediaAsset>,
    onResolveMedia: suspend (String) -> String?,
    durationMinutes: Int? = null
) {
    val carousel = remember(media) { media.distinctBy { it.id } }
    val pagerState = rememberPagerState(pageCount = { carousel.size })
    Surface(color = Color(0xFFFFF8EC)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (carousel.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp))) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        ExperienceHeroImage(carousel[page], title, onResolveMedia)
                    }
                    if (carousel.size > 1) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                            color = Color.Black.copy(alpha = 0.68f),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                "${pagerState.currentPage + 1}/${carousel.size}",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Text(title, color = ExperienceInk, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            if (subtitle.isNotBlank()) Text(subtitle, color = ExperienceMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Default.Star, null, tint = Color(0xFF185F46), modifier = Modifier.size(17.dp))
                Text(if (rating > 0f) String.format(Locale.US, "%.1f provider rating", rating) else "New provider", color = ExperienceInk, fontWeight = FontWeight.SemiBold)
                Text("by $providerName", color = ExperienceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(experiencePrice(price), color = ExperienceInk, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                durationMinutes?.takeIf { it > 0 }?.let {
                    Text("•", color = ExperienceMuted)
                    Text("$it min", color = ExperienceMuted, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ExperienceHeroImage(
    media: MediaAsset,
    title: String,
    onResolveMedia: suspend (String) -> String?
) {
    val fileId = remember(media.id) { media.fileIdFor("HERO") }
    var photo by remember(fileId) { mutableStateOf<String?>(null) }
    LaunchedEffect(fileId) { photo = fileId?.let { onResolveMedia(it) } }
    Box(Modifier.fillMaxSize().background(Color(0xFFE8EEEB)), contentAlignment = Alignment.Center) {
        photo?.let {
            AsyncImage(it, title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun ExperienceSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = ExperienceInk, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            content()
        }
    }
}

@Composable
private fun PackageModeCard(modifier: Modifier, title: String, caption: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) ExperienceTint else Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, if (selected) ExperienceAccent else Color(0xFFDDE3E0))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SelectionMark(selected)
                Text(title, color = ExperienceInk, fontWeight = FontWeight.Bold)
            }
            Text(caption, color = ExperienceMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PackageServiceChoice(
    item: ProviderServiceOffering,
    selected: Boolean,
    enabled: Boolean,
    supportingText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (selected) ExperienceTint else Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) Color(0xFFB8D9CD) else Color(0xFFE2E7E4))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionMark(selected)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, color = ExperienceInk, fontWeight = FontWeight.Bold)
                if (item.description.isNotBlank()) Text(item.description, color = ExperienceMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                supportingText?.let { Text(it, color = ExperienceAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
            }
            Text(experiencePrice(item.priceAmount), color = ExperienceInk, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    Box(
        Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (selected) ExperienceAccent else Color(0xFFE8ECEA)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun ExperienceBenefit(modifier: Modifier, number: String, title: String, caption: String) {
    Surface(modifier = modifier, color = ExperienceTint, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.heightIn(min = 116.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(31.dp).clip(RoundedCornerShape(9.dp)).background(ExperienceAccent), contentAlignment = Alignment.Center) {
                Text(number, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(title, color = ExperienceInk, fontWeight = FontWeight.Bold)
            Text(caption, color = ExperienceMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ExperienceCallout(title: String, body: String, positive: Boolean) {
    Surface(color = if (positive) ExperienceTint else Color(0xFFFFF7ED), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = if (positive) ExperienceAccent else Color(0xFF8A541D), fontWeight = FontWeight.Bold)
            Text(body, color = ExperienceInk, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun ProviderConfidenceCard(listing: ServiceListing) {
    ExperienceSection("Your service provider") {
        Surface(color = Color(0xFFF7F8F7), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(ExperienceAccent), contentAlignment = Alignment.Center) {
                    Text(listing.providerName.trim().take(1).uppercase(Locale.getDefault()), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(listing.providerName, color = ExperienceInk, fontWeight = FontWeight.ExtraBold)
                    Text(if (listing.rating > 0f) String.format(Locale.US, "Rated %.1f out of 5", listing.rating) else "New on Nestora", color = ExperienceMuted, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.Star, null, tint = Color(0xFF185F46))
            }
        }
        Text("Availability, location, and booking details are validated before checkout.", color = ExperienceMuted, style = MaterialTheme.typography.bodySmall)
    }
}
