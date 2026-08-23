package com.estatenestora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estatenestora.app.data.model.AndroidBridgeResponse
import androidx.compose.ui.res.painterResource
import com.estatenestora.app.R
import com.estatenestora.app.data.model.TelegramChatMessage
import com.estatenestora.app.data.model.ServiceType
import com.estatenestora.app.ui.theme.*
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.estatenestora.app.data.model.GeocodePlace
import com.estatenestora.app.util.findActivity
import com.estatenestora.app.util.getCurrentLocation
import com.estatenestora.app.util.hasLocationPermission
import com.estatenestora.app.util.isSystemLocationEnabled


/**
 * One catalog attribute as sent in aiso_summary.attributes — a JSON array
 * of {key, display_label, input_type, options, is_required, value} objects.
 * See [parseAisoAttributes] for why this is never read via raw
 * JsonObject.get(...).asX chains.
 */
private data class AisoAttributeField(
    val key: String,
    val displayLabel: String,
    val inputType: String,
    val options: List<String>,
    val isRequired: Boolean,
    val value: String
)

/**
 * Safely parses aiso_summary's "attributes" array — the actual root cause
 * of this screen's crash-on-every-registration bug: the backend used to
 * (in one of its two response builders) send "attributes" as a flat
 * key→value JSON *object* while this screen called
 * `JsonObject.getAsJsonArray("attributes")` expecting a JSON *array* —
 * Gson throws IllegalStateException on that type mismatch instead of
 * returning null, so it crashed the whole app the instant any registration
 * reached the "ready to confirm" step (i.e. for every service type, since
 * almost none have zero attribute templates). The backend shape is fixed
 * now (both builders funnel through one shared function), but this parser
 * stays defensive regardless — every field access is null/type-checked, and
 * a malformed individual element is skipped (via runCatching) rather than
 * aborting the whole list, so a *future* backend response shape change
 * degrades to "attribute missing from the form" instead of a hard crash.
 */
private fun parseAisoAttributes(summary: JsonObject?): List<AisoAttributeField> {
    val attributesElement = summary?.get("attributes") ?: return emptyList()
    if (!attributesElement.isJsonArray) return emptyList()
    return attributesElement.asJsonArray.mapNotNull { element ->
        runCatching {
            if (!element.isJsonObject) return@runCatching null
            val obj = element.asJsonObject
            val key = obj.get("key")?.takeIf { !it.isJsonNull }?.asString ?: return@runCatching null
            AisoAttributeField(
                key = key,
                displayLabel = obj.get("display_label")?.takeIf { !it.isJsonNull }?.asString ?: key,
                inputType = obj.get("input_type")?.takeIf { !it.isJsonNull }?.asString ?: "text",
                options = obj.get("options")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { opt -> opt.takeIf { !it.isJsonNull }?.asString } ?: emptyList(),
                isRequired = obj.get("is_required")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
                value = obj.get("value")?.takeIf { !it.isJsonNull }?.asString ?: ""
            )
        }.getOrNull()
    }
}

/**
 * Recovers from a turn that didn't reach the client cleanly by resetting
 * the *server's* AISO session and telling the user plainly to start over —
 * see [AutoRegisterScreen]'s onReset doc for why a bare "please try again"
 * isn't safe here: the server may have already advanced past this exact
 * text (e.g. now waiting for a location answer) even though the client
 * never saw that response, so blindly retrying the same message would get
 * silently reinterpreted as an answer to a question the user was never
 * shown. onReset is best-effort (runCatching) — even if clearing the
 * server session also fails, the user still gets a clear "start over"
 * instruction instead of a confusing dead end.
 */
private suspend fun resetAfterFailedTurn(
    onReset: suspend () -> Unit,
    messages: androidx.compose.runtime.snapshots.SnapshotStateList<TelegramChatMessage>,
    timestamp: String
) {
    runCatching { onReset() }
    messages.add(
        TelegramChatMessage(
            id = "err-${System.currentTimeMillis()}",
            sender = "Nestora AI",
            text = "That last step didn't go through cleanly, so I've reset this conversation to be safe. Please describe your service again from the start — e.g. \"I'm a plumber in Newtown, I charge 500 per visit.\"",
            timestamp = timestamp,
            isUser = false
        )
    )
}

/**
 * "Auto Register" — free-text, LLM-driven onboarding, reusing the exact
 * chat-bubble UI (ChatMessageItem, from AIChatScreen.kt) but driving
 * AISO_PARSE/AISO_SAVE turn-by-turn instead of repository.chat(). This is a
 * SEPARATE conversation from the Telegram-chat "🤖 Auto Register" flow — see
 * the backend's androidAisoSessions doc for why the two never share state.
 */
@Composable
fun AutoRegisterScreen(
    onBack: () -> Unit,
    onParse: suspend (String) -> AndroidBridgeResponse?,
    onSave: suspend () -> AndroidBridgeResponse?,
    onUpdate: suspend (String) -> AndroidBridgeResponse?,
    onReverseGeocode: suspend (Double, Double) -> GeocodePlace?,
    messages: androidx.compose.runtime.snapshots.SnapshotStateList<TelegramChatMessage>,
    onClearChat: () -> Unit,
    onExploreClick: () -> Unit = {},
    onSelectLocationClick: () -> Unit = {},
    pendingMapLocationToSend: String? = null,
    onClearPendingMapLocation: () -> Unit = {},
    userPhotoPath: String? = null,
    onFetchAllServiceTypes: suspend () -> List<ServiceType> = { emptyList() },
    onReset: suspend () -> Unit = {},
    userName: String? = null
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var inputText by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var readyToConfirm by remember { mutableStateOf(false) }
    var isLocatingLocation by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<JsonObject?>(null) }
    var isDone by remember { mutableStateOf(false) }

    // Same permanently-denied / system-location-off handling as
    // MapLocationPickerScreen — this screen used to carry its own simpler,
    // drifted copy that only ever toasted and gave up, which is why "Share
    // My Location" here looked broken (denied once -> Android stops showing
    // its own permission dialog -> re-tapping silently no-ops) even though
    // the identical underlying problem was already solved in the map picker.
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showEnableLocationDialog by remember { mutableStateOf(false) }
    var permissionPermanentlyDenied by remember { mutableStateOf(false) }
    val activity = remember(context) { context.findActivity() }

    var isRejected by remember { mutableStateOf(false) }
    var activeErrorAlert by remember { mutableStateOf<String?>(null) }

    var editablePrice by remember { mutableStateOf("") }
    var editableLocation by remember { mutableStateOf("") }
    var editableDescription by remember { mutableStateOf("") }
    val editableAttributes = remember { mutableStateMapOf<String, String>() }

    var selectedServiceType by remember { mutableStateOf<com.estatenestora.app.data.model.ServiceType?>(null) }
    val isInputEnabled = selectedServiceType != null || messages.size > 1
    var serviceTypes by remember { mutableStateOf<List<com.estatenestora.app.data.model.ServiceType>>(emptyList()) }

    LaunchedEffect(Unit) {
        serviceTypes = try { onFetchAllServiceTypes() } catch(e: Exception) { emptyList() }
    }

    val fallbackServiceTypes = remember {
        listOf(
            com.estatenestora.app.data.model.ServiceType("plumber", "Plumber", "🔧", "Book plumber", "home_repairs"),
            com.estatenestora.app.data.model.ServiceType("electrician", "Electrician", "⚡", "Book electrician", "home_repairs"),
            com.estatenestora.app.data.model.ServiceType("ac_technician", "AC Repair", "❄️", "Book AC repair", "home_repairs"),
            com.estatenestora.app.data.model.ServiceType("maid_service", "Maid Service", "🧹", "Book maid", "housekeeping"),
            com.estatenestora.app.data.model.ServiceType("cook", "Cook", "🍳", "Book cook", "housekeeping"),
            com.estatenestora.app.data.model.ServiceType("flat_owner", "Flat Owner", "🏢", "List a flat", "rentals"),
            com.estatenestora.app.data.model.ServiceType("tutor", "Tutor", "🎓", "Book tutor", "education")
        )
    }

    fun getCurrentFormattedTime(): String {
        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun sendTurn(text: String, lat: Double? = null, lon: Double? = null) {
        if (text.isBlank() || isBusy) return
        val isFirstUserMsg = messages.count { it.isUser } == 0
        messages.add(TelegramChatMessage(id = "usr-${System.currentTimeMillis()}", sender = "You", text = text, timestamp = getCurrentFormattedTime(), isUser = true))
        isBusy = true
        scope.launch {
            // A coroutine launched via rememberCoroutineScope() has no
            // exception handler by default — anything thrown inside (a
            // malformed response, a parsing bug, a network hiccup) would
            // otherwise propagate up and crash the whole app instead of
            // just this one turn. This is the actual fix for "crashes when
            // I type anything" — every step routes through sendTurn(), so
            // an uncaught exception anywhere in this call chain took the
            // entire screen down with it.
            try {
                val baseText = if (selectedServiceType != null && isFirstUserMsg) {
                    "I want to register as a ${selectedServiceType!!.name}. $text"
                } else {
                    text
                }
                val queryText = if (lat != null && lon != null) "$baseText||$lat,$lon" else baseText
                val response = onParse(queryText)
                isBusy = false
                if (response == null) {
                    resetAfterFailedTurn(onReset, messages, getCurrentFormattedTime())
                    return@launch
                }
                messages.add(
                    TelegramChatMessage(
                        id = "bot-${System.currentTimeMillis()}",
                        sender = "Nestora AI",
                        text = response.reply,
                        timestamp = getCurrentFormattedTime(),
                        isUser = false,
                        aisoGap = response.aisoGap
                    )
                )
                if (response.intent == "aiso_policy_rejected") {
                    isRejected = true
                }
                readyToConfirm = response.intent == "aiso_ready_to_confirm"
                summary = if (readyToConfirm) response.aisoSummary else null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // structured concurrency: never swallow cancellation
            } catch (e: Exception) {
                isBusy = false
                resetAfterFailedTurn(onReset, messages, getCurrentFormattedTime())
            }
        }
    }

    LaunchedEffect(summary) {
        summary?.let { s ->
            editablePrice = s.get("base_price")?.takeIf { !it.isJsonNull }?.asString ?: ""
            editableLocation = s.get("location_display_name")?.takeIf { !it.isJsonNull }?.asString ?: ""
            editableDescription = s.get("description")?.takeIf { !it.isJsonNull }?.asString ?: ""

            editableAttributes.clear()
            parseAisoAttributes(s).forEach { field ->
                editableAttributes[field.key] = field.value
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    LaunchedEffect(pendingMapLocationToSend) {
        pendingMapLocationToSend?.let { fullText ->
            val parts = fullText.split("||")
            val displayName = parts.firstOrNull() ?: fullText
            
            messages.add(
                TelegramChatMessage(
                    id = "usr-${System.currentTimeMillis()}",
                    sender = "You",
                    text = displayName,
                    timestamp = getCurrentFormattedTime(),
                    isUser = true
                )
            )
            
            isBusy = true
            scope.launch {
                try {
                    val response = onParse(fullText)
                    isBusy = false
                    if (response == null) {
                        resetAfterFailedTurn(onReset, messages, getCurrentFormattedTime())
                        return@launch
                    }
                    messages.add(
                        TelegramChatMessage(
                            id = "bot-${System.currentTimeMillis()}",
                            sender = "Nestora AI",
                            text = response.reply,
                            timestamp = getCurrentFormattedTime(),
                            isUser = false,
                            aisoGap = response.aisoGap
                        )
                    )
                    if (response.intent == "aiso_policy_rejected") {
                        isRejected = true
                    }
                    readyToConfirm = response.intent == "aiso_ready_to_confirm"
                    summary = if (readyToConfirm) response.aisoSummary else null
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    isBusy = false
                    resetAfterFailedTurn(onReset, messages, getCurrentFormattedTime())
                }
            }
            onClearPendingMapLocation()
        }
    }

    fun locateAndSend() {
        if (isLocatingLocation) return
        isLocatingLocation = true
        val loadingId = "locating-${System.currentTimeMillis()}"
        messages.add(
            TelegramChatMessage(
                id = loadingId,
                sender = "Nestora AI",
                text = "⏳ Detecting your location...",
                timestamp = getCurrentFormattedTime(),
                isUser = false
            )
        )
        scope.launch {
            try {
                val loc = getCurrentLocation(context)
                val loadingIndex = messages.indexOfFirst { it.id == loadingId }
                if (loadingIndex >= 0) messages.removeAt(loadingIndex)

                if (loc != null) {
                    val place = onReverseGeocode(loc.latitude, loc.longitude)
                    isLocatingLocation = false
                    if (place != null) {
                        val fullAddress = "${place.title}, ${place.subtitle}"
                        sendTurn(fullAddress, loc.latitude, loc.longitude)
                    } else {
                        Toast.makeText(context, "Could not resolve the address. Use Choose on Map to set the service location.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    isLocatingLocation = false
                    Toast.makeText(
                        context,
                        "Couldn't get a location fix yet — try again, or use Choose on Map.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                isLocatingLocation = false
                val loadingIndex = messages.indexOfFirst { it.id == loadingId }
                if (loadingIndex >= 0) messages.removeAt(loadingIndex)
                Toast.makeText(context, "Couldn't detect your location. Use Choose on Map instead.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            permissionPermanentlyDenied = false
            if (isSystemLocationEnabled(context)) locateAndSend() else showEnableLocationDialog = true
        } else {
            val canAskAgain = activity != null && (
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            permissionPermanentlyDenied = !canAskAgain
            if (!canAskAgain) {
                // Real denial just came back and Android says it won't show
                // its own dialog again — this is what made a second tap on
                // "Share My Location" look like nothing happened at all.
                showPermissionDeniedDialog = true
            } else {
                Toast.makeText(context, "Location permission is needed to auto-detect your area — tap \"Share My Location\" to try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun shareLocationClick() {
        when {
            !hasLocationPermission(context) -> {
                if (permissionPermanentlyDenied) {
                    showPermissionDeniedDialog = true
                } else {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            }
            !isSystemLocationEnabled(context) -> showEnableLocationDialog = true
            else -> locateAndSend()
        }
    }

    fun confirmAndSave() {
        if (isBusy) return
        
        val priceVal = editablePrice.trim()
        val dPrice = priceVal.toDoubleOrNull()
        if (dPrice == null || dPrice <= 0.0) {
            activeErrorAlert = "Base Price must be a valid positive number."
            return
        }
        
        if (editableLocation.trim().isEmpty()) {
            activeErrorAlert = "Location cannot be empty."
            return
        }
        
        if (editableDescription.trim().isEmpty()) {
            activeErrorAlert = "Service Description cannot be empty."
            return
        }
        
        parseAisoAttributes(summary).forEach { field ->
            val valEntered = editableAttributes[field.key]?.trim() ?: ""

            if (field.isRequired && valEntered.isEmpty()) {
                activeErrorAlert = "Field '${field.displayLabel}' is required."
                return
            }

            if (valEntered.isNotEmpty() && field.options.isNotEmpty()) {
                if (field.inputType == "select" || field.inputType == "boolean") {
                    val isValidOpt = field.options.any { it.equals(valEntered, ignoreCase = true) }
                    if (!isValidOpt) {
                        activeErrorAlert = "Field '${field.displayLabel}' must be one of the valid options: ${field.options.joinToString(", ")}."
                        return
                    }
                } else if (field.inputType == "multiselect") {
                    val parts = valEntered.split(",").map { it.trim() }
                    for (part in parts) {
                        val isValidOpt = field.options.any { it.equals(part, ignoreCase = true) }
                        if (!isValidOpt) {
                            activeErrorAlert = "Field '${field.displayLabel}' contains an invalid option '$part'. Valid options are: ${field.options.joinToString(", ")}."
                            return
                        }
                    }
                }
            }
        }
        
        isBusy = true
        scope.launch {
            try {
                val builder = StringBuilder()
                builder.append("base_price=").append(dPrice)
                builder.append("||location_display_name=").append(editableLocation.trim())
                builder.append("||description=").append(editableDescription.trim())
                for ((k, v) in editableAttributes) {
                    builder.append("||attributes:").append(k).append("=").append(v.trim())
                }

                onUpdate(builder.toString())
                val response = onSave()
                isBusy = false
                if (response != null && response.ok) {
                    isDone = true
                } else {
                    val rawErr = response?.reply ?: "Could not save your listing. Please try again."
                    val friendlyErr = if (rawErr.contains("err_single_listing_per_service_type")) {
                        "You have already registered this service type. Nestora only allows a single listing per provider for standard services (like plumber or electrician), while property and rental categories support multiple listings. Please edit your existing listing to make changes."
                    } else {
                        rawErr
                    }
                    activeErrorAlert = friendlyErr
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                isBusy = false
                activeErrorAlert = "Something went wrong while saving your listing. Please try again."
            }
        }
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Location Permission Needed", fontWeight = FontWeight.Bold) },
            text = { Text("Location access is denied, and Android won't show the permission prompt again automatically. Enable it for Nestora in system Settings to auto-detect your area — or just type it instead.") },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
                        runCatching { context.startActivity(intent) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                ) { Text("Open Settings", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPermissionDeniedDialog = false }) { Text("Not now", color = NestoraTextMuted) }
            }
        )
    }

    if (showEnableLocationDialog) {
        AlertDialog(
            onDismissRequest = { showEnableLocationDialog = false },
            title = { Text("Turn on Location", fontWeight = FontWeight.Bold) },
            text = { Text("Location is switched off for this device — an app can't turn it on for you. Turn it on in system Settings to auto-detect your area, or just type it instead.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEnableLocationDialog = false
                        runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NestoraMint)
                ) { Text("Open Location Settings", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEnableLocationDialog = false }) { Text("Not now", color = NestoraTextMuted) }
            }
        )
    }

    if (isDone) {
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "Your service listing is now active and search-ready for users on Nestora.",
                        fontSize = 14.sp,
                        color = NestoraTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            onClearChat()
                            selectedServiceType = null
                            isRejected = false
                            isDone = false
                            onBack()
                        },
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
        return
    }

    if (isRejected) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF0F0), Color(0xFFFFFFFF))
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
                            .background(Color(0xFFFFEBEE), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFD32F2F), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚠️", fontSize = 28.sp)
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "REGISTRATION BLOCKED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Registration Denied",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC62828),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        text = "Nestora is a marketplace for home, professional, and business services (plumbers, electricians, tutors, etc.). Personal companionship or adult services are not allowed and this registration cannot continue.",
                        fontSize = 13.sp,
                        color = Color(0xFF5D4037),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            onClearChat()
                            selectedServiceType = null
                            isRejected = false
                            isDone = false
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
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
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9F9F9) // Clean light-gray background matching brand theme
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF004D40)) // Deep teal header background
                    .statusBarsPadding()
                    .padding(vertical = 8.dp, horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "Nestora AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )

                val isRegistrationStarted = messages.size > 1
                IconButton(
                    onClick = {
                        if (isRegistrationStarted) {
                            onClearChat()
                            selectedServiceType = null
                            isRejected = false
                            isDone = false
                            readyToConfirm = false
                            summary = null
                        }
                    },
                    enabled = isRegistrationStarted,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Chat",
                        tint = if (isRegistrationStarted) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                // Welcoming Dashboard at the top of the registration flow
                item {
                    WelcomeDashboard(
                        userName = userName ?: "Provider",
                        isRegisterFlow = true,
                        onCardClick = { text ->
                            sendTurn(text)
                        }
                    )
                }

                items(messages) { msg ->
                    // The backend already gives this message a typed AISO gap.  Do
                    // not infer an important step such as location from English
                    // presentation text: wording can change without changing the
                    // protocol, which previously hid both location actions from a
                    // valid required-location response.
                    val isLocReq = !msg.isUser && (
                        msg.aisoGap?.fieldType == "location" ||
                            msg.aisoGap?.key == "__location__" ||
                            // Retain the old copy check only for a response from an
                            // older backend during a rolling app/backend update.
                            msg.text.contains("Where is this service located", ignoreCase = true) ||
                            msg.text.contains("Share My Location", ignoreCase = true)
                        )
                    val isCustReq = !msg.isUser && msg.text.contains("switch to the Explore or Find Service tab", ignoreCase = true)
                    val isLatestBotMsg = messages.lastOrNull { !it.isUser }?.id == msg.id
                    
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column {
                            ChatMessageItem(
                                message = msg,
                                onBookListing = {},
                                showLocationButton = isLocReq && isLatestBotMsg,
                                onLocationButtonClick = { shareLocationClick() },
                                onSelectLocationClick = onSelectLocationClick,
                                showExploreButton = isCustReq && isLatestBotMsg,
                                onExploreButtonClick = onExploreClick,
                                userPhotoPath = userPhotoPath
                            )
                            if (isLatestBotMsg && msg.aisoGap != null) {
                                VerticalGapOptionsWidget(
                                    gap = msg.aisoGap,
                                    onSendResponse = { response -> sendTurn(response) },
                                    isBusy = isBusy
                                )
                            }
                        }
                    }
                }

                if (!isInputEnabled) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Choose a service to register:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF004D40), // Deep teal section header
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            val displayList = serviceTypes.ifEmpty { fallbackServiceTypes }
                            
                            displayList.forEach { serviceType ->
                                Surface(
                                    onClick = {
                                        selectedServiceType = serviceType
                                        messages.add(
                                            TelegramChatMessage(
                                                id = "bot-select-${System.currentTimeMillis()}",
                                                sender = "Nestora AI",
                                                text = "You have selected to register as a ${serviceType.name}. Describe your service details below (for example: rate, availability, and specific skills).",
                                                timestamp = getCurrentFormattedTime(),
                                                isUser = false
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE8F5E9), // Soft mint background
                                    border = BorderStroke(1.dp, Color(0xFFE2EAF2)), // Clean light border
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color.White, shape = CircleShape), // White circle background
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(serviceType.emoji, fontSize = 18.sp)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = serviceType.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF004D40) // Deep teal text
                                            )
                                        }
                                        Text(
                                            text = "→",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF004D40) // Deep teal arrow
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            if (readyToConfirm) {
                item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                                Text("Preview & Edit Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                                Spacer(Modifier.height(4.dp))
                                Text("Review and update details before publishing your service listing.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(Modifier.height(16.dp))
 
                                summary?.let { s ->
                                    val svcType = s.get("service_type_name")?.takeIf { !it.isJsonNull }?.asString ?: "Service Provider"
                                    Text("Service Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                                    Text(svcType, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C2C2C))
                                    Spacer(Modifier.height(12.dp))
                                }
 
                                Text("Base Price (in ₹)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = editablePrice,
                                    onValueChange = { editablePrice = it },
                                    placeholder = { Text("e.g. 500", color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF004D40),
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedTextColor = Color(0xFF2C2C2C),
                                        unfocusedTextColor = Color(0xFF2C2C2C)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(12.dp))
 
                                Text("Location Area/City", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editableLocation,
                                        onValueChange = { },
                                        readOnly = true,
                                        placeholder = { Text("Tap button to choose location...", color = Color.Gray) },
                                        singleLine = false,
                                        maxLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.LightGray,
                                            unfocusedBorderColor = Color.LightGray,
                                            focusedTextColor = Color(0xFF2C2C2C),
                                            unfocusedTextColor = Color(0xFF2C2C2C)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = onSelectLocationClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(52.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Pick on Map",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
 
                                Text("Service Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = editableDescription,
                                    onValueChange = { editableDescription = it },
                                    placeholder = { Text("Describe details of the services you offer...", color = Color.Gray) },
                                    singleLine = false,
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF004D40),
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedTextColor = Color(0xFF2C2C2C),
                                        unfocusedTextColor = Color(0xFF2C2C2C)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
 
                                parseAisoAttributes(summary).forEach { field ->
                                    val currentVal = editableAttributes[field.key] ?: field.value
 
                                    Spacer(Modifier.height(12.dp))
                                    Text(field.displayLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                                    Spacer(Modifier.height(4.dp))
 
                                    if (field.inputType == "boolean") {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf("Yes", "No").forEach { opt ->
                                                val isSel = currentVal.trim().lowercase() == opt.lowercase()
                                                FilterChip(
                                                    selected = isSel,
                                                    onClick = { editableAttributes[field.key] = opt },
                                                    label = { Text(opt) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color(0xFFE0F2F1),
                                                        selectedLabelColor = Color(0xFF004D40),
                                                        containerColor = Color.White,
                                                        labelColor = Color.Gray
                                                    ),
                                                    border = FilterChipDefaults.filterChipBorder(
                                                        enabled = true,
                                                        selected = isSel,
                                                        borderColor = Color.LightGray,
                                                        selectedBorderColor = Color(0xFF004D40),
                                                        borderWidth = 1.dp,
                                                        selectedBorderWidth = 1.5.dp
                                                    )
                                                )
                                            }
                                        }
                                    } else if ((field.inputType == "select" || field.inputType == "multiselect") && field.options.isNotEmpty()) {
                                        val selectedList = currentVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                        androidx.compose.foundation.layout.FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            field.options.forEach { opt ->
                                                val isSel = if (field.inputType == "multiselect") {
                                                    selectedList.contains(opt)
                                                } else {
                                                    currentVal.trim() == opt
                                                }
                                                FilterChip(
                                                    selected = isSel,
                                                    onClick = {
                                                        if (field.inputType == "multiselect") {
                                                            val newList = if (isSel) {
                                                                selectedList - opt
                                                            } else {
                                                                selectedList + opt
                                                            }
                                                            editableAttributes[field.key] = newList.joinToString(", ")
                                                        } else {
                                                            editableAttributes[field.key] = opt
                                                        }
                                                    },
                                                    label = { Text(opt, fontSize = 12.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color(0xFFE0F2F1),
                                                        selectedLabelColor = Color(0xFF004D40),
                                                        containerColor = Color.White,
                                                        labelColor = Color.Gray
                                                    ),
                                                    border = FilterChipDefaults.filterChipBorder(
                                                        enabled = true,
                                                        selected = isSel,
                                                        borderColor = Color.LightGray,
                                                        selectedBorderColor = Color(0xFF004D40),
                                                        borderWidth = 1.dp,
                                                        selectedBorderWidth = 1.5.dp
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = currentVal,
                                            onValueChange = { editableAttributes[field.key] = it },
                                            placeholder = { Text("Enter ${field.displayLabel}", color = Color.Gray) },
                                            singleLine = field.inputType == "text" || field.inputType == "number",
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF004D40),
                                                unfocusedBorderColor = Color.LightGray,
                                                focusedTextColor = Color(0xFF2C2C2C),
                                                unfocusedTextColor = Color(0xFF2C2C2C)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
 
                                Button(
                                    onClick = { confirmAndSave() },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(if (isBusy) "Publishing…" else "Confirm & Publish", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

        val currentGap = messages.lastOrNull { !it.isUser }?.aisoGap
        // Location is deliberately GPS/map-only. This keeps listing geography
        // precise and prevents free-text area names from producing ambiguous or
        // misleading service locations.
        val isStrictInput = currentGap != null && requiresChoiceOnlyRegistrationInput(
            currentGap.inputType,
            currentGap.fieldType,
            currentGap.key
        )

        if ((isInputEnabled || isStrictInput) && !readyToConfirm) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().imePadding()
            ) {
                if (isStrictInput) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFF9F5), // Soft orange tint background
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.nestora_bottom_logo),
                                contentDescription = null,
                                tint = Color(0xFFFF8C00),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (currentGap?.fieldType == "location" || currentGap?.key == "__location__")
                                    "Use Share My Location or Choose on Map above"
                                else "Select from the choices above to reply",
                                color = Color(0xFFFF8C00), // High-contrast orange
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                        HorizontalDivider(color = Color(0xFFE2EAF2), thickness = 1.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                            ) {
                                if (inputText.isEmpty()) {
                                    Text("Describe your service...", color = Color.Gray, fontSize = 14.sp)
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = Color(0xFF2C2C2C)),
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (inputText.isNotBlank() && !isBusy) Color(0xFF004D40) else Color(0xFFE2EAF2))
                                    .clickable(enabled = inputText.isNotBlank() && !isBusy) {
                                        val text = inputText
                                        inputText = ""
                                        sendTurn(text)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank() && !isBusy) Color.White else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeErrorAlert != null) {
        AlertDialog(
            onDismissRequest = { activeErrorAlert = null },
            title = { Text("Registration Alert", fontWeight = FontWeight.Bold) },
            text = { Text(activeErrorAlert ?: "") },
            confirmButton = {
                TextButton(onClick = { activeErrorAlert = null }) {
                    Text("OK", color = NestoraMintDark, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
}

@Composable
fun VerticalGapOptionsWidget(
    gap: com.estatenestora.app.data.model.AisoGapField,
    onSendResponse: (String) -> Unit,
    isBusy: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val brandColor = Color(0xFF004D40) // Deep Teal brand color
        val accentBg = Color(0xFFE8FAF4) // Soft mint bg

        when (gap.inputType) {
            "boolean" -> {
                listOf("Yes", "No").forEach { option ->
                    OutlinedButton(
                        onClick = { if (!isBusy) onSendResponse(option) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, brandColor),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(option, color = brandColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            "select" -> {
                val options = gap.options ?: emptyList()
                options.forEach { option ->
                    OutlinedButton(
                        onClick = { if (!isBusy) onSendResponse(option) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, brandColor),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(option, color = brandColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            "multiselect" -> {
                val options = gap.options ?: emptyList()
                val selectedOptions = remember(gap) { mutableStateListOf<String>() }
                
                options.forEach { option ->
                    val isSelected = selectedOptions.contains(option)
                    OutlinedButton(
                        onClick = {
                            if (isSelected) {
                                selectedOptions.remove(option)
                            } else {
                                selectedOptions.add(option)
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) accentBg else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            color = if (isSelected) brandColor else Color(0xFFE2EAF2)
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option, color = if (isSelected) brandColor else Color(0xFF2C2C2C), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = brandColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                
                if (options.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            if (!isBusy && selectedOptions.isNotEmpty()) {
                                onSendResponse(selectedOptions.joinToString(", "))
                            }
                        },
                        enabled = !isBusy && selectedOptions.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandColor,
                            disabledContainerColor = Color(0xFFE2EAF2)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Confirm Selection", color = if (selectedOptions.isNotEmpty()) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}// getCurrentLocation / hasLocationPermission / isSystemLocationEnabled /
// findActivity now live in com.estatenestora.app.util.LocationUtils, shared
// with MapLocationPickerScreen — see shareLocationClick() above for why.
