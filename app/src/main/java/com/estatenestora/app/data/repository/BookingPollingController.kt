package com.estatenestora.app.data.repository

import android.util.Log
import com.estatenestora.app.data.model.BookingDetail
import com.estatenestora.app.data.model.BookingSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal fun shouldRunBookingListPolling(foregrounded: Boolean, bookingsScreenVisible: Boolean): Boolean =
    foregrounded && bookingsScreenVisible

internal fun shouldRefreshBookingsForRoute(
    authenticated: Boolean,
    activeScreen: String,
    selectedTab: Int
): Boolean = authenticated && activeScreen == "main" && selectedTab == 2

internal fun shouldRefreshOpenBookingDetailForEvent(activeDetailId: String?, eventBookingId: String): Boolean =
    activeDetailId != null && activeDetailId == eventBookingId

internal fun shouldRefreshBookingListForEvent(
    sessionActive: Boolean,
    foregrounded: Boolean,
    bookingsScreenVisible: Boolean
): Boolean = sessionActive && foregrounded && bookingsScreenVisible

/**
 * Adaptive polling for the booking list (My/Sent/Received tabs) and a single
 * booking's detail/tracking screen — one small, single-purpose controller
 * rather than a general ViewModel layer (every other screen in the app keeps
 * the existing "closures + repository" idiom untouched; this exists only
 * because booking state must be shared across 3 sub-tabs + a detail screen
 * with one polling loop each, not duplicated per screen).
 *
 * Poll cadence is deliberately adaptive rather than fixed, to feel like
 * low-latency live tracking without hammering Telegram's Bot API: each poll
 * is one client-initiated sendInlineQuery -> answerInlineQuery round trip,
 * not a bot-pushed message, so it doesn't count against Telegram's per-chat
 * outbound flood limits — but there's still no reason to poll every second
 * while a booking is just sitting in REQUESTED.
 *
 *   - List refresh: one full fetch when Bookings opens, immediate FCM-driven
 *     deltas for changes, and a 60s fallback delta only while that tab is open.
 *   - Detail polling: 15s while the booking is in a passive stage
 *     (REQUESTED/ACCEPTED/PAYMENT), 5s while it's IN_PROGRESS (provider en
 *     route/arrived/OTP/service running) — the parts a Swiggy-style tracker
 *     actually needs to feel live.
 *   - Both stop entirely when backgrounded, and back off (up to 60s) after
 *     two consecutive failures rather than retrying at the base interval
 *     into a possibly-down backend.
 */
class BookingPollingController(
    private val repository: NestoraRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "BookingPolling"
        // FCM is the normal real-time transport. This is intentionally only a
        // recovery path for missed pushes, so opening Bookings does not create
        // a permanent high-frequency read load on the database.
        private const val LIST_INTERVAL_MS = 60_000L
        private const val DETAIL_PASSIVE_INTERVAL_MS = 15_000L
        private const val DETAIL_ACTIVE_INTERVAL_MS = 5_000L
        private const val MAX_BACKOFF_MS = 300_000L
        private val ACTIVE_STAGES = setOf("IN_PROGRESS")
        private val TERMINAL_STAGES = setOf("DONE", "ENDED")
    }

    private val _bookings = MutableStateFlow<List<BookingSummary>>(emptyList())
    val bookings: StateFlow<List<BookingSummary>> = _bookings.asStateFlow()

    private val _detail = MutableStateFlow<BookingDetail?>(null)
    val detail: StateFlow<BookingDetail?> = _detail.asStateFlow()

    private val _detailNotFound = MutableStateFlow<String?>(null)
    val detailNotFound: StateFlow<String?> = _detailNotFound.asStateFlow()

    private var listJob: Job? = null
    private var detailJob: Job? = null
    private var refreshJob: Job? = null
    private val listSyncMutex = Mutex()
    private var lastSyncIso: String? = null
    private var hasCompletedListSync = false
    private var bookingsScreenVisible = false
    private var sessionActive = false
    private var activeDetailId: String? = null
    private var foregrounded = true

    init {
        // FCM is the normal path for a booking change. Refresh the list only
        // when it is actually visible; otherwise the next visible-screen delta
        // catches it using the saved cursor. An open detail still refreshes
        // immediately regardless of the list screen.
        scope.launch {
            NestoraEventBus.bookingUpdates.collect { bookingId ->
                if (shouldRefreshBookingListForEvent(sessionActive, foregrounded, bookingsScreenVisible)) {
                    triggerListSync()
                }
                // A push for the booking the customer is currently viewing
                // must refresh that detail immediately. Keeping its cached
                // content visible avoids a blank screen while the latest
                // status replaces it, including dismissal of the OTP card.
                if (sessionActive && foregrounded && shouldRefreshOpenBookingDetailForEvent(activeDetailId, bookingId)) {
                    openDetail(bookingId, clearCache = false)
                }
            }
        }
    }

    /** Enables/disables booking refreshes for the authenticated app session. */
    fun setSessionActive(active: Boolean) {
        sessionActive = active
        if (!active) {
            clear()
        } else if (bookingsScreenVisible && foregrounded) {
            triggerListSync()
            startVisibleListPolling()
        }
    }

    /** Called whenever the Bookings tab becomes visible or hidden. */
    fun setBookingsScreenVisible(visible: Boolean) {
        bookingsScreenVisible = visible
        if (!visible) {
            listJob?.cancel()
            listJob = null
            return
        }
        triggerListSync()
        startVisibleListPolling()
    }

    private fun startVisibleListPolling() {
        if (!sessionActive || !shouldRunBookingListPolling(foregrounded, bookingsScreenVisible)) return
        if (listJob?.isActive == true) return
        listJob = scope.launch {
            var backoffMs = LIST_INTERVAL_MS
            var consecutiveFailures = 0
            // setBookingsScreenVisible already triggers the immediate load.
            // Wait before the first background poll to avoid two back-to-back
            // Telegram queries when the user opens the tab.
            delay(LIST_INTERVAL_MS)
            while (isActive) {
                try {
                    syncListOnce()
                    consecutiveFailures = 0
                    backoffMs = LIST_INTERVAL_MS
                } catch (e: Throwable) {
                    consecutiveFailures++
                    backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                    Log.w(TAG, "[List] poll failed ($consecutiveFailures consecutive), backing off to ${backoffMs}ms", e)
                }
                delay(backoffMs)
            }
        }
    }

    /** One immediate, coalesced refresh after a user action or FCM event. */
    fun triggerListSync() {
        if (!sessionActive || !foregrounded || refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            try {
                syncListOnce(forceFullRefresh = !hasCompletedListSync)
            } catch (e: Throwable) {
                Log.e(TAG, "Triggered list sync failed", e)
            }
        }
    }

    /** Serialises full/delta refreshes so polling cannot race a user/FCM sync. */
    private suspend fun syncListOnce(forceFullRefresh: Boolean = false) = listSyncMutex.withLock {
        val requestStartedAt = java.time.Instant.now().toString()
        val isFullRefresh = forceFullRefresh || !hasCompletedListSync
        val updates = if (isFullRefresh) {
            repository.getMyBookingsForPolling()
        } else {
            repository.getBookingUpdatesForPolling(lastSyncIso)
        } ?: throw IllegalStateException("Booking sync did not receive a server response")
        mergeSummaries(updates, isFullRefresh = isFullRefresh)
        hasCompletedListSync = true
        // An empty first response must still create a cursor. Otherwise every
        // periodic fallback remains GET_MY_BOOKINGS forever for new users.
        if (lastSyncIso == null) lastSyncIso = requestStartedAt
    }

    /** Merges a delta (or full refresh) into the current list by id, newest-first by updatedAt. */
    private fun mergeSummaries(updates: List<BookingSummary>, isFullRefresh: Boolean = false) {
        if (isFullRefresh) {
            _bookings.value = updates.sortedByDescending { it.updatedAt }
            lastSyncIso = _bookings.value.maxOfOrNull { it.updatedAt } ?: lastSyncIso
            return
        }
        if (updates.isEmpty() && lastSyncIso != null) return // nothing changed since last poll
        val byId = _bookings.value.associateBy { it.id }.toMutableMap()
        for (u in updates) byId[u.id] = u
        _bookings.value = byId.values.sortedByDescending { it.updatedAt }
        lastSyncIso = _bookings.value.maxOfOrNull { it.updatedAt } ?: lastSyncIso
    }

    /** Called when a BookingDetailScreen for [bookingId] becomes visible. */
    fun openDetail(bookingId: String, clearCache: Boolean = true) {
        activeDetailId = bookingId
        _detailNotFound.value = null
        if (clearCache) {
            _detail.value = null
        }
        if (!foregrounded) return
        detailJob?.cancel()
        detailJob = scope.launch {
            var backoffMs = DETAIL_PASSIVE_INTERVAL_MS
            var consecutiveFailures = 0
            while (isActive) {
                try {
                    val d = repository.getBookingDetail(bookingId)
                    consecutiveFailures = 0
                    if (d != null) {
                        _detail.value = d
                        _detailNotFound.value = null
                        // Keep the list in sync too, so leaving the detail
                        // screen shows the latest status without a fresh poll.
                        mergeSummaries(listOf(d.toSummary()), isFullRefresh = false)
                        if (d.stage in TERMINAL_STAGES) {
                            Log.d(TAG, "[Detail] booking $bookingId reached terminal stage ${d.stage}, stopping")
                            break
                        }
                        backoffMs = if (d.stage in ACTIVE_STAGES) DETAIL_ACTIVE_INTERVAL_MS else DETAIL_PASSIVE_INTERVAL_MS
                    } else {
                        _detail.value = null
                        _detailNotFound.value = bookingId
                        // Remove from active list immediately
                        val byId = _bookings.value.associateBy { it.id }.toMutableMap()
                        if (byId.remove(bookingId) != null) {
                            _bookings.value = byId.values.sortedByDescending { it.updatedAt }
                        }
                        Log.d(TAG, "[Detail] booking $bookingId not found, stopping polling")
                        break
                    }
                } catch (e: Throwable) {
                    consecutiveFailures++
                    backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                    Log.w(TAG, "[Detail] poll failed ($consecutiveFailures consecutive) for $bookingId, backing off to ${backoffMs}ms", e)
                }
                delay(backoffMs)
            }
        }
    }

    fun closeDetail() {
        activeDetailId = null
        detailJob?.cancel()
        detailJob = null
        _detail.value = null
        _detailNotFound.value = null
    }

    /** Resume whichever loop(s) were active before the app went to background. */
    fun onAppForeground() {
        if (foregrounded) return
        foregrounded = true
        if (bookingsScreenVisible) {
            triggerListSync()
            startVisibleListPolling()
        }
        activeDetailId?.let { openDetail(it) }
    }

    fun onAppBackground() {
        foregrounded = false
        listJob?.cancel()
        listJob = null
        refreshJob?.cancel()
        refreshJob = null
        detailJob?.cancel()
        detailJob = null
    }

    /** Stops all polling and wipes cached state — call on logout/guest-mode switch. */
    fun clear() {
        sessionActive = false
        bookingsScreenVisible = false
        activeDetailId = null
        lastSyncIso = null
        hasCompletedListSync = false
        listJob?.cancel()
        listJob = null
        refreshJob?.cancel()
        refreshJob = null
        detailJob?.cancel()
        detailJob = null
        _bookings.value = emptyList()
        _detail.value = null
        _detailNotFound.value = null
    }
}
