package com.estatenestora.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingPaymentPolicyTest {
    @Test
    fun `provider cancellation is offered only after paid confirmation`() {
        assertTrue(canProviderCancelPaidConfirmedBooking("CONFIRMED", "2026-08-26T10:00:00Z"))
        assertFalse(canProviderCancelPaidConfirmedBooking("CONFIRMED", null))
        assertFalse(canProviderCancelPaidConfirmedBooking("PAYMENT_PENDING", "2026-08-26T10:00:00Z"))
        assertFalse(canProviderCancelPaidConfirmedBooking("PROVIDER_EN_ROUTE", "2026-08-26T10:00:00Z"))
    }

    @Test
    fun `customer cancellation ends when provider begins commuting`() {
        assertTrue(canCustomerCancelBeforeProviderCommutes("CONFIRMED"))
        assertFalse(canCustomerCancelBeforeProviderCommutes("PROVIDER_EN_ROUTE"))
        assertFalse(canCustomerCancelBeforeProviderCommutes("SERVICE_STARTED"))
    }
}
