package com.estatenestora.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryRecoveryPolicyTest {
    @Test
    fun `successful discovery data is fresh only inside its ttl`() {
        val cachedAt = 10_000L

        assertTrue(
            DiscoveryRecoveryPolicy.isFresh(
                cachedAt,
                cachedAt + DiscoveryRecoveryPolicy.FEED_TTL_MS - 1,
                DiscoveryRecoveryPolicy.FEED_TTL_MS
            )
        )
        assertFalse(
            DiscoveryRecoveryPolicy.isFresh(
                cachedAt,
                cachedAt + DiscoveryRecoveryPolicy.FEED_TTL_MS,
                DiscoveryRecoveryPolicy.FEED_TTL_MS
            )
        )
    }

    @Test
    fun `an outage is not retried on every service tap`() {
        val failedAt = 100_000L
        val retryDelay = DiscoveryRecoveryPolicy.retryDelayMs(consecutiveFailures = 1)

        assertEquals(1_500L, retryDelay)
        assertFalse(DiscoveryRecoveryPolicy.mayRetry(failedAt, 1, failedAt + retryDelay - 1))
        assertTrue(DiscoveryRecoveryPolicy.mayRetry(failedAt, 1, failedAt + retryDelay))
    }

    @Test
    fun `repeated outages back off but remain bounded for recovery`() {
        assertEquals(3_000L, DiscoveryRecoveryPolicy.retryDelayMs(consecutiveFailures = 2))
        assertEquals(24_000L, DiscoveryRecoveryPolicy.retryDelayMs(consecutiveFailures = 5))
        assertEquals(24_000L, DiscoveryRecoveryPolicy.retryDelayMs(consecutiveFailures = 20))
    }

    @Test
    fun `empty listing results expire sooner than populated results`() {
        assertEquals(
            DiscoveryRecoveryPolicy.EMPTY_LIST_TTL_MS,
            DiscoveryRecoveryPolicy.cacheTtlMs(DiscoveryRecoveryPolicy.FEED_TTL_MS, isEmptyList = true)
        )
        assertEquals(
            DiscoveryRecoveryPolicy.FEED_TTL_MS,
            DiscoveryRecoveryPolicy.cacheTtlMs(DiscoveryRecoveryPolicy.FEED_TTL_MS, isEmptyList = false)
        )
    }

    @Test
    fun `listing invalidation preserves unrelated catalog cache entries`() {
        assertTrue(DiscoveryRecoveryPolicy.isListingQuery("GET_MY_LISTINGS"))
        assertTrue(DiscoveryRecoveryPolicy.isListingQuery("SEARCH_AT::19.076000,72.877700::GET_FEED_SERVICES"))
        assertTrue(DiscoveryRecoveryPolicy.isListingQuery("SEARCH_CATEGORY::home-services"))
        assertFalse(DiscoveryRecoveryPolicy.isListingQuery("GET_ALL_SERVICE_TYPES"))
        assertFalse(DiscoveryRecoveryPolicy.isListingQuery("GET_CATEGORIES"))
    }
}
