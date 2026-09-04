package com.estatenestora.app.ui.screens

import com.estatenestora.app.data.model.ProviderDashboardSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderDashboardScreenTest {
    @Test
    fun `wallet at minus fifty blocks acceptance and becomes first action`() {
        val actions = providerDashboardAttention(healthySummary(walletBalance = -50.0))

        assertEquals(ProviderDashboardDestination.WALLET, actions.first().destination)
        assertTrue(actions.first().message.contains("above -₹50"))
    }

    @Test
    fun `wallet just above threshold does not show a wallet warning`() {
        val actions = providerDashboardAttention(healthySummary(walletBalance = -49.99))

        assertFalse(actions.any { it.destination == ProviderDashboardDestination.WALLET })
    }

    @Test
    fun `provider without a listing is directed to registration`() {
        val actions = providerDashboardAttention(
            healthySummary(totalListings = 0, activeListings = 0)
        )

        assertTrue(actions.any { it.destination == ProviderDashboardDestination.REGISTER })
        assertFalse(actions.any { it.destination == ProviderDashboardDestination.LISTINGS })
    }

    @Test
    fun `unseen requests and verification state produce actionable destinations`() {
        val actions = providerDashboardAttention(
            healthySummary(unseenRequests = 2, verificationStatus = "PENDING")
        )

        assertTrue(actions.any { it.destination == ProviderDashboardDestination.BOOKINGS })
        assertTrue(actions.any { it.destination == ProviderDashboardDestination.ACCOUNT })
    }

    @Test
    fun `published provider whose availability is off is directed to availability`() {
        val actions = providerDashboardAttention(healthySummary(isAvailable = false))

        assertTrue(actions.any {
            it.destination == ProviderDashboardDestination.AVAILABILITY &&
                it.title == "Customer requests are paused"
        })
    }

    @Test
    fun `healthy provider dashboard does not create false warnings`() {
        assertTrue(providerDashboardAttention(healthySummary()).isEmpty())
    }

    private fun healthySummary(
        walletBalance: Double = 0.0,
        totalListings: Int = 1,
        activeListings: Int = 1,
        unseenRequests: Int = 0,
        verificationStatus: String = "VERIFIED",
        isAvailable: Boolean = true
    ) = ProviderDashboardSummary(
        verificationStatus = verificationStatus,
        isAvailable = isAvailable,
        totalListings = totalListings,
        activeListings = activeListings,
        inactiveListings = (totalListings - activeListings).coerceAtLeast(0),
        unseenRequests = unseenRequests,
        walletBalance = walletBalance
    )
}
