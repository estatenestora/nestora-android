package com.estatenestora.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NestoraNavigationPolicyTest {
    @Test
    fun `serve mode exposes five persistent provider workspaces in task order`() {
        val destinations = primaryDestinationsFor(isProviderMode = true)

        assertEquals(
            listOf(
                NestoraPrimaryDestination.Dashboard,
                NestoraPrimaryDestination.Register,
                NestoraPrimaryDestination.Listings,
                NestoraPrimaryDestination.Bookings,
                NestoraPrimaryDestination.Account
            ),
            destinations
        )
        assertEquals(destinations.size, destinations.distinct().size)
    }

    @Test
    fun `hire mode keeps bookings under account rather than primary navigation`() {
        val destinations = primaryDestinationsFor(isProviderMode = false)

        assertEquals(
            listOf(
                NestoraPrimaryDestination.Explore,
                NestoraPrimaryDestination.Finder,
                NestoraPrimaryDestination.Account
            ),
            destinations
        )
        assertFalse(destinations.contains(NestoraPrimaryDestination.Bookings))
        assertTrue(destinations.contains(NestoraPrimaryDestination.Account))
    }

    @Test
    fun `hire and serve navigation share only account`() {
        val hire = primaryDestinationsFor(isProviderMode = false).toSet()
        val serve = primaryDestinationsFor(isProviderMode = true).toSet()

        assertEquals(setOf(NestoraPrimaryDestination.Account), hire.intersect(serve))
    }

    @Test
    fun `booking and other focused journeys never show primary navigation`() {
        assertFalse(shouldShowPrimaryNavigation(false, "booking_detail", 1))
        assertFalse(shouldShowPrimaryNavigation(false, "booking_loader", 1))
        assertFalse(shouldShowPrimaryNavigation(true, "register_service", 0))
        assertFalse(shouldShowPrimaryNavigation(true, "map_picker", 0))
    }

    @Test
    fun `serve bottom navigation remains on every root workspace`() {
        assertTrue(shouldShowPrimaryNavigation(true, "dashboard", 0))
        assertTrue(shouldShowPrimaryNavigation(true, "register_choice", 0, selectedRegisterTab = 0))
        assertTrue(shouldShowPrimaryNavigation(true, "listings", 0))
        assertTrue(shouldShowPrimaryNavigation(true, "main", 2))
        assertTrue(shouldShowPrimaryNavigation(true, "main", 3))
    }

    @Test
    fun `nested journeys hide primary navigation in both modes`() {
        assertFalse(shouldShowPrimaryNavigation(true, "register_choice", 0, selectedRegisterTab = 1))
        assertFalse(shouldShowPrimaryNavigation(true, "listings", 0, nestedPageOpen = true))
        assertFalse(shouldShowPrimaryNavigation(false, "main", 1, selectedFinderTab = 1))
        assertFalse(shouldShowPrimaryNavigation(false, "main", 4))
    }

    @Test
    fun `provider operational workspaces are full screen destinations`() {
        assertTrue(isProviderFullScreenDestination("register_choice", 0))
        assertTrue(isProviderFullScreenDestination("listings", 0))
        assertTrue(isProviderFullScreenDestination("main", 2))
        assertFalse(isProviderFullScreenDestination("dashboard", 0))
    }

    @Test
    fun `root workspaces resolve the destination shown as selected`() {
        assertEquals(
            NestoraPrimaryDestination.Listings,
            primaryDestinationFor(true, "listings", 0)
        )
        assertEquals(
            NestoraPrimaryDestination.Account,
            primaryDestinationFor(true, "main", 3)
        )
        assertEquals(
            NestoraPrimaryDestination.Finder,
            primaryDestinationFor(false, "main", 1)
        )
        assertEquals(
            NestoraPrimaryDestination.Account,
            primaryDestinationFor(false, "main", 3)
        )
    }

    @Test
    fun `secondary actions remain complete without permanent top tabs`() {
        assertEquals(
            listOf(
                ProviderListingTool.Availability,
                ProviderListingTool.Packages,
                ProviderListingTool.Settings
            ),
            providerListingTools()
        )
        assertEquals("availability", ProviderListingTool.Availability.sectionId)
        assertEquals("packages", ProviderListingTool.Packages.sectionId)
        assertEquals("Work items & packages", ProviderListingTool.Packages.label)
        assertEquals("manage", providerListingSectionOrDefault("manage"))
        assertEquals("listings", providerListingSectionOrDefault("unknown"))

        val providerViews = bookingWorkspaceViews(isProviderMode = true)
        assertEquals(1, providerViews.first().tabIndex)
        assertEquals("Active requests", providerViews.first().label)
        assertEquals("History", providerViews.last().label)
    }

    @Test
    fun `listing filters hide down show up and remain visible at top`() {
        assertFalse(resolveScrollAwareFilterVisibility(0, 10, 0, 30, true))
        assertTrue(resolveScrollAwareFilterVisibility(2, 20, 2, 5, false))
        assertTrue(resolveScrollAwareFilterVisibility(1, 50, 0, 0, false))
        assertFalse(resolveScrollAwareFilterVisibility(2, 20, 2, 20, false))
    }
}
