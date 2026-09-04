package com.estatenestora.app.ui.screens

import com.estatenestora.app.data.model.AndroidBridgeResponse
import com.estatenestora.app.data.model.ListingServiceCatalog
import com.estatenestora.app.data.model.ProviderServiceOffering
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderPackageItemPayloadTest {
    @Test
    fun `package payload preserves each provider selected quantity and listing order`() {
        val offers = listOf(
            ProviderServiceOffering(id = "washer", title = "Washer", priceAmount = 99.0, durationMinutes = 10),
            ProviderServiceOffering(id = "valve", title = "Valve", priceAmount = 149.0, durationMinutes = 20),
            ProviderServiceOffering(id = "tap", title = "Tap", priceAmount = 249.0, durationMinutes = 25)
        )

        val payload = providerPackageItemPayloads(offers, mapOf("valve" to 2, "tap" to 5, "washer" to 0))

        assertEquals(2, payload.size())
        assertEquals("valve", payload[0].asJsonObject["offering_id"].asString)
        assertEquals(2, payload[0].asJsonObject["quantity"].asInt)
        assertEquals(0, payload[0].asJsonObject["display_order"].asInt)
        assertEquals("tap", payload[1].asJsonObject["offering_id"].asString)
        assertEquals(5, payload[1].asJsonObject["quantity"].asInt)
    }

    @Test
    fun `inline package work item resolves the new server item for quantity one selection`() {
        val existing = ProviderServiceOffering(id = "washer", title = "Washer", priceAmount = 99.0, durationMinutes = 10)
        val created = ProviderServiceOffering(id = "tap", title = "Tap", priceAmount = 249.0, durationMinutes = 25)
        val response = AndroidBridgeResponse(
            ok = true,
            intent = "provider_service_offering_saved",
            reply = "Work item saved",
            serviceCatalog = ListingServiceCatalog(
                listingId = "listing-1",
                providerId = "provider-1",
                serviceTypeId = "plumber",
                offerings = listOf(existing, created)
            )
        )

        assertEquals(created, newlyCreatedPackageOffering(setOf(existing.id), response))
    }

    @Test
    fun `failed or unchanged work item response is never auto selected`() {
        val existing = ProviderServiceOffering(id = "washer", title = "Washer", priceAmount = 99.0, durationMinutes = 10)
        val unchangedCatalog = ListingServiceCatalog(
            listingId = "listing-1", providerId = "provider-1", serviceTypeId = "plumber",
            offerings = listOf(existing)
        )

        assertNull(newlyCreatedPackageOffering(
            setOf(existing.id),
            AndroidBridgeResponse(false, "error", "Invalid work item", serviceCatalog = unchangedCatalog)
        ))
        assertNull(newlyCreatedPackageOffering(
            setOf(existing.id),
            AndroidBridgeResponse(true, "provider_service_offering_saved", "Saved", serviceCatalog = unchangedCatalog)
        ))
    }
}
