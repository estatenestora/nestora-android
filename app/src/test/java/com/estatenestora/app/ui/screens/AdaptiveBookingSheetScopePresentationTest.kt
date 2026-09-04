package com.estatenestora.app.ui.screens

import com.estatenestora.app.data.model.ProviderServiceOffering
import com.estatenestora.app.data.model.ProviderServicePackage
import com.estatenestora.app.data.model.ListingServiceCatalog
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveBookingSheetScopePresentationTest {
    @Test
    fun `customer scope details use a readable description and typed attributes`() {
        val attributes = JsonObject().apply {
            addProperty("pipe_material", "CPVC")
            addProperty("water_isolated", true)
        }
        val offer = ProviderServiceOffering(
            id = "offer-1", title = "Replace tap washer", description = "Washer and fitting work",
            attributeValues = attributes, priceAmount = 149.0, durationMinutes = 25
        )

        assertEquals(
            listOf("Includes: Washer and fitting work", "Details: Pipe Material: CPVC · Water Isolated: Yes"),
            providerOfferingCustomerDetails(offer)
        )
    }

    @Test
    fun `package scope keeps quantities visible`() {
        val offer = ProviderServiceOffering(
            id = "offer-1", title = "Replace tap washer", priceAmount = 149.0,
            durationMinutes = 25, quantity = 2
        )
        val pack = ProviderServicePackage(
            id = "package-1", name = "Kitchen refresh", packagePriceAmount = 299.0,
            durationMinutes = 50, items = listOf(offer)
        )

        assertEquals("2 x Replace tap washer", providerPackageItemsLabel(pack))
        assertEquals(298.0, providerPackageItemsTotal(pack), 0.001)
        assertEquals(0.0, providerPackageSavings(pack), 0.001)
        assertTrue(providerOfferingCustomerDetails(offer).isEmpty())
    }

    @Test
    fun `package savings compares its exact item quantities`() {
        val pack = ProviderServicePackage(
            id = "package-1", name = "Kitchen refresh", packagePriceAmount = 399.0,
            durationMinutes = 50,
            items = listOf(
                ProviderServiceOffering(id = "offer-1", title = "Tap washer", priceAmount = 149.0, durationMinutes = 25, quantity = 2),
                ProviderServiceOffering(id = "offer-2", title = "Sealant", priceAmount = 150.0, durationMinutes = 10, quantity = 1)
            )
        )

        assertEquals(448.0, providerPackageItemsTotal(pack), 0.001)
        assertEquals(49.0, providerPackageSavings(pack), 0.001)
    }

    @Test
    fun `package payload contains only the package id`() {
        val payload = customerServiceSelectionPayload(" package-1 ", emptyMap(), false)!!

        assertEquals("package-1", payload.get("package_id").asString)
        assertFalse(payload.has("items"))
        assertFalse(payload.has("use_listing_price"))
    }

    @Test
    fun `individual cart payload is deterministic and keeps quantities`() {
        val payload = customerServiceSelectionPayload(
            packageId = null,
            offeringQuantities = linkedMapOf("offer-b" to 2, "offer-a" to 1),
            useListingPrice = false
        )!!
        val items = payload.getAsJsonArray("items")

        assertEquals(2, items.size())
        assertEquals("offer-a", items[0].asJsonObject.get("offering_id").asString)
        assertEquals(1, items[0].asJsonObject.get("quantity").asInt)
        assertEquals("offer-b", items[1].asJsonObject.get("offering_id").asString)
        assertEquals(2, items[1].asJsonObject.get("quantity").asInt)
    }

    @Test
    fun `package and individual extras share one provider cart payload`() {
        val payload = customerServiceSelectionPayload("package-1", mapOf("offer-1" to 2), false)!!
        assertEquals("package-1", payload.get("package_id").asString)
        assertEquals(2, payload.getAsJsonArray("items")[0].asJsonObject.get("quantity").asInt)
    }

    @Test
    fun `invalid or custom mixed cart modes cannot be submitted`() {
        assertNull(customerServiceSelectionPayload(null, mapOf("offer-1" to 0), false))
        assertNull(customerServiceSelectionPayload(null, emptyMap(), false))
        assertNull(customerServiceSelectionPayload("package-1", emptyMap(), true))
    }

    @Test
    fun `individual cart summary totals provider amount and duration`() {
        val catalog = ListingServiceCatalog(
            listingId = "listing-1", providerId = "provider-1", serviceTypeId = "plumber",
            offerings = listOf(
                ProviderServiceOffering(id = "offer-1", title = "Replace washer", priceAmount = 150.0, durationMinutes = 20),
                ProviderServiceOffering(id = "offer-2", title = "Seal joint", priceAmount = 100.0, durationMinutes = 15)
            )
        )

        val summary = customerServiceCartSummary(
            catalog, null, mapOf("offer-1" to 2, "offer-2" to 1), false, 500.0, 60
        )!!

        assertEquals("ITEMS", summary.kind)
        assertEquals(3, summary.itemCount)
        assertEquals(400.0, summary.providerAmount, 0.001)
        assertEquals(55, summary.durationMinutes)
    }

    @Test
    fun `mixed cart summary combines package and extra item terms`() {
        val extra = ProviderServiceOffering(id = "offer-extra", title = "Extra fitting", priceAmount = 100.0, durationMinutes = 15)
        val pack = ProviderServicePackage(
            id = "package-1", name = "Bathroom refresh", packagePriceAmount = 500.0,
            durationMinutes = 60,
            items = listOf(ProviderServiceOffering(id = "offer-base", title = "Base repair", priceAmount = 450.0, durationMinutes = 60))
        )
        val catalog = ListingServiceCatalog(
            listingId = "listing-1", providerId = "provider-1", serviceTypeId = "plumber",
            offerings = listOf(extra), packages = listOf(pack)
        )

        val summary = customerServiceCartSummary(catalog, pack.id, mapOf(extra.id to 2), false, 0.0, 60)!!

        assertEquals("MIXED", summary.kind)
        assertEquals(3, summary.itemCount)
        assertEquals(700.0, summary.providerAmount, 0.001)
        assertEquals(90, summary.durationMinutes)
    }

    @Test
    fun `empty provider catalog still permits an explicit custom request`() {
        val catalog = ListingServiceCatalog(
            listingId = "listing-1", providerId = "provider-1", serviceTypeId = "plumber"
        )

        val payload = customerServiceSelectionPayload(null, emptyMap(), useListingPrice = true)!!
        val summary = customerServiceCartSummary(
            catalog, null, emptyMap(), true, listingPrice = 500.0, defaultDurationMinutes = 60
        )!!

        assertTrue(payload.get("use_listing_price").asBoolean)
        assertEquals("LISTING", summary.kind)
        assertEquals("Custom service request", summary.title)
        assertEquals(500.0, summary.providerAmount, 0.001)
    }
}
