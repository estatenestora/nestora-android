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
import androidx.compose.ui.unit.dp
import com.estatenestora.app.data.model.BookingPolicy
import com.estatenestora.app.data.model.AvailabilitySlot
import com.estatenestora.app.data.model.ServiceListing
import com.estatenestora.app.data.repository.NestoraRepository
import com.estatenestora.app.ui.theme.NestoraMint
import com.google.gson.JsonObject
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

// P3's one sheet adapts from catalog policy instead of maintaining a flow per
// service. It writes only P2 app drafts; no legacy CREATE_BOOKING call is used.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdaptiveBookingSheet(
    listing: ServiceListing,
    initialLocationText: String,
    initialLat: Double,
    initialLon: Double,
    onDismiss: () -> Unit,
    onFetchPolicy: suspend (String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onFetchAvailability: suspend (String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onCreateDraft: suspend (String, String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onSetLocation: suspend (String, Boolean, Double, Double, String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onSetSchedule: suspend (String, String?, String?, String) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
    onSetTimePreference: suspend (String, String, JsonObject) -> com.estatenestora.app.data.model.AndroidBridgeResponse?,
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

        val response = onFetchAvailability(listing.id)
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
        policy = policyResponse?.bookingPolicy
        policy?.let { loaded ->
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
        val response = policy?.let { onCreateDraft(listing.id, UUID.randomUUID().toString()) }
        draftId = response?.engagementDraft?.id
        if (policy == null || draftId == null) {
            error = bookingStartFailureMessage(
                policyLoaded = policy != null,
                policyReply = policyResponse?.reply,
                draftReply = response?.reply
            )
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
                                suspend fun saveAndSubmit(activeDraftId: String): com.estatenestora.app.data.model.AndroidBridgeResponse? {
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
                                var submitted = saveAndSubmit(id)
                                // A draft can become stale after the sheet opened (for
                                // example after an app reconnect). Preserve every typed
                                // value, create one fresh draft, and replay the form once.
                                if (isEngagementDraftNoLongerActive(submitted)) {
                                    val refreshed = onCreateDraft(listing.id, UUID.randomUUID().toString())
                                    val refreshedId = refreshed?.engagementDraft?.id
                                    if (refreshed?.ok == true && !refreshedId.isNullOrBlank()) {
                                        draftId = refreshedId
                                        submitted = saveAndSubmit(refreshedId)
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
