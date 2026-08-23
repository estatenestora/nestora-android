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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
 *   - List polling: 20s, only while the Bookings tab is open.
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
        private const val LIST_INTERVAL_MS = 20_000L
        private const val DETAIL_PASSIVE_INTERVAL_MS = 15_000L
        private const val DETAIL_ACTIVE_INTERVAL_MS = 5_000L
        private const val MAX_BACKOFF_MS = 60_000L
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
    private var lastSyncIso: String? = null
    private var wantsListPolling = false
    private var activeDetailId: String? = null
    private var foregrounded = true

    /** Called when the Bookings tab becomes visible/hidden (e.g. LaunchedEffect(selectedTab)). */
    fun startListPolling() {
        wantsListPolling = true
        if (!foregrounded) return
        if (listJob?.isActive == true) return
        listJob = scope.launch {
            var backoffMs = LIST_INTERVAL_MS
            var consecutiveFailures = 0
            while (isActive) {
                try {
                    val updates = if (lastSyncIso == null) {
                        repository.getMyBookings()
                    } else {
                        repository.getBookingUpdates(lastSyncIso)
                    }
                    consecutiveFailures = 0
                    backoffMs = LIST_INTERVAL_MS
                    mergeSummaries(updates, isFullRefresh = (lastSyncIso == null))
                } catch (e: Throwable) {
                    consecutiveFailures++
                    backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                    Log.w(TAG, "[List] poll failed ($consecutiveFailures consecutive), backing off to ${backoffMs}ms", e)
                }
                delay(backoffMs)
            }
        }
    }

    fun stopListPolling() {
        wantsListPolling = false
        listJob?.cancel()
        listJob = null
    }

    fun triggerListSync() {
        scope.launch {
            try {
                val updates = repository.getMyBookings()
                mergeSummaries(updates, isFullRefresh = true)
            } catch (e: Throwable) {
                Log.e(TAG, "Triggered list sync failed", e)
            }
        }
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
        if (wantsListPolling) startListPolling()
        activeDetailId?.let { openDetail(it) }
    }

    fun onAppBackground() {
        foregrounded = false
        listJob?.cancel()
        listJob = null
        detailJob?.cancel()
        detailJob = null
    }

    /** Stops all polling and wipes cached state — call on logout/guest-mode switch. */
    fun clear() {
        wantsListPolling = false
        activeDetailId = null
        lastSyncIso = null
        listJob?.cancel()
        listJob = null
        detailJob?.cancel()
        detailJob = null
        _bookings.value = emptyList()
        _detail.value = null
        _detailNotFound.value = null
    }
}
