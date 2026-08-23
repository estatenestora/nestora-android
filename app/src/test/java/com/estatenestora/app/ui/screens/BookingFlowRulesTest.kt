package com.estatenestora.app.ui.screens

import com.estatenestora.app.data.model.AvailabilitySlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingFlowRulesTest {
    @Test
    fun `uses current address bar coordinates when GPS has no fix`() {
        val actual = resolveBookingCoordinates(null, null, 22.570428, 88.509248, 0.0, 0.0)
        assertEquals(22.570428, actual.latitude, 0.0)
        assertEquals(88.509248, actual.longitude, 0.0)
    }

    @Test
    fun `explicit booking map selection takes precedence over address bar`() {
        val actual = resolveBookingCoordinates(22.58, 88.52, 22.570428, 88.509248, 0.0, 0.0)
        assertEquals(22.58, actual.latitude, 0.0)
        assertEquals(88.52, actual.longitude, 0.0)
    }

    @Test
    fun `never combines partial address coordinates with GPS`() {
        val actual = resolveBookingCoordinates(null, null, 22.570428, null, 22.6, 88.6)
        assertEquals(22.6, actual.latitude, 0.0)
        assertEquals(88.6, actual.longitude, 0.0)
    }

    @Test
    fun `received booking keeps provider inbox visible while profile hint is stale`() {
        assertTrue(shouldShowProviderInbox(false, 1))
        assertTrue(shouldShowProviderInbox(true, 0))
        assertFalse(shouldShowProviderInbox(false, 0))
    }

    @Test
    fun `booking list polling only runs on the visible bookings tab`() {
        assertTrue(shouldRunBookingListPolling(true, "main", 2))
        assertFalse(shouldRunBookingListPolling(true, "main", 1))
        assertFalse(shouldRunBookingListPolling(true, "booking_detail", 2))
        assertFalse(shouldRunBookingListPolling(false, "main", 2))
    }

    @Test
    fun `listing cannot be made inactive while any booking is unresolved`() {
        assertTrue(canDeactivateListing(0))
        assertFalse(canDeactivateListing(1))
        assertFalse(canDeactivateListing(7))
    }

    @Test
    fun `booking start error identifies policy versus draft response failure`() {
        assertEquals(
            "listing is not available for booking",
            bookingStartFailureMessage(false, "listing is not available for booking", null)
        )
        assertEquals(
            "Nestora did not return the booking-form confirmation. Reopen this service to continue; no provider request or payment was submitted.",
            bookingStartFailureMessage(true, null, null)
        )
        assertEquals(
            "customer cannot book own listing",
            bookingStartFailureMessage(true, null, "customer cannot book own listing")
        )
    }

    @Test
    fun `booking submit error identifies the failed form step`() {
        assertEquals(
            "Nestora did not confirm service time. Your request was not submitted; reopen the form and try again.",
            bookingSubmissionFailureMessage("service time", null)
        )
        assertEquals(
            "This slot is no longer available.",
            bookingSubmissionFailureMessage("request submission", "This slot is no longer available.")
        )
    }

    @Test
    fun `changing preferred start also resets end using service duration`() {
        assertEquals(
            "2026-08-21T15:15+05:30",
            preferredWindowEndForStart("2026-08-21T14:15+05:30", 60)
        )
        assertEquals(
            "2026-08-21T14:25+05:30",
            preferredWindowEndForStart("2026-08-21T14:15+05:30", 0)
        )
        assertEquals(null, preferredWindowEndForStart("not-a-time", 60))
    }

    @Test
    fun `live flexible label uses the provider slot rather than a generic future date`() {
        assertEquals(
            "Fri, Aug 21 09:00",
            liveAvailabilitySlotLabel(AvailabilitySlot("2026-08-21T03:30:00Z", "2026-08-21T04:30:00Z"))
        )
        assertEquals("Available time", liveAvailabilitySlotLabel(AvailabilitySlot("bad", "bad")))
    }

    @Test
    fun `availability makes the full service duration explicit`() {
        assertEquals("1 hour", availabilityDurationLabel(60))
        assertEquals("2 hours", availabilityDurationLabel(120))
        assertEquals("45 minutes", availabilityDurationLabel(45))
        assertEquals("15 minutes", availabilityDurationLabel(0))
    }

    @Test
    fun locationRegistrationGapDisablesFreeTextWhileNormalTextRemainsAvailable() {
        assertTrue(requiresChoiceOnlyRegistrationInput("text", "location", "__location__"))
        assertTrue(requiresChoiceOnlyRegistrationInput("select", "attribute", "work_arrangement"))
        assertFalse(requiresChoiceOnlyRegistrationInput("text", "attribute", "description"))
    }

    @Test
    fun quoteScopesRejectContactDetailsAndLinks() {
        listOf(
            "Call me on +91 98765 43210",
            "Call 9134733four",
            "Email provider@example.com",
            "Visit https://example.com",
            "WhatsApp me for details",
            "Message @provider"
        ).forEach { unsafe ->
            assertTrue("Expected unsafe quote to be rejected: $unsafe", quoteScopeSafetyError(unsafe) != null)
        }
        assertEquals(null, quoteScopeSafetyError("Replace inlet valve and pressure-test the line"))
    }

    @Test
    fun requestedBookingCopyIdentifiesThePartyWhoMustAct() {
        val providerCopy = requestedBookingStatusCopy(true, "Ritesh")
        assertEquals("New service request", providerCopy.header)
        assertTrue(providerCopy.subtext.contains("Review the request"))

        val customerCopy = requestedBookingStatusCopy(false, "Rtn")
        assertEquals("Request sent!", customerCopy.header)
        assertEquals("Rtn is reviewing your request.", customerCopy.subtext)
    }

    @Test
    fun `provider can quote only while requested and no proposal is awaiting customer`() {
        assertTrue(canProviderSendServiceQuote(true, "REQUESTED", null))
        assertTrue(canProviderSendServiceQuote(true, "REQUESTED", "REJECTED"))
        assertTrue(canProviderSendServiceQuote(true, "REQUESTED", "EXPIRED"))
        assertFalse(canProviderSendServiceQuote(true, "REQUESTED", "PROPOSED"))
        assertFalse(canProviderSendServiceQuote(true, "PAYMENT_PENDING", "ACCEPTED"))
        assertFalse(canProviderSendServiceQuote(false, "REQUESTED", null))
    }

    @Test
    fun `customer can decide only the current live proposal before payment`() {
        assertTrue(canCustomerDecideServiceQuote("REQUESTED", "PROPOSED", true))
        assertFalse(canCustomerDecideServiceQuote("PAYMENT_PENDING", "PROPOSED", true))
        assertFalse(canCustomerDecideServiceQuote("REQUESTED", "ACCEPTED", true))
        assertFalse(canCustomerDecideServiceQuote("REQUESTED", "PROPOSED", false))
    }

    @Test
    fun `finder main menu tab click redirects to finder home instead of chat`() {
        assertTrue(shouldOpenFinderHomeInsteadOfChat(true))
        // Negative test case: if it wasn't the finder menu click, it doesn't redirect
        assertFalse(shouldOpenFinderHomeInsteadOfChat(false))
    }

    @Test
    fun `assistant tab does not show nestora ai chat`() {
        // Negative test case: Assistant tab must NEVER show Nestora AI Chat now
        assertFalse(shouldShowAIChatInAssistantTab(1))
        assertFalse(shouldShowAIChatInAssistantTab(0))
    }

    @Test
    fun `finder cards only show in assistant tab and finder tab remains clean`() {
        assertTrue(shouldShowFinderCards(1)) // Assistant tab shows the cards
        // Negative test case: Finder tab (0) must remain clean (not show cards)
        assertFalse(shouldShowFinderCards(0))
    }

    @Test
    fun `formatListingBookingsCount returns correct total sum`() {
        assertEquals("Bookings: 5", formatListingBookingsCount(3, 2))
        assertEquals("Bookings: 0", formatListingBookingsCount(0, 0))
        
        // Negative test case: handles negative values by coercing to 0
        assertEquals("Bookings: 0", formatListingBookingsCount(-1, -5))
        assertEquals("Bookings: 3", formatListingBookingsCount(3, -2))
    }

    @Test
    fun `formatListingRating returns correctly formatted float value`() {
        assertEquals("4.5", formatListingRating(4.5f))
        assertEquals("0.0", formatListingRating(0.0f))
        
        // Negative test cases: coerces values outside [0, 5] range
        assertEquals("0.0", formatListingRating(-1.5f))
        assertEquals("5.0", formatListingRating(6.2f))
    }
}

