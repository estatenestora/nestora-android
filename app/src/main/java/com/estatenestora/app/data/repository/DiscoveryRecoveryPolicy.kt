package com.estatenestora.app.data.repository

/**
 * Shared policy for read-only discovery data (feed, category and service
 * searches). It deliberately does not apply to booking, payment or provider
 * workflow commands: those always need the current server state.
 */
internal object DiscoveryRecoveryPolicy {
    // Catalog definitions change only through an explicit admin write, which
    // reloads the backend singleton. Keep a stable app-session cache instead
    // of refetching unchanged categories/types during ordinary navigation.
    // Admin deactivation must disappear promptly for customers while still
    // avoiding a request on ordinary navigation within the same few minutes.
    const val CATALOG_TTL_MS = 5 * 60_000L
    const val FEED_TTL_MS = 5 * 60_000L
    const val PROVIDER_LISTINGS_TTL_MS = 5 * 60_000L
    const val SEARCH_TTL_MS = 30_000L
    const val EMPTY_LIST_TTL_MS = 30_000L

    private const val INITIAL_RETRY_DELAY_MS = 1_500L
    private const val MAX_RETRY_DELAY_MS = 30_000L

    fun isFresh(cachedAtMs: Long, nowMs: Long, ttlMs: Long): Boolean =
        cachedAtMs > 0 && nowMs >= cachedAtMs && nowMs - cachedAtMs < ttlMs

    fun cacheTtlMs(configuredTtlMs: Long, isEmptyList: Boolean): Long =
        if (isEmptyList) minOf(configuredTtlMs, EMPTY_LIST_TTL_MS) else configuredTtlMs

    fun isListingQuery(query: String): Boolean =
        query == "GET_MY_LISTINGS" ||
            query.contains("GET_FEED_SERVICES") ||
            query.contains("SEARCH_CATEGORY::") ||
            query.contains("SEARCH_SERVICE_TYPE::")

    /** A bounded exponential backoff prevents a down bridge being retried on every tap. */
    fun retryDelayMs(consecutiveFailures: Int): Long {
        val shift = (consecutiveFailures - 1).coerceIn(0, 4)
        return (INITIAL_RETRY_DELAY_MS shl shift).coerceAtMost(MAX_RETRY_DELAY_MS)
    }

    fun mayRetry(lastFailureAtMs: Long, consecutiveFailures: Int, nowMs: Long): Boolean =
        lastFailureAtMs <= 0 || nowMs - lastFailureAtMs >= retryDelayMs(consecutiveFailures)
}
