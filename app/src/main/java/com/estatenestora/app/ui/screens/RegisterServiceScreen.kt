package com.estatenestora.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.data.model.AndroidBridgeResponse
import com.estatenestora.app.data.model.Category
import com.estatenestora.app.data.model.GeocodePlace
import com.estatenestora.app.data.model.ServiceAttributeTemplate
import com.estatenestora.app.data.model.ServiceType
import com.estatenestora.app.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip

/**
 * "Self Register" — the manual form path. Deliberately separate from Auto
 * Register (AutoRegisterScreen.kt): this never touches the AI conversation
 * state, it's a direct, one-shot create-listing call (REGISTER_SERVICE on the
 * backend, via NestoraRepository.registerService).
 *
 * After a service type is selected, the form fetches dynamic attribute
 * templates from the backend (GET_SERVICE_ATTRS) and renders appropriate
 * input widgets per attribute type:
 *   • text / url / email / phone  →  OutlinedTextField
 *   • number                      →  numeric OutlinedTextField
 *   • boolean                     →  Yes / No toggle chip pair
 *   • select                      →  single-choice ExposedDropdownMenu
 *   • multiselect                 →  scrollable FlowRow of FilterChips
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterServiceScreen(
    categories: List<Category>,
    onFetchServiceTypes: suspend (categorySlug: String) -> List<ServiceType>,
    onFetchAllServiceTypes: suspend () -> List<ServiceType>,
    onFetchServiceAttributes: suspend (serviceTypeSlug: String) -> List<ServiceAttributeTemplate>,
    onSubmit: suspend (
        categorySlug: String,
        serviceTypeSlug: String,
        basePrice: Double,
        locationDisplayName: String,
        city: String,
        description: String,
        collectedAttributes: Map<String, String>
    ) -> AndroidBridgeResponse?,
    onBack: () -> Unit,
    onSearchAddress: suspend (String, Double?, Double?) -> List<GeocodePlace>,
    onReverseGeocode: suspend (Double, Double) -> GeocodePlace?
) {
    val scope = rememberCoroutineScope()

    // ── Basic form state ─────────────────────────────────────────────────────
    var serviceName by remember { mutableStateOf("") }
    var priceRate by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedLat by remember { mutableStateOf(0.0) }
    var selectedLon by remember { mutableStateOf(0.0) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    // ── Category / service-type selection sheets ─────────────────────────────
    var showCategorySheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    var showServiceTypeSheet by remember { mutableStateOf(false) }
    var allServiceTypes by remember { mutableStateOf<List<ServiceType>>(emptyList()) }
    var serviceTypes by remember { mutableStateOf<List<ServiceType>>(emptyList()) }
    var selectedServiceType by remember { mutableStateOf<ServiceType?>(null) }
    var loadingServiceTypes by remember { mutableStateOf(false) }

    // Fetch the complete dataset once on initialization to prevent database load
    LaunchedEffect(Unit) {
        try {
            allServiceTypes = onFetchAllServiceTypes()
        } catch (e: Exception) {
            // fallback
        }
    }

    // ── Dynamic attributes ───────────────────────────────────────────────────
    var dynamicAttributes by remember { mutableStateOf<List<ServiceAttributeTemplate>>(emptyList()) }
    var loadingAttributes by remember { mutableStateOf(false) }
    // Collected values: key → string value (multiselect joins with ", ")
    val collectedValues = remember { mutableStateMapOf<String, String>() }

    // ── Submit state ─────────────────────────────────────────────────────────
    var isSubmitting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Fetch service types when category changes ────────────────────────────
    LaunchedEffect(selectedCategory, allServiceTypes) {
        val cat = selectedCategory
        selectedServiceType = null
        dynamicAttributes = emptyList()
        collectedValues.clear()
        if (cat != null) {
            if (allServiceTypes.isNotEmpty()) {
                serviceTypes = allServiceTypes.filter { it.categorySlug == cat.id }
            } else {
                loadingServiceTypes = true
                serviceTypes = onFetchServiceTypes(cat.id)
                loadingServiceTypes = false
            }
        } else {
            serviceTypes = emptyList()
        }
    }

    // ── Fetch attributes when service type changes ───────────────────────────
    LaunchedEffect(selectedServiceType) {
        val st = selectedServiceType
        dynamicAttributes = emptyList()
        collectedValues.clear()
        if (st != null) {
            loadingAttributes = true
            dynamicAttributes = onFetchServiceAttributes(st.slug)
            loadingAttributes = false
        }
    }

    // ── Location picker overlay ──────────────────────────────────────────────
    if (showLocationPicker) {
        MapLocationPickerScreen(
            initialAddress = address,
            onSearchAddress = onSearchAddress,
            onReverseGeocode = onReverseGeocode,
            onLocationConfirmed = { title, subtitle, lat, lon ->
                address = if (subtitle.isNotBlank()) "$title, $subtitle" else title
                selectedLat = lat ?: 0.0
                selectedLon = lon ?: 0.0
                showLocationPicker = false
            },
            onBack = { showLocationPicker = false }
        )
        return
    }

    // ── Theme colours ────────────────────────────────────────────────────────
    val mintColor = NestoraMint
    val mutedColor = NestoraTextMuted
    val borderActive = NestoraMint
    val borderIdle = Color(0xFFD4EFE6)
    val fieldShape = RoundedCornerShape(16.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = borderActive,
        unfocusedBorderColor = borderIdle
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FDFA))
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {

        // ── Success screen ───────────────────────────────────────────────────
        if (isSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFE8FAF4), Color(0xFFFFFFFF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(Color(0xFFD4EFE6), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(NestoraMint, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Surface(
                            color = Color(0xFFE8FAF4),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "LISTING LIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NestoraMintDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Registered Successfully!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D1A13),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Your service listing is now active and search-ready for users on Nestora.",
                            fontSize = 14.sp,
                            color = NestoraTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Return to Home", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
            return@Box
        }

        // ── Main form ────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0D1A13))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Register Your Service",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D1A13)
                    )
                }
                Text(
                    text = "List your professional services, flats, or technical jobs to find local customers instantly.",
                    fontSize = 14.sp,
                    color = NestoraTextMuted,
                    modifier = Modifier.padding(bottom = 24.dp),
                    lineHeight = 20.sp
                )
            }

            // ── 1. Business / Service Name ──────────────────────────────────
            item {
                SectionLabel("Basic Information")
                Spacer(Modifier.height(12.dp))
            }

            item {
                FormFieldWrapper(hint = "Provide your brand name or business moniker.") {
                    OutlinedTextField(
                        value = serviceName,
                        onValueChange = { serviceName = it },
                        label = { Text("Business / Service Name") },
                        placeholder = { Text("e.g. Apex Electricals") },
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = mintColor) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── 2. Category ─────────────────────────────────────────────────
            item {
                FormFieldWrapper(hint = "Select the general industry of your service.") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory?.let { "${it.emoji} ${it.name}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            placeholder = { Text("Select a category") },
                            leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, tint = mintColor) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { showCategorySheet = true }
                                )
                            },
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategorySheet = true }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── 3. Service Type ─────────────────────────────────────────────
            item {
                FormFieldWrapper(hint = "Select the specific service role or job type.") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedServiceType?.let { "${it.emoji} ${it.name}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Service Type") },
                            placeholder = {
                                Text(
                                    when {
                                        loadingServiceTypes -> "Loading..."
                                        else -> "Select a service type"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    tint = mintColor
                                )
                            },
                            trailingIcon = {
                                if (loadingServiceTypes) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = mintColor
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.clickable {
                                            showServiceTypeSheet = true
                                        }
                                    )
                                }
                            },
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showServiceTypeSheet = true
                                }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Dynamic Attributes section header ───────────────────────────
            if (loadingAttributes) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = mintColor
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Loading service details...", fontSize = 13.sp, color = mutedColor)
                    }
                }
            }

            if (dynamicAttributes.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionLabel("Service Details")
                    Text(
                        text = "Specific details for ${selectedServiceType?.name ?: "this service type"}.",
                        fontSize = 12.sp,
                        color = mutedColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // ── Dynamic Attribute widgets ───────────────────────────────────
            items(dynamicAttributes.size) { idx ->
                val attr = dynamicAttributes[idx]
                val currentValue = collectedValues[attr.key] ?: ""

                when (attr.inputType) {

                    // ── text / url / email / phone ──────────────────────────
                    "text", "url", "email", "phone" -> {
                        val kbType = when (attr.inputType) {
                            "url" -> KeyboardType.Uri
                            "email" -> KeyboardType.Email
                            "phone" -> KeyboardType.Phone
                            else -> KeyboardType.Text
                        }
                        FormFieldWrapper(
                            hint = if (attr.isRequired) "Required" else "Optional"
                        ) {
                            OutlinedTextField(
                                value = currentValue,
                                onValueChange = { collectedValues[attr.key] = it },
                                label = { Text(attr.displayLabel) },
                                placeholder = { Text(attr.hintText ?: attr.displayLabel) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = kbType,
                                    capitalization = if (kbType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                                    imeAction = ImeAction.Next
                                ),
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── number ──────────────────────────────────────────────
                    "number" -> {
                        FormFieldWrapper(
                            hint = if (attr.isRequired) "Required — numbers only" else "Optional — numbers only"
                        ) {
                            OutlinedTextField(
                                value = currentValue,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) collectedValues[attr.key] = it },
                                label = { Text(attr.displayLabel) },
                                placeholder = { Text(attr.hintText ?: "Enter a number") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── boolean ─────────────────────────────────────────────
                    "boolean" -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${attr.displayLabel}${if (attr.isRequired) " *" else ""}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0D1A13)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("Yes" to "true", "No" to "false").forEach { (label, value) ->
                                    val selected = currentValue == value
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            collectedValues[attr.key] = if (selected) "" else value
                                        },
                                        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = mintColor,
                                            selectedLabelColor = Color.White
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            selectedBorderColor = mintColor,
                                            borderColor = borderIdle
                                        )
                                    )
                                }
                            }
                            if (!attr.isRequired) {
                                Text("Optional", fontSize = 11.sp, color = mutedColor)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── select ──────────────────────────────────────────────
                    "select" -> {
                        val options = attr.options ?: emptyList()
                        if (options.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${attr.displayLabel}${if (attr.isRequired) " *" else ""}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0D1A13)
                                )
                                // Render as a compact chip grid for select options
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    options.forEach { option ->
                                        val selected = currentValue == option
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                collectedValues[attr.key] = if (selected) "" else option
                                            },
                                            label = { Text(option, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = mintColor,
                                                selectedLabelColor = Color.White
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = selected,
                                                selectedBorderColor = mintColor,
                                                borderColor = borderIdle
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = if (attr.isRequired) "Required — select one" else "Optional — select one",
                                    fontSize = 11.sp,
                                    color = mutedColor
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // ── multiselect ─────────────────────────────────────────
                    "multiselect" -> {
                        val options = attr.options ?: emptyList()
                        if (options.isNotEmpty()) {
                            // current value is a ", "-delimited list of selected options
                            val selectedSet = remember(currentValue) {
                                currentValue.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${attr.displayLabel}${if (attr.isRequired) " *" else ""}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0D1A13)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    options.forEach { option ->
                                        val selected = option in selectedSet
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                val updated = selectedSet.toMutableSet()
                                                if (selected) updated.remove(option) else updated.add(option)
                                                collectedValues[attr.key] = updated.joinToString(", ")
                                            },
                                            label = { Text(option, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = mintColor,
                                                selectedLabelColor = Color.White
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = selected,
                                                selectedBorderColor = mintColor,
                                                borderColor = borderIdle
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = if (attr.isRequired) "Required — select all that apply" else "Optional — select all that apply",
                                    fontSize = 11.sp,
                                    color = mutedColor
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }

            // ── Common fields separator ─────────────────────────────────────
            if (dynamicAttributes.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionLabel("Pricing & Location")
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                item { Spacer(Modifier.height(4.dp)) }
            }

            // ── 4. Starting Price ───────────────────────────────────────────
            item {
                FormFieldWrapper(hint = "Leave empty or enter your base consultation/service rate.") {
                    OutlinedTextField(
                        value = priceRate,
                        onValueChange = { priceRate = it },
                        label = { Text("Starting Price (₹) — optional") },
                        placeholder = { Text("e.g. 500") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "₹",
                                    color = mintColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── 5. Address (Map picker) ─────────────────────────────────────
            item {
                FormFieldWrapper(hint = "Select your service location on the map.") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLocationPicker = true }
                    ) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Address") },
                            placeholder = { Text("Tap to select on map") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Select address",
                                    tint = mintColor
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = borderIdle,
                                disabledTextColor = Color(0xFF0D1A13),
                                disabledLabelColor = Color(0xFF888888)
                            ),
                            shape = fieldShape
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── 6. Description ──────────────────────────────────────────────
            item {
                if (dynamicAttributes.isEmpty()) {
                    SectionLabel("Description")
                    Spacer(Modifier.height(12.dp))
                }
                FormFieldWrapper(hint = "Detail your services, timings, experience, and what you offer (min 10 characters).") {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Short Description") },
                        placeholder = { Text("Describe the services you offer...") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = mintColor) },
                        shape = fieldShape,
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Error message ───────────────────────────────────────────────
            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 13.sp,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Submit button ───────────────────────────────────────────────
            item {
                Button(
                    onClick = {
                        val cat = selectedCategory
                        val st = selectedServiceType
                        val parsedPrice = priceRate.trim().toDoubleOrNull()

                        // Validate required attributes
                        val missingRequired = dynamicAttributes.filter { attr ->
                            attr.isRequired && (collectedValues[attr.key].isNullOrBlank() ||
                                (attr.inputType == "boolean" && collectedValues[attr.key].isNullOrBlank()))
                        }

                        when {
                            serviceName.trim().length < 3 ->
                                errorMessage = "Business name must be at least 3 characters."
                            cat == null ->
                                errorMessage = "Please select a category."
                            st == null ->
                                errorMessage = "Please select a service type."
                            priceRate.trim().isNotEmpty() && (parsedPrice == null || parsedPrice <= 0.0) ->
                                errorMessage = "Please enter a valid positive number for the starting price."
                            address.isBlank() ->
                                errorMessage = "Please select your address on the map."
                            description.trim().length < 10 ->
                                errorMessage = "Please write a short description (min 10 characters)."
                            missingRequired.isNotEmpty() ->
                                errorMessage = "Please fill in: ${missingRequired.joinToString(", ") { it.displayLabel }}"
                            else -> {
                                errorMessage = null
                                isSubmitting = true
                                scope.launch {
                                    // Extract city from address string
                                    val parts = address.split(",").map { it.trim() }
                                    var extractedCity = ""
                                    if (parts.size >= 3) {
                                        val candidate = parts[parts.size - 3]
                                        if (candidate.none { it.isDigit() }) extractedCity = candidate
                                    }
                                    if (extractedCity.isBlank() && parts.size >= 2) {
                                        val candidate = parts[parts.size - 2]
                                        if (candidate.none { it.isDigit() }) extractedCity = candidate
                                    }
                                    if (extractedCity.isBlank()) extractedCity = parts.firstOrNull() ?: ""

                                    // Build collected attributes map (omit empty values)
                                    val attrs = collectedValues.filter { (_, v) -> v.isNotBlank() }

                                    val response = onSubmit(
                                        cat.id,
                                        st.slug,
                                        parsedPrice ?: 0.0,
                                        address.trim(),
                                        extractedCity,
                                        description.trim(),
                                        attrs
                                    )
                                    isSubmitting = false
                                    if (response != null && response.ok) {
                                        isSuccess = true
                                    } else {
                                        errorMessage = response?.reply
                                            ?: "Nestora did not receive all listing details. Please tap Register again; no listing was created."
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        if (isSubmitting) "Registering..." else "Register & List Service",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        // --- Selection Bottom Sheet Modals ---
        if (showCategorySheet) {
            SelectionBottomSheet(
                title = "Select Category",
                searchPlaceholder = "Search categories...",
                items = categories,
                itemToText = { it.name },
                itemToEmoji = { it.emoji },
                onItemSelected = { cat ->
                    selectedCategory = cat
                    showCategorySheet = false
                },
                onDismissRequest = { showCategorySheet = false }
            )
        }

        if (showServiceTypeSheet) {
            val groupLookup = remember(categories) { categories.associateBy { it.id } }
            val listToShow = remember(selectedCategory, allServiceTypes, serviceTypes) {
                if (selectedCategory != null) serviceTypes else allServiceTypes
            }
            SelectionBottomSheet(
                title = "Select Service Type",
                searchPlaceholder = "Search service types...",
                items = listToShow,
                itemToText = { it.name },
                itemToEmoji = { it.emoji },
                grouping = { st ->
                    groupLookup[st.categorySlug]?.name ?: "Other"
                },
                onItemSelected = { st ->
                    selectedServiceType = st
                    val parentCat = categories.find { it.id == st.categorySlug }
                    if (parentCat != null) {
                        selectedCategory = parentCat
                    }
                    showServiceTypeSheet = false
                },
                onDismissRequest = { showServiceTypeSheet = false }
            )
        }
    }
}

/** Small section label with a tinted left rule */
@Composable
private fun SectionLabel(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .background(NestoraMint, shape = RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NestoraMintDark,
            letterSpacing = 1.sp
        )
    }
}

/** Wraps a form field with a hint text below it */
@Composable
private fun FormFieldWrapper(hint: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        content()
        Text(
            text = hint,
            fontSize = 11.sp,
            color = NestoraTextMuted,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionBottomSheet(
    title: String,
    searchPlaceholder: String,
    items: List<T>,
    itemToText: (T) -> String,
    itemToEmoji: (T) -> String,
    onItemSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    grouping: ((T) -> String)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
        ) {
            // Sticky Header & Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1A13)
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF0D1A13)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Sticky Search Field with Magnifying glass icon placeholder
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(searchPlaceholder, color = Color(0xFF718096), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF718096)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NestoraMint,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp) // Touch target height >= 44px
                        .focusRequester(focusRequester)
                )
            }

            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            // Inner List Body (Scrollable and filtered entirely in client memory matching search text)
            val filteredItems = remember(searchQuery, items) {
                items.filter { itemToText(it).contains(searchQuery, ignoreCase = true) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (grouping != null) {
                    val grouped = filteredItems.groupBy(grouping)
                    grouped.forEach { (groupName, groupItems) ->
                        // Small non-clickable subsection dividers
                        item {
                            Text(
                                text = groupName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF718096),
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 8.dp)
                            )
                        }
                        items(groupItems) { item ->
                            SelectionItemRow(
                                emoji = itemToEmoji(item),
                                name = itemToText(item),
                                onClick = { onItemSelected(item) }
                            )
                        }
                    }
                } else {
                    items(filteredItems) { item ->
                        SelectionItemRow(
                            emoji = itemToEmoji(item),
                            name = itemToText(item),
                            onClick = { onItemSelected(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionItemRow(
    emoji: String,
    name: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp) // Strict minimum touch layout height of 48dp (Fitts's Law)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = Color(0xFFF8FAFC)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nest all leading icons inside a soft, low-opacity colored circular boundary badge backdrop
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NestoraMint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
                
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1A13)
                )
            }
            
            // Trailing right-aligned chevron arrow symbol (>)
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Select",
                tint = Color(0xFF718096),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
