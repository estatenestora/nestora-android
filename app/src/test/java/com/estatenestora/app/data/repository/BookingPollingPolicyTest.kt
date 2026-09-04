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

    @Test
    fun `bookings route performs the initial provider inbox refresh`() {
        assertTrue(shouldRefreshBookingsForRoute(authenticated = true, activeScreen = "main", selectedTab = 2))
        assertFalse(shouldRefreshBookingsForRoute(authenticated = false, activeScreen = "main", selectedTab = 2))
        assertFalse(shouldRefreshBookingsForRoute(authenticated = true, activeScreen = "booking_detail", selectedTab = 2))
        assertFalse(shouldRefreshBookingsForRoute(authenticated = true, activeScreen = "main", selectedTab = 1))
    }

    @Test
    fun `FCM refreshes only the booking detail currently open`() {
        assertTrue(shouldRefreshOpenBookingDetailForEvent("booking-1", "booking-1"))
        assertFalse(shouldRefreshOpenBookingDetailForEvent("booking-1", "booking-2"))
        assertFalse(shouldRefreshOpenBookingDetailForEvent(null, "booking-1"))
    }

    @Test
    fun `FCM does not query the list while Bookings is not visible`() {
        assertTrue(shouldRefreshBookingListForEvent(true, true, true))
        assertFalse(shouldRefreshBookingListForEvent(true, true, false))
        assertFalse(shouldRefreshBookingListForEvent(true, false, true))
        assertFalse(shouldRefreshBookingListForEvent(false, true, true))
    }
}
