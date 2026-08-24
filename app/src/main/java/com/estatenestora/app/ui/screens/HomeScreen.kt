package com.estatenestora.app.ui.screens

import com.estatenestora.app.ui.components.ProjectFooter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.data.model.Category
import com.estatenestora.app.data.model.ServiceListing
import com.estatenestora.app.ui.theme.*

// Client-side name→image lookup, kept intentionally instead of a backend
// migration (new `image_url` column + API field): categories/services are a
// small, curator-controlled catalog (~50 entries) that changes rarely, so a
// static map costs nothing at runtime — Coil caches these CDN URLs after
// first load, so scrolling is instant with no extra API round-trip. A
// backend-driven field would only pay off once non-technical staff need to
// swap art without an app release, or once a second client (iOS/web) needs
// the same mapping — at that point, move this table server-side and have
// Category/ServiceListing carry an `imageUrl`, with this function staying
// on as the fallback for anything the API leaves blank.
fun getRealLifeImageUrl(key: String): String {
    val clean = key.lowercase().replace("&", "and").replace("_", " ").trim()
    return when {
        // --- CATEGORIES ---
        clean.contains("housekeeper") || clean.contains("maintenance") -> "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=320&h=320&fit=crop&q=80"
        clean.contains("cleaning") || clean.contains("household") -> "https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?w=320&h=320&fit=crop&q=80"
        clean.contains("transport") || clean.contains("logistics") -> "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?w=320&h=320&fit=crop&q=80"
        clean.contains("commuting") || clean.contains("cab") -> "https://images.unsplash.com/photo-1549317661-bd32c8ce0db2?w=320&h=320&fit=crop&q=80"
        clean.contains("construction") || clean.contains("interior") -> "https://images.unsplash.com/photo-1503387873255-3a4a234588c7?w=320&h=320&fit=crop&q=80"
        clean.contains("digital") || clean.contains("technical") || clean.contains("computer") -> "https://images.unsplash.com/photo-1588508065123-287b28e013da?w=320&h=320&fit=crop&q=80"
        clean.contains("vehicle") -> "https://images.unsplash.com/photo-1486006920555-c77dce18193b?w=320&h=320&fit=crop&q=80"
        clean.contains("property") || clean.contains("real estate") -> "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=320&h=320&fit=crop&q=80"
        clean.contains("food") || clean.contains("event") -> "https://images.unsplash.com/photo-1555244162-803834f70033?w=320&h=320&fit=crop&q=80"
        clean.contains("personal") || clean.contains("professional") -> "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=320&h=320&fit=crop&q=80"
        clean.contains("health") || clean.contains("wellness") || clean.contains("medical") -> "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=320&h=320&fit=crop&q=80"

        // --- HOUSEKEEPER & MAINTENANCE ---
        clean == "plumber" || clean.contains("plumb") -> "https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=600&h=800&fit=crop&q=80"
        clean == "electrician" || clean.contains("electric") -> "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=600&h=800&fit=crop&q=80"
        clean == "ac technician" || clean == "ac" -> "https://images.unsplash.com/photo-1621905251918-48416bd8575a?w=320&h=320&fit=crop&q=80"
        clean == "ro repair technician" -> "https://images.unsplash.com/photo-1585837575652-267c041d77d4?w=320&h=320&fit=crop&q=80"
        clean == "carpenter" -> "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=320&h=320&fit=crop&q=80"
        clean == "painter" -> "https://images.unsplash.com/photo-1562259949-e8e7689d7828?w=320&h=320&fit=crop&q=80"
        clean == "tv repair" -> "https://images.unsplash.com/photo-1595935736128-db120a26a2eb?w=320&h=320&fit=crop&q=80"
        clean == "fridge repair" -> "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=320&h=320&fit=crop&q=80"
        clean == "washing machine repair" -> "https://images.unsplash.com/photo-1626806787461-102c1bfaaea1?w=320&h=320&fit=crop&q=80"
        clean == "water tank cleaner" -> "https://images.unsplash.com/photo-1508962914676-134849a727f0?w=320&h=320&fit=crop&q=80"

        // --- CLEANING & HOUSEHOLD ---
        clean == "house cleaner" -> "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=600&h=800&fit=crop&q=80"
        clean == "deep cleaning" -> "https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?w=320&h=320&fit=crop&q=80"
        clean == "bathroom cleaning" -> "https://images.unsplash.com/photo-1600585154526-990dced4db0d?w=320&h=320&fit=crop&q=80"
        clean == "maid service" || clean == "maid" || clean.contains("maid") -> "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=600&h=800&fit=crop&q=80"
        clean == "cook" -> "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=320&h=320&fit=crop&q=80"
        clean == "babysitter" -> "https://images.unsplash.com/photo-1502086223501-7ea6ecd79368?w=320&h=320&fit=crop&q=80"
        clean == "elder care" -> "https://images.unsplash.com/photo-1576765608535-5f04d1e3f289?w=320&h=320&fit=crop&q=80"
        clean == "patient care" -> "https://images.unsplash.com/photo-1584515980181-12ecbfa12d8a?w=320&h=320&fit=crop&q=80"
        clean == "laundry service" || clean == "laundry" || clean.contains("laundry") -> "https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?w=600&h=800&fit=crop&q=80"
        clean == "ironing service" -> "https://images.unsplash.com/photo-1489274495757-95c7c837b101?w=320&h=320&fit=crop&q=80"

        // --- TRANSPORT & LOGISTICS & COMMUTING ---
        clean == "packers movers" -> "https://images.unsplash.com/photo-1528698827591-e19ccd7bc23d?w=320&h=320&fit=crop&q=80"
        clean == "delivery service" -> "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=320&h=320&fit=crop&q=80"
        clean == "truck rental" -> "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?w=320&h=320&fit=crop&q=80"
        clean == "bike transport" -> "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=320&h=320&fit=crop&q=80"
        clean == "auto driver" -> "https://images.unsplash.com/photo-1609137144813-1497931448b4?w=320&h=320&fit=crop&q=80"
        clean == "cab driver" -> "https://images.unsplash.com/photo-1494859814649-3f05927d9715?w=320&h=320&fit=crop&q=80"
        clean == "courier service" -> "https://images.unsplash.com/photo-1566576912321-d58ddd7a6088?w=320&h=320&fit=crop&q=80"
        clean == "bike" -> "https://images.unsplash.com/photo-1558981806-ec527fa84c39?w=320&h=320&fit=crop&q=80"

        // --- CONSTRUCTION & INTERIOR ---
        clean == "interior designer" -> "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=320&h=320&fit=crop&q=80"
        clean == "architect" -> "https://images.unsplash.com/photo-1503387873255-3a4a234588c7?w=320&h=320&fit=crop&q=80"
        clean == "false ceiling worker" -> "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=320&h=320&fit=crop&q=80"
        clean == "modular kitchen expert" -> "https://images.unsplash.com/photo-1556912173-3bb406ef7e77?w=320&h=320&fit=crop&q=80"
        clean == "glass worker" -> "https://images.unsplash.com/photo-1618220179428-22790b461013?w=320&h=320&fit=crop&q=80"
        clean == "furniture maker" -> "https://images.unsplash.com/photo-1618219908412-a29a1bb7b86e?w=320&h=320&fit=crop&q=80"
        clean == "flooring expert" -> "https://images.unsplash.com/photo-1581858726788-75bc0f6a952d?w=320&h=320&fit=crop&q=80"

        // --- DIGITAL & TECHNICAL ---
        clean == "laptop repair" -> "https://images.unsplash.com/photo-1588508065123-287b28e013da?w=320&h=320&fit=crop&q=80"
        clean == "mobile repair" -> "https://images.unsplash.com/photo-1597740985671-2a8a3b80f02e?w=320&h=320&fit=crop&q=80"
        clean == "wifi installation" -> "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=320&h=320&fit=crop&q=80"
        clean == "printer repair" -> "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=320&h=320&fit=crop&q=80"
        clean == "cctv monitoring" -> "https://images.unsplash.com/photo-1557597774-9d273605dfa9?w=320&h=320&fit=crop&q=80"
        clean == "graphic designer" -> "https://images.unsplash.com/photo-1626785774573-4b799315345d?w=320&h=320&fit=crop&q=80"
        clean == "photographer" -> "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=320&h=320&fit=crop&q=80"
        clean == "video editor" -> "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=320&h=320&fit=crop&q=80"

        // --- VEHICLE ---
        clean == "mechanic" -> "https://images.unsplash.com/photo-1486006920555-c77dce18193b?w=320&h=320&fit=crop&q=80"
        clean == "car repair wash" -> "https://images.unsplash.com/photo-1607860108855-64acf2078ed9?w=320&h=320&fit=crop&q=80"
        clean == "bike repair" -> "https://images.unsplash.com/photo-1558981852-426c6c22a09a?w=320&h=320&fit=crop&q=80"
        clean == "garage service" -> "https://images.unsplash.com/photo-1617886903355-9354bb57751f?w=320&h=320&fit=crop&q=80"
        clean == "driver service" -> "https://images.unsplash.com/photo-1549317661-bd32c8ce0db2?w=320&h=320&fit=crop&q=80"

        // --- REAL ESTATE ---
        clean == "property dealer" -> "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=600&h=800&fit=crop&q=80"
        clean == "rental agent" -> "https://images.unsplash.com/photo-1560520031-3a4dc4e9de0c?w=320&h=320&fit=crop&q=80"
        clean == "pg provider" -> "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?w=320&h=320&fit=crop&q=80"
        clean == "hostel provider" -> "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?w=320&h=320&fit=crop&q=80"
        clean == "flat owner" || clean.contains("flat owner") || clean.contains("flat") -> "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=600&h=800&fit=crop&q=80"
        clean == "broker" || clean.contains("broker") -> "https://images.unsplash.com/photo-1560518883-ce09059eeffa?w=600&h=800&fit=crop&q=80"

        // --- FOOD & EVENT ---
        clean == "food mess" -> "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=320&h=320&fit=crop&q=80"
        clean == "restaurant" -> "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=320&h=320&fit=crop&q=80"
        clean == "caterer" -> "https://images.unsplash.com/photo-1555244162-803834f70033?w=320&h=320&fit=crop&q=80"
        clean == "event decorator" -> "https://images.unsplash.com/photo-1478812954026-9c750f0e89fc?w=320&h=320&fit=crop&q=80"
        clean == "dj service" -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=320&h=320&fit=crop&q=80"
        clean == "wedding planner" -> "https://images.unsplash.com/photo-1519741497674-611481863552?w=320&h=320&fit=crop&q=80"
        clean == "tent house" -> "https://images.unsplash.com/photo-1533900298318-6b8da08a523e?w=320&h=320&fit=crop&q=80"
        clean == "canteen" -> "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=320&h=320&fit=crop&q=80"

        // --- PERSONAL & TUTOR ---
        clean == "tutor" -> "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=320&h=320&fit=crop&q=80"
        clean == "fitness trainer" -> "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=320&h=320&fit=crop&q=80"
        clean == "yoga trainer" -> "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=320&h=320&fit=crop&q=80"
        clean == "dance teacher" -> "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=320&h=320&fit=crop&q=80"
        clean == "music teacher" -> "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=320&h=320&fit=crop&q=80"
        clean == "beautician" -> "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=320&h=320&fit=crop&q=80"
        clean == "barber" -> "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=320&h=320&fit=crop&q=80"
        clean == "mehendi artist" -> "https://images.unsplash.com/photo-1610030469668-93535c17b6b3?w=320&h=320&fit=crop&q=80"
        clean == "tailor" -> "https://images.unsplash.com/photo-1528570798076-59d407897f7f?w=320&h=320&fit=crop&q=80"

        // --- HEALTH & WELLNESS ---
        clean == "physiotherapist" -> "https://images.unsplash.com/photo-1576091160550-2173dba999ef?w=320&h=320&fit=crop&q=80"
        clean == "nurse" -> "https://images.unsplash.com/photo-1576765608535-5f04d1e3f289?w=320&h=320&fit=crop&q=80"
        clean == "doctor appointment" -> "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=320&h=320&fit=crop&q=80"
        clean == "ambulance service" -> "https://images.unsplash.com/photo-1587560699334-cc4ff634909a?w=320&h=320&fit=crop&q=80"
        clean == "lab test service" -> "https://images.unsplash.com/photo-1579154341098-e4e158cc7f55?w=320&h=320&fit=crop&q=80"
        clean == "medicine delivery" -> "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=320&h=320&fit=crop&q=80"

        // --- EXTRA BROAD MATCHERS for slugs that may vary ---
        clean.contains("tb repair") || clean.contains("tv repair") || clean.contains("tuberculosis") -> "https://images.unsplash.com/photo-1595935736128-db120a26a2eb?w=320&h=320&fit=crop&q=80"
        clean.contains("patient care") || clean.contains("patient") -> "https://images.unsplash.com/photo-1631815588090-d4bfec5b1ccb?w=320&h=320&fit=crop&q=80"
        clean.contains("laundry") -> "https://images.unsplash.com/photo-1545173168-9f1947e8017e?w=320&h=320&fit=crop&q=80"
        clean.contains("auto driver") || clean.contains("auto rickshaw") || clean.contains("autodriver") -> "https://images.unsplash.com/photo-1609137144813-1497931448b4?w=320&h=320&fit=crop&q=80"
        clean.contains("courier") -> "https://images.unsplash.com/photo-1566576912321-d58ddd7a6088?w=320&h=320&fit=crop&q=80"
        clean.contains("interior design") || clean.contains("interior") -> "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=320&h=320&fit=crop&q=80"
        clean.contains("architect") -> "https://images.unsplash.com/photo-1503387873255-3a4a234588c7?w=320&h=320&fit=crop&q=80"
        clean.contains("mobile repair") || clean.contains("phone repair") -> "https://images.unsplash.com/photo-1597740985671-2a8a3b80f02e?w=320&h=320&fit=crop&q=80"
        clean.contains("tailor") || clean.contains("stitching") -> "https://images.unsplash.com/photo-1528570798076-59d407897f7f?w=320&h=320&fit=crop&q=80"
        clean.contains("mehendi") || clean.contains("henna") -> "https://images.unsplash.com/photo-1610030469668-93535c17b6b3?w=320&h=320&fit=crop&q=80"
        clean.contains("mechanic") -> "https://images.unsplash.com/photo-1530046339160-ce3e530c7d2f?w=320&h=320&fit=crop&q=80"
        clean.contains("garage") -> "https://images.unsplash.com/photo-1617886903355-9354bb57751f?w=320&h=320&fit=crop&q=80"
        clean.contains("bike repair") -> "https://images.unsplash.com/photo-1558981852-426c6c22a09a?w=320&h=320&fit=crop&q=80"
        clean.contains("bike") -> "https://images.unsplash.com/photo-1558981806-ec527fa84c39?w=320&h=320&fit=crop&q=80"
        clean.contains("plumb") -> "https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=320&h=320&fit=crop&q=80"
        clean.contains("electric") -> "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=320&h=320&fit=crop&q=80"
        clean.contains("carpenter") || clean.contains("wood") -> "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=320&h=320&fit=crop&q=80"
        clean.contains("paint") -> "https://images.unsplash.com/photo-1562259949-e8e7689d7828?w=320&h=320&fit=crop&q=80"
        clean.contains("ac") || clean.contains("air condition") -> "https://images.unsplash.com/photo-1621905251918-48416bd8575a?w=320&h=320&fit=crop&q=80"
        clean.contains("tutor") || clean.contains("teacher") -> "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=320&h=320&fit=crop&q=80"
        clean.contains("nurse") || clean.contains("nursing") -> "https://images.unsplash.com/photo-1576765608535-5f04d1e3f289?w=320&h=320&fit=crop&q=80"
        clean.contains("maid") || clean.contains("domestic") -> "https://images.unsplash.com/photo-1563453392212-326f5e854473?w=320&h=320&fit=crop&q=80"
        clean.contains("cook") || clean.contains("chef") -> "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=320&h=320&fit=crop&q=80"
        clean.contains("flat") || clean.contains("apartment") || clean.contains("rent") -> "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=320&h=320&fit=crop&q=80"
        clean.contains("packer") || clean.contains("mover") -> "https://images.unsplash.com/photo-1528698827591-e19ccd7bc23d?w=320&h=320&fit=crop&q=80"

        // --- FALLBACKS ---
        else -> "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=320&h=320&fit=crop&q=80"
    }
}

fun getRealLifeImageModel(key: String): Any {
    val clean = key.lowercase().replace("&", "and").replace("_", " ").trim()
    return when {
        clean == "plumber" || clean.contains("plumb") -> com.estatenestora.app.R.drawable.plumber
        clean == "maid service" || clean == "maid" || clean.contains("maid") -> com.estatenestora.app.R.drawable.maid
        clean == "electrician" || clean.contains("electric") -> com.estatenestora.app.R.drawable.electrician
        clean == "broker" || clean.contains("broker") -> com.estatenestora.app.R.drawable.broker
        clean == "flat owner" || clean.contains("flat owner") || clean.contains("flat") -> com.estatenestora.app.R.drawable.flat_owner
        clean == "laundry service" || clean == "laundry" || clean.contains("laundry") -> com.estatenestora.app.R.drawable.laundry_service
        else -> getRealLifeImageUrl(key)
    }
}

data class QuickFilter(val id: String, val label: String, val icon: String)

data class PopularServiceItem(val label: String, val subtitle: String, val query: String)

// ── POPULAR SERVICE FULL-BLEED PORTRAIT CARD (Large 175dp x 230dp Edge-to-Edge Format) ──
@Composable
fun PopularServiceCard(
    service: PopularServiceItem,
    imageModel: Any,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .width(175.dp)
            .height(230.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Full-bleed local/remote image filling 100% of card area
            AsyncImage(
                model = imageModel,
                contentDescription = service.label,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Subtle top gradient scrim for text contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.78f),
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )

            // 3. Text directly overlaying top-left (matching media_1787500716781.png / media_1787501843099.png)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = service.label,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = service.subtitle,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                )
            }
        }
    }
}

// Neutral placeholder shown behind card art while the image loads (or if it
// fails) — keeps the grid visually stable instead of flashing white/blank.
private val cardPlaceholderColor = Color(0xFFF0F4F2)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    categories: List<Category>,
    listings: List<ServiceListing> = emptyList(),
    onListingClick: (ServiceListing) -> Unit = {},
    onSearchClick: () -> Unit,
    onSendTelegramMessage: (String) -> Unit = {},
    onSendTelegramSupport: () -> Unit = {},
    onBookViaTelegram: (ServiceListing) -> Unit = {},
    onCategorySelected: (Category) -> Unit = {},
    onSeeAllCategoriesClick: () -> Unit = {},
    currentLocation: String? = null,
    onSelectLocationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onRegisterServiceClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onScrollChanged: (Boolean) -> Unit = {},
    userPhotoPath: String? = null,
    isProviderMode: Boolean = false,
    onModeToggle: () -> Unit = {},
    tabsList: List<com.estatenestora.app.ui.theme.NestoraTab> = emptyList(),
    selectedTabId: String = "explore",
    onTabSelected: (String) -> Unit = {},
    currentTheme: com.estatenestora.app.ui.theme.RoyalTheme = com.estatenestora.app.ui.theme.RoyalThemeRepository.getThemeForToday(),
    isLoadingFeed: Boolean = false,
    onRefreshFeed: () -> Unit = {},
    recentlyViewedServices: List<com.estatenestora.app.data.model.ServiceType> = emptyList(),
    allServiceTypes: List<com.estatenestora.app.data.model.ServiceType> = emptyList(),
    onServiceTypeClick: (com.estatenestora.app.data.model.ServiceType) -> Unit = {}
) {
    val pageSurface = remember(currentTheme) { selectedMenuSurface(currentTheme) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var aiInputText by remember { mutableStateOf("") }
    val activeSearchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun closeActiveSearch(openFinder: Boolean = false) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        isSearchFocused = false
        if (openFinder) onSearchClick()
    }

    LaunchedEffect(isSearchFocused) {
        if (isSearchFocused) activeSearchFocusRequester.requestFocus()
    }

    val listState = rememberLazyListState()
    // Keep the carousel directly behind the active card instead of leaving
    // the top navigation beneath it. The sticky resting bar is covered by the
    // floating surface, while the hero starts under its rounded lower edge.
    LaunchedEffect(isSearchFocused) {
        if (isSearchFocused) listState.animateScrollToItem(1)
    }
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    LaunchedEffect(isScrolled) {
        onScrollChanged(isScrolled)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── TOP HEADER (Dark Teal-Green Gradient matching Swiggy Layout) ────
            item {
                OnboardingTopBar(
                    currentLocation = currentLocation,
                    onSelectLocationClick = onSelectLocationClick,
                    onProfileClick = onProfileClick,
                    userPhotoPath = userPhotoPath,
                    isProviderMode = isProviderMode,
                    onModeToggle = onModeToggle,
                    tabsList = tabsList,
                    selectedTabId = selectedTabId,
                    onTabSelected = onTabSelected,
                    currentTheme = currentTheme
                )
            }

        if (!isSearchFocused) {
            stickyHeader {
                OnboardingSearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    isScrolled = isScrolled,
                    hasCarouselBelow = true,
                    onClick = { isSearchFocused = true },
                    currentTheme = currentTheme
                )
            }
        }

        // ── SWIGGY-STYLE INTEGRATED HERO CAROUSEL ────────────────────────────
        item {
            HeroCarousel(theme = "explore", canvasColor = pageSurface)
        }

        // ── POPULAR SERVICES CAROUSEL ─────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                    .background(pageSurface)
                    .padding(vertical = 12.dp)
            ) {
                val services = remember {
                    listOf(
                        PopularServiceItem("Plumbers", "Expert Fixes", "plumber"),
                        PopularServiceItem("Maids", "Daily House Help", "maid"),
                        PopularServiceItem("Electricians", "Wiring & Repair", "electrician"),
                        PopularServiceItem("Brokers", "Verified Agents", "broker"),
                        PopularServiceItem("Flat Owners", "Direct Rentals", "flat owner"),
                        PopularServiceItem("Laundry Service", "Wash & Fold", "laundry service")
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(services) { svc ->
                        PopularServiceCard(
                            service = svc,
                            imageModel = getRealLifeImageModel(svc.query),
                            onClick = {
                                onServiceTypeClick(
                                    com.estatenestora.app.data.model.ServiceType(
                                        slug = svc.query,
                                        name = svc.label,
                                        emoji = "",
                                        description = "",
                                        categorySlug = ""
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // ── YOU RECENTLY VIEWED STRIP ───────────────────────────────────────
        val activeRecentlyViewed = recentlyViewedServices.filter { it.isActive }
        if (activeRecentlyViewed.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = "You recently viewed",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D1A13),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(activeRecentlyViewed) { svc ->
                            SwiggyStyleCard(
                                label = svc.name,
                                imageUrl = svc.slug.ifBlank { svc.name },
                                onClick = { onServiceTypeClick(svc) }
                            )
                        }
                    }
                }
            }
        }

        // ── PER-CATEGORY SECTIONS (SS1 PATTERN) ──────────────────────────────
        categories.filter { it.isActive }.forEach { cat ->
            val catServices = allServiceTypes.filter { it.isActive && it.categorySlug == cat.id }
                .ifEmpty { getFallbackServicesForCategory(cat.id, cat.name) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = cat.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D1A13),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(catServices) { svc ->
                            SwiggyStyleCard(
                                label = svc.name,
                                imageUrl = svc.slug.ifBlank { svc.name },
                                onClick = { onServiceTypeClick(svc) }
                            )
                        }
                    }
                }
            }
        }

        // Restored from main: active provider cards remain directly bookable
        // from Explore, while the newer category-first layout stays intact.
        if (listings.isNotEmpty() || isLoadingFeed) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(top = 14.dp, bottom = 6.dp)
                ) {
                    Text(
                        text = "Featured service providers",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D1A13),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = "Available near your selected location",
                        fontSize = 11.sp,
                        color = NestoraTextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                    if (isLoadingFeed) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            color = NestoraMint
                        )
                    }
                }
            }
            items(listings.take(12), key = ServiceListing::id) { listing ->
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    MarketplaceListingCard(
                        listing = listing,
                        onClick = { onListingClick(listing) },
                        onBookViaTelegram = { onBookViaTelegram(listing) }
                    )
                }
            }
        } else if (!isLoadingFeed) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF7FAF8),
                    border = BorderStroke(1.dp, Color(0xFFE2EBE5))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚡ Looking for specific help?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1A13)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap any service above or chat with AI to match with top providers instantly.",
                            fontSize = 12.sp,
                            color = NestoraTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onSearchClick,
                                colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Chat with Nestora AI", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = onRefreshFeed,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, NestoraMint),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Refresh Feed", color = NestoraMint, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
        item { ProjectFooter() }
    }


    // ── Active Search Overlay ──
    if (isSearchFocused) {
        val homeOverlayContext = androidx.compose.ui.platform.LocalContext.current
        DisposableEffect(isSearchFocused) {
            val window = (homeOverlayContext as? android.app.Activity)?.window
            if (window != null && isSearchFocused) {
                val oldColor = window.statusBarColor
                window.statusBarColor = android.graphics.Color.WHITE
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                val oldLightStatusBars = insetsController.isAppearanceLightStatusBars
                insetsController.isAppearanceLightStatusBars = true
                
                onDispose {
                    window.statusBarColor = oldColor
                    insetsController.isAppearanceLightStatusBars = oldLightStatusBars
                }
            } else {
                onDispose {}
            }
        }
        // Dim backdrop overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { closeActiveSearch() }
        )

        // Floating Search Card at the top (overlaps the top portion of HeroCarousel)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Top Title / Navigation Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Left side: Thin black back arrow icon (←)
                    IconButton(
                        onClick = { closeActiveSearch() },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF2A2A2A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center/Title: Centered, clean dark text
                    Text(
                        text = "Search for services & providers",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2A2A2A),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(12.dp))

                // The overlay owns the same query as the resting search bar;
                // closing it never loses what the provider/customer typed.
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFFE0E0E0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Input Box Area (No magnifying glass icon on the left)
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text("Try 'Plumber'", color = Color(0xFF9E9E9E), fontSize = 13.sp)
                                    Text("Try 'Maid'", color = Color(0xFF9E9E9E).copy(alpha = 0.6f), fontSize = 10.sp)
                                }
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    color = Color(0xFF0D1A13)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(activeSearchFocusRequester),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { closeActiveSearch(openFinder = searchQuery.isNotBlank()) }
                                )
                            )
                        }

                        // Right side: vertical divider line and orange microphone icon
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(Color(0xFFEAEAEA))
                                .padding(vertical = 12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        MicIcon(
                            color = Color(0xFFFF5722),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
}

// ─── CATEGORY CARD ───────────────────────────────────────────────────────────

@Composable
fun CategoryCard(
    category: Category,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2EAF2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Text at the top left
            Text(
                text = category.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D1A13),
                lineHeight = 15.sp,
                maxLines = 2
            )
            
            // Image at the bottom right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                AsyncImage(
                    model = getRealLifeImageUrl(if (category.emoji.isBlank()) category.name else category.emoji),
                    contentDescription = category.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}
// ── SWIGGY-STYLE CARD: white square bg, image fills top, label wraps below ────
@Composable
fun SwiggyStyleCard(
    label: String,
    imageUrl: String,
    onClick: () -> Unit,
    imageSize: androidx.compose.ui.unit.Dp = 80.dp
) {
    val model = remember(imageUrl, label) {
        getRealLifeImageModel(if (imageUrl.isNotBlank()) imageUrl else label)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(imageSize)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(imageSize)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = model,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF222222),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Legacy ImageLabelCard kept for any remaining callers
@Composable
fun ImageLabelCard(
    label: String,
    imageUrl: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 78.dp
) {
    SwiggyStyleCard(label = label, imageUrl = imageUrl, onClick = onClick, imageSize = size)
}
// ─── LISTING CARD ────────────────────────────────────────────────────────────

@Composable
fun MarketplaceListingCard(
    listing: ServiceListing,
    onClick: () -> Unit,
    onBookViaTelegram: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listing.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1A13)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${listing.providerName} • ${listing.serviceType}",
                        fontSize = 12.sp,
                        color = NestoraTextMuted
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = NestoraMint, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(listing.location, fontSize = 11.sp, color = NestoraTextMuted)
                    }
                }

                Spacer(Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF8E8)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = NestoraAmber, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(String.format("%.1f", listing.rating), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B6914))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Starting from", fontSize = 10.sp, color = NestoraTextMuted)
                    Text(
                        text = "₹${listing.price.toInt()}${if (listing.serviceType.contains("Flat")) " /mo" else ""}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NestoraMint
                    )
                }

                Button(
                    onClick = onBookViaTelegram,
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("Book Now ⚡", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun HighlightFeatureCard(
    label: String,
    highlight: String,
    emoji: String,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(110.dp)
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = colors[0],
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Top Label (e.g. "Up To 50% OFF")
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 14.sp
                )

                // Bottom section: large typography highlight or illustration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = emoji,
                        fontSize = 28.sp
                    )

                    Text(
                        text = highlight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = NestoraAmber,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// Fallback services per category (ss1 pattern) when server category service types aren't loaded yet
fun getFallbackServicesForCategory(categoryId: String, categoryName: String): List<com.estatenestora.app.data.model.ServiceType> {
    val cleanId = categoryId.lowercase().replace("&", "and").replace("_", " ").trim()
    val cleanName = categoryName.lowercase()

    fun createType(slug: String, name: String) = com.estatenestora.app.data.model.ServiceType(
        slug = slug,
        name = name,
        emoji = "",
        description = "",
        categorySlug = categoryId
    )

    return when {
        cleanId.contains("housekeeper") || cleanName.contains("housekeeper") || cleanName.contains("maintenance") -> listOf(
            createType("plumber", "Plumber"),
            createType("electrician", "Electrician"),
            createType("ac_technician", "AC Technician"),
            createType("ro_repair_technician", "RO Repair Technician"),
            createType("carpenter", "Carpenter"),
            createType("painter", "Painter"),
            createType("tv_repair", "TV Repair"),
            createType("fridge_repair", "Fridge Repair")
        )
        cleanId.contains("cleaning") || cleanName.contains("cleaning") || cleanName.contains("household") -> listOf(
            createType("house_cleaner", "House Cleaner"),
            createType("deep_cleaning", "Deep Cleaning"),
            createType("bathroom_cleaning", "Bathroom Cleaning"),
            createType("maid_service", "Maid Service"),
            createType("cook", "Cook"),
            createType("babysitter", "Babysitter"),
            createType("laundry_service", "Laundry Service")
        )
        cleanId.contains("transport") || cleanName.contains("transport") || cleanName.contains("logistics") -> listOf(
            createType("packers_movers", "Packers & Movers"),
            createType("delivery_service", "Delivery Service"),
            createType("truck_rental", "Truck Rental"),
            createType("bike_transport", "Bike Transport"),
            createType("auto_driver", "Auto Driver")
        )
        cleanId.contains("commuting") || cleanName.contains("commuting") || cleanName.contains("cab") -> listOf(
            createType("cab", "Cab"),
            createType("bike", "Bike"),
            createType("driver_service", "Driver Service")
        )
        cleanId.contains("digital") || cleanName.contains("digital") || cleanName.contains("technical") -> listOf(
            createType("laptop_repair", "Laptop Repair"),
            createType("mobile_repair", "Mobile Repair"),
            createType("wifi_installation", "Wifi Installation"),
            createType("graphic_designer", "Graphic Designer"),
            createType("photographer", "Photographer")
        )
        cleanId.contains("real_estate") || cleanName.contains("real estate") || cleanName.contains("property") -> listOf(
            createType("flat", "Flat / Apartment"),
            createType("pg_provider", "PG Provider"),
            createType("property_dealer", "Property Dealer"),
            createType("rental_agent", "Rental Agent")
        )
        else -> listOf(
            createType("tutor", "Tutor"),
            createType("beautician", "Beautician"),
            createType("fitness_trainer", "Fitness Trainer"),
            createType("physiotherapist", "Physiotherapist")
        )
    }
}
