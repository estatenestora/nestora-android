package com.estatenestora.app.ui.screens

import com.estatenestora.app.data.model.BookingSummary

/** Coordinates supplied to a booking sheet. A pair is always kept together. */
internal data class BookingCoordinates(val latitude: Double, val longitude: Double)

/** Resolves service location: booking-map override, current address bar, then GPS. */
internal fun resolveBookingCoordinates(
    pendingLatitude: Double?,
    pendingLongitude: Double?,
    addressLatitude: Double?,
    addressLongitude: Double?,
    gpsLatitude: Double,
    gpsLongitude: Double
): BookingCoordinates = when {
    isUsableCoordinatePair(pendingLatitude, pendingLongitude) -> BookingCoordinates(pendingLatitude!!, pendingLongitude!!)
    isUsableCoordinatePair(addressLatitude, addressLongitude) -> BookingCoordinates(addressLatitude!!, addressLongitude!!)
    else -> BookingCoordinates(gpsLatitude, gpsLongitude)
}

private fun isUsableCoordinatePair(latitude: Double?, longitude: Double?): Boolean =
    latitude != null && longitude != null && latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 && !(latitude == 0.0 && longitude == 0.0)

/** A received booking is authoritative even while the profile hint is stale. */
internal fun shouldShowProviderInbox(hasProviderListings: Boolean, receivedBookingCount: Int): Boolean =
    hasProviderListings || receivedBookingCount > 0

/**
 * The server tags every GET_MY_BOOKINGS row with the authenticated viewer's
 * role. Use that tag first so Sent/Received never depends on a separate
 * profile request. Older cached responses fall back to the UUID comparison.
 */
internal fun bookingBelongsToSelectedRole(
    booking: BookingSummary,
    viewerUserId: String?,
    isProviderMode: Boolean
): Boolean = when (booking.viewerRole.uppercase()) {
    "PROVIDER" -> isProviderMode
    "CUSTOMER" -> !isProviderMode
    else -> if (isProviderMode) booking.providerUserId == viewerUserId else booking.customerUserId == viewerUserId
}

internal data class BookingStats(
    val total: Int,
    val active: Int,
    val completed: Int,
    val cancelled: Int
)

private const val PROVIDER_TRAVEL_EARLY_START_MINUTES = 30L
private const val MAXIMUM_TRAVEL_START_DISTANCE_METERS = 250_000.0

/**
 * A scheduled service is not a live trip.  GPS sharing and ETA become useful
 * only shortly before the agreed service time; before then they can publish a
 * provider's unrelated current location and make a future job look broken.
 */
internal data class LiveTravelEligibility(val allowed: Boolean, val unavailableMessage: String? = null)

internal fun liveTravelEligibility(
    scheduledStartAt: String?,
    now: java.time.Instant = java.time.Instant.now()
): LiveTravelEligibility {
    if (scheduledStartAt.isNullOrBlank()) return LiveTravelEligibility(allowed = true)
    val scheduledStart = try {
        java.time.OffsetDateTime.parse(scheduledStartAt).toInstant()
    } catch (_: Exception) {
        // The backend is authoritative and still rejects an invalid attempt.
        return LiveTravelEligibility(allowed = true)
    }
    val opensAt = scheduledStart.minusSeconds(PROVIDER_TRAVEL_EARLY_START_MINUTES * 60)
    if (!now.isBefore(opensAt)) return LiveTravelEligibility(allowed = true)

    val localStart = scheduledStart.atZone(java.time.ZoneId.systemDefault())
    val readableStart = java.time.format.DateTimeFormatter
        .ofPattern("EEEE, d MMMM 'at' h:mm a", java.util.Locale.getDefault())
        .format(localStart)
    val readableOpenTime = java.time.format.DateTimeFormatter
        .ofPattern("h:mm a", java.util.Locale.getDefault())
        .format(opensAt.atZone(java.time.ZoneId.systemDefault()))
    return LiveTravelEligibility(
        allowed = false,
        unavailableMessage = "Your appointment is on $readableStart. GPS Tracking will be available from $readableOpenTime."
    )
}

/**
 * Stops a simulator/default GPS position on another continent before it is
 * submitted. The backend enforces the same 250 km safety boundary.
 */
internal fun providerTravelLocationEligibility(
    providerLatitude: Double,
    providerLongitude: Double,
    customerLatitude: Double?,
    customerLongitude: Double?
): LiveTravelEligibility {
    if (!isUsableCoordinatePair(customerLatitude, customerLongitude)) return LiveTravelEligibility(allowed = true)
    if (!isUsableCoordinatePair(providerLatitude, providerLongitude)) {
        return LiveTravelEligibility(false, "Nestora could not verify your current GPS location. Check device location and try again.")
    }
    val distanceMeters = haversineDistanceMeters(
        providerLatitude,
        providerLongitude,
        customerLatitude!!,
        customerLongitude!!
    )
    return if (distanceMeters > MAXIMUM_TRAVEL_START_DISTANCE_METERS) {
        LiveTravelEligibility(
            false,
            "Your GPS location is far from this service address. Check your device location before starting travel."
        )
    } else {
        LiveTravelEligibility(allowed = true)
    }
}

private fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusMeters = 6_371_000.0
    val latitudeDelta = Math.toRadians(lat2 - lat1)
    val longitudeDelta = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(latitudeDelta / 2).let { it * it } +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(longitudeDelta / 2).let { it * it }
    return 2 * earthRadiusMeters * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
}

/** Stats must use raw statuses: the display ENDED stage also contains rejects and expiries. */
internal fun bookingStatsForRole(bookings: List<BookingSummary>): BookingStats = BookingStats(
    total = bookings.size,
    active = bookings.count { it.stage !in setOf("DONE", "ENDED") },
    completed = bookings.count { it.status.uppercase() in setOf("COMPLETED", "CLOSED", "PAID") },
    cancelled = bookings.count { it.status.uppercase() == "CANCELLED" }
)

/** Background list polling is needed only when its data is on screen. */
internal fun shouldRunBookingListPolling(isAuthenticated: Boolean, activeScreen: String, selectedTab: Int): Boolean =
    isAuthenticated && activeScreen == "main" && selectedTab == 2

/** A listing can be paused only after every request and open job is concluded. */
internal fun canDeactivateListing(openBookingCount: Int): Boolean = openBookingCount == 0

internal fun bookingStartFailureMessage(policyLoaded: Boolean, policyReply: String?, draftReply: String?): String? = when {
    !policyLoaded -> policyReply?.takeIf { it.isNotBlank() }
        ?: "Could not load booking options because Nestora did not return a response. Check your connection and try again."
    else -> draftReply?.takeIf { it.isNotBlank() }
        ?: "Nestora did not return the booking-form confirmation. Reopen this service to continue; no provider request or payment was submitted."
}

internal fun bookingSubmissionFailureMessage(step: String, reply: String?): String =
    reply?.takeIf { it.isNotBlank() }
        ?: "Nestora did not confirm $step. Your request was not submitted; reopen the form and try again."

/** A changed preferred start must never retain a default end from another day. */
internal fun preferredWindowEndForStart(startAt: String, durationMinutes: Int): String? = try {
    java.time.OffsetDateTime.parse(startAt)
        .plusMinutes(durationMinutes.coerceAtLeast(10).toLong())
        .toString()
} catch (_: Exception) {
    null
}

/** Registration location is intentionally collected only by GPS or map picker. */
internal fun requiresChoiceOnlyRegistrationInput(inputType: String?, fieldType: String?, key: String?): Boolean =
    inputType in setOf("select", "multiselect", "boolean") ||
        fieldType.equals("location", ignoreCase = true) || key == "__location__"

internal data class RequestedBookingStatusCopy(val header: String, val subtext: String)

/** Requested booking copy must describe the current viewer's responsibility. */
internal fun requestedBookingStatusCopy(isViewerProvider: Boolean, counterpartName: String): RequestedBookingStatusCopy =
    if (isViewerProvider) {
        RequestedBookingStatusCopy("New service request", "Review the request and send a quote when required.")
    } else {
        RequestedBookingStatusCopy("Request sent!", "$counterpartName is reviewing your request.")
    }

/** Quotes are negotiated only while the first visit is still awaiting a decision. */
internal fun canProviderSendServiceQuote(
    requiresProviderQuote: Boolean,
    bookingStatus: String,
    latestQuoteStatus: String?
): Boolean = requiresProviderQuote && bookingStatus.equals("REQUESTED", ignoreCase = true) &&
    (latestQuoteStatus == null || latestQuoteStatus.equals("REJECTED", ignoreCase = true) ||
        latestQuoteStatus.equals("EXPIRED", ignoreCase = true))

/** A customer never sends a quote; they can only decide the current live proposal. */
internal fun canCustomerDecideServiceQuote(
    bookingStatus: String,
    quoteStatus: String,
    canCustomerAct: Boolean
): Boolean = bookingStatus.equals("REQUESTED", ignoreCase = true) &&
    quoteStatus.equals("PROPOSED", ignoreCase = true) && canCustomerAct

/** Quote scopes must describe work only; contact and off-platform coordination are never allowed. */
internal fun quoteScopeSafetyError(scope: String): String? {
    val normalized = scope.trim()
    val digitNormalized = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9"
    ).entries.fold(normalized.lowercase()) { value, (word, digit) ->
        value.replace(Regex("""(?i)(?<![a-z])$word(?![a-z])"""), digit)
    }
    if (normalized.isBlank()) return "Describe the work included in this quote."
    if (Regex("""(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""").containsMatchIn(normalized)) return "Email addresses cannot be included in a quote."
    if (Regex("""(?i)(?:https?://|www\.|t\.me/|wa\.me/|(?:[a-z0-9-]+\.)+(?:com|in|net|org|me|io|app|co)\b)""").containsMatchIn(normalized)) return "Links cannot be included in a quote."
    if (Regex("""(?<!\d)(?:\+?\d[\s().-]*){7,14}\d(?!\d)""").containsMatchIn(digitNormalized)) return "Phone numbers cannot be included in a quote."
    if (Regex("""(?i)(?<!\w)@[a-z0-9_]{3,}|\b(?:whats\s*app|telegram|signal|instagram|facebook|linkedin|email|e-?mail|phone|mobile)\b|\b(?:call|text|message|contact|reach)\s+(?:me|us|on|at)\b""").containsMatchIn(normalized)) return "Contact details or off-platform communication instructions are not allowed in a quote."
    return null
}

/** When Finder main menu tab is clicked, we must open Finder Homepage instead of Nestora AI Chat. */
internal fun shouldOpenFinderHomeInsteadOfChat(isFinderMenuClicked: Boolean): Boolean = isFinderMenuClicked

/** Assistant tab must show the homepage center cards and NOT Nestora AI Chat. */
internal fun shouldShowAIChatInAssistantTab(selectedFinderTab: Int): Boolean = false

/** Assistant tab (selectedFinderTab == 1) shows the cards, while Finder tab (selectedFinderTab == 0) remains clean. */
internal fun shouldShowFinderCards(selectedFinderTab: Int): Boolean = selectedFinderTab == 1

/** Formats listing booking count showing only total count and no open/requested sub-counts. */
internal fun formatListingBookingsCount(openCount: Int, requestedCount: Int): String {
    val total = openCount.coerceAtLeast(0) + requestedCount.coerceAtLeast(0)
    return "Bookings: $total"
}

/** Formats listing rating string. If rating is negative or invalid, coerces to 0.0. */
internal fun formatListingRating(rating: Float): String {
    val coerced = rating.coerceIn(0f, 5f)
    return String.format(java.util.Locale.US, "%.1f", coerced)
}

