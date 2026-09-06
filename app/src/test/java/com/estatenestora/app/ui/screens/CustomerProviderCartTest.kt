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
    fun `custom package services remain single-unit selections`() {
        val quantities = customerPackageItemSelection(catalog, mapOf(offer.id to 1), mapOf(offer.id to 3))
        val cart = customerCartFromSelection(listing, catalog, null, quantities, false)!!
        assertEquals(1, quantities[offer.id])
        assertEquals(100.0, cart.providerAmount, 0.001)
        assertEquals(15, cart.durationMinutes)
    }

    @Test
    fun `custom package selection excludes unavailable or package only services`() {
        val inactive = offer.copy(id = "inactive", isActive = false)
        val quantities = customerPackageItemSelection(
            catalog.copy(offerings = listOf(offer, inactive)), emptyMap(),
            mapOf("base" to 1, "inactive" to 1, offer.id to 2)
        )
        assertEquals(mapOf(offer.id to 1), quantities)
    }

    @Test
    fun `cart combines package and extras but stays provider scoped`() {
        val cart = customerCartFromSelection(
            listing, catalog, pack.id, mapOf(offer.id to 1), useListingPrice = false
        )!!

        assertEquals(2, cart.itemCount)
        assertEquals(600.0, cart.providerAmount, 0.001)
        assertTrue(customerCartMatchesCatalog(cart, catalog))
        assertFalse(customerCartMatchesCatalog(cart, catalog.copy(providerId = "provider-2")))
        assertFalse(customerCartMatchesCatalog(cart, catalog.copy(serviceTypeId = "electrician-id")))
    }
}
