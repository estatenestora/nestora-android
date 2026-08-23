package com.estatenestora.app.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListingEditorUpdateSerializationTest {
    @Test
    fun `editor update uses the backend snake case contract`() {
        val payload = Gson().toJson(
            ListingEditorUpdate(
                listingId = "listing-1",
                title = "Reliable plumber",
                tagline = "Same day repairs",
                description = "Leak and fixture repairs",
                basePrice = 500.0,
                pricingModel = "FIXED",
                currency = "INR",
                unitLabel = "per visit",
                platformNote = "Materials are quoted separately",
                isNegotiable = true,
                location = "New Town",
                city = "Kolkata",
                latitude = 22.57,
                longitude = 88.36,
                serviceRadiusKm = 8,
                attributes = mapOf("experience" to "5 years"),
                mediaUrls = listOf("https://example.test/service.jpg")
            )
        )

        assertTrue(payload.contains("\"listing_id\""))
        assertTrue(payload.contains("\"base_price\""))
        assertTrue(payload.contains("\"service_radius_km\""))
        assertTrue(payload.contains("\"media_urls\""))
        assertFalse(payload.contains("\"listingId\""))
        assertFalse(payload.contains("\"serviceRadiusKm\""))

        val decoded = Gson().fromJson(payload, Map::class.java)
        assertEquals("listing-1", decoded["listing_id"])
        assertEquals(8.0, decoded["service_radius_km"])
    }
}
