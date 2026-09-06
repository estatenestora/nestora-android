package com.estatenestora.app.data.model

import com.google.gson.Gson
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMediaCompatibilityTest {
    @Test
    fun `missing and null gallery fields are safe for older catalog responses`() {
        val catalog = Gson().fromJson(
            """
            {
              "listing_id":"listing-1",
              "provider_id":"provider-1",
              "service_type_id":"service-1",
              "offerings":[{
                "id":"offering-1",
                "title":"Haircut",
                "price_amount":199,
                "duration_minutes":30,
                "media_gallery":null
              }],
              "packages":[{
                "id":"package-1",
                "name":"Salon package",
                "package_price_amount":499,
                "duration_minutes":60,
                "items":[]
              }],
              "listing_gallery":null
            }
            """.trimIndent(),
            ListingServiceCatalog::class.java
        )

        assertTrue(catalog.listingGallery.orEmpty().isEmpty())
        assertTrue(catalog.offerings.single().mediaGallery.orEmpty().isEmpty())
        assertTrue(catalog.packages.single().mediaGallery.orEmpty().isEmpty())
    }
}
