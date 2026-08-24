package com.estatenestora.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingPollingPolicyTest {
    @Test
    fun `list polling runs only for a foreground visible bookings screen`() {
        assertTrue(shouldRunBookingListPolling(foregrounded = true, bookingsScreenVisible = true))
        assertFalse(shouldRunBookingListPolling(foregrounded = false, bookingsScreenVisible = true))
        assertFalse(shouldRunBookingListPolling(foregrounded = true, bookingsScreenVisible = false))
        assertFalse(shouldRunBookingListPolling(foregrounded = false, bookingsScreenVisible = false))
    }
}
