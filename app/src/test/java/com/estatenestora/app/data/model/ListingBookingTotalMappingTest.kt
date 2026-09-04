package com.estatenestora.app.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ListingBookingTotalMappingTest {
    @Test
    fun `provider listing keeps the authoritative lifetime booking total`() {
        val bridgeListing = Gson().fromJson(
            """{"listing_id":"listing-1","title":"Electrician","description":"","provider_name":"Ankush","service_type":"electrician","category":"home_services","city":"Kolkata","area":"","total_booking_count":9,"open_booking_count":3,"requested_booking_count":1}""",
            AndroidBridgeListing::class.java
        )

        val listing = bridgeListing.toServiceListing()

        assertEquals(9, listing.totalBookingCount)
        assertEquals(3, listing.openBookingCount)
        assertEquals(1, listing.requestedBookingCount)
    }
}
