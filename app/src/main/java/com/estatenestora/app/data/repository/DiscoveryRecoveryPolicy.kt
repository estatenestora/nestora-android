package com.estatenestora.app.data.repository

/**
 * Shared policy for read-only discovery data (feed, category and service
 * searches). It deliberately does not apply to booking, payment or provider
 * workflow commands: those always need the current server state.
 */
internal object DiscoveryRecoveryPolicy {
    const val CATALOG_TTL_MS = 5 * 60_000L
    const val FEED_TTL_MS = 45_000L
    const val SEARCH_TTL_MS = 30_000L

    private const val INITIAL_RETRY_DELAY_MS = 1_500L
    private const val MAX_RETRY_DELAY_MS = 30_000L

    fun isFresh(cachedAtMs: Long, nowMs: Long, ttlMs: Long): Boolean =
        cachedAtMs > 0 && nowMs >= cachedAtMs && nowMs - cachedAtMs < ttlMs

    /** A bounded exponential backoff prevents a down bridge being retried on every tap. */
    fun retryDelayMs(consecutiveFailures: Int): Long {
        val shift = (consecutiveFailures - 1).coerceIn(0, 4)
        return (INITIAL_RETRY_DELAY_MS shl shift).coerceAtMost(MAX_RETRY_DELAY_MS)
    }

    fun mayRetry(lastFailureAtMs: Long, consecutiveFailures: Int, nowMs: Long): Boolean =
        lastFailureAtMs <= 0 || nowMs - lastFailureAtMs >= retryDelayMs(consecutiveFailures)
}
