package com.estatenestora.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftCreateRetryPolicyTest {
    @Test
    fun `only a lost first idempotent draft response retries`() {
        assertTrue(shouldRetryLostDraftCreateResponse(responseReceived = false, attempt = 0))
        assertFalse(shouldRetryLostDraftCreateResponse(responseReceived = true, attempt = 0))
        assertFalse(shouldRetryLostDraftCreateResponse(responseReceived = false, attempt = 1))
    }

    @Test
    fun `retry policy never retries a server response or an already retried request`() {
        assertFalse(shouldRetryLostDraftCreateResponse(responseReceived = true, attempt = 1))
        assertFalse(shouldRetryLostDraftCreateResponse(responseReceived = false, attempt = 2))
    }

    @Test
    fun `an idempotent draft step retries once only when its acknowledgement is lost`() {
        assertTrue(shouldRetryLostIdempotentDraftStep(responseReceived = false, attempt = 0))
        assertFalse(shouldRetryLostIdempotentDraftStep(responseReceived = true, attempt = 0))
        assertFalse(shouldRetryLostIdempotentDraftStep(responseReceived = false, attempt = 1))
    }
}
