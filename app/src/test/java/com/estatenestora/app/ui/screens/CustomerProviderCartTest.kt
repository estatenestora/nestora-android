package com.estatenestora.app.ui.screens

import com.estatenestora.app.data.model.ListingServiceCatalog
import com.estatenestora.app.data.model.ProviderServiceOffering
import com.estatenestora.app.data.model.ProviderServicePackage
import com.estatenestora.app.data.model.ServiceListing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerProviderCartTest {
    private val listing = ServiceListing(
        id = "listing-1", title = "Plumber", categoryName = "Home",
        serviceType = "plumber", providerName = "Ankush", price = 300.0,
        location = "New Town"
    )
    private val offer = ProviderServiceOffering(
        id = "offer-1", title = "Extra fitting", priceAmount = 100.0, durationMinutes = 15
    )
    private val pack = ProviderServicePackage(
        id = "package-1", name = "Bathroom refresh", packagePriceAmount = 500.0,
        durationMinutes = 60,
        items = listOf(ProviderServiceOffering(id = "base", title = "Base work", priceAmount = 450.0, durationMinutes = 60))
    )
    private val catalog = ListingServiceCatalog(
        listingId = listing.id, providerId = "provider-1", serviceTypeId = "plumber-id",
        offerings = listOf(offer), packages = listOf(pack)
    )

    @Test
    fun `cart combines package and extras but stays provider scoped`() {
        val cart = customerCartFromSelection(
            listing, catalog, pack.id, mapOf(offer.id to 2), useListingPrice = false
        )!!

        assertEquals(3, cart.itemCount)
        assertEquals(700.0, cart.providerAmount, 0.001)
        assertTrue(customerCartMatchesCatalog(cart, catalog))
        assertFalse(customerCartMatchesCatalog(cart, catalog.copy(providerId = "provider-2")))
        assertFalse(customerCartMatchesCatalog(cart, catalog.copy(serviceTypeId = "electrician-id")))
    }
}
