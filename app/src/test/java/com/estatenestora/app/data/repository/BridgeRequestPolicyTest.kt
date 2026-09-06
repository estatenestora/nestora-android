package com.estatenestora.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeRequestPolicyTest {
    @Test
    fun `finder near me query carries the selected address bar coordinates`() {
        val query = NestoraRepository().withAddressBarCoordinates(
            payload = "plumber near me",
            addressBarLatitude = 22.566963,
            addressBarLongitude = 88.513828
        )

        assertEquals("SEARCH_AT::22.566963,88.513828::plumber near me", query)
    }

    @Test
    fun `service tile discovery carries the selected address bar coordinates`() {
        val query = NestoraRepository().withAddressBarCoordinates(
            payload = "SEARCH_SERVICE_TYPE::plumber",
            addressBarLatitude = 22.566963,
            addressBarLongitude = 88.513828
        )

        assertEquals("SEARCH_AT::22.566963,88.513828::SEARCH_SERVICE_TYPE::plumber", query)
    }

    @Test
    fun `finder does not invent coordinates when no address is selected`() {
        val query = NestoraRepository().withAddressBarCoordinates("plumber near me", null, null)

        assertEquals("plumber near me", query)
    }

    @Test
    fun `read-only queries keep the short deadline`() {
        assertTrue(BridgeRequestPolicy.isReadOnly("GET_BOOKING::booking-id"))
        assertTrue(BridgeRequestPolicy.isReadOnly("SEARCH_SERVICE_TYPE::plumber"))
        assertTrue(BridgeRequestPolicy.isReadOnly("need plumber near me"))
        assertEquals(8_000L, BridgeRequestPolicy.READ_TIMEOUT_MS)
    }

    @Test
    fun `state-changing queries use the confirmation deadline`() {
        assertFalse(BridgeRequestPolicy.isReadOnly("ACCEPT_BOOKING::booking-id"))
        assertFalse(BridgeRequestPolicy.isReadOnly("CONFIRM_PAYMENT::booking-id"))
        assertFalse(BridgeRequestPolicy.isReadOnly("SET_DRAFT_ANSWER::draft::key::value"))
        assertEquals(30_000L, BridgeRequestPolicy.WRITE_TIMEOUT_MS)
    }
}
