package com.estatenestora.app.ui.screens

import com.estatenestora.app.data.model.ListingServiceCatalog
import com.estatenestora.app.data.model.ProviderServiceOffering
import com.estatenestora.app.data.model.ProviderServicePackage
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerCatalogSearchTest {
    private val tapRepair = ProviderServiceOffering(
        id = "tap", title = "Tap repair", description = "Stops leakage",
        attributeValues = JsonObject().apply { addProperty("work_category", "Bathroom fittings") },
        priceAmount = 299.0, durationMinutes = 20
    )
    private val pumpRepair = ProviderServiceOffering(
        id = "pump", title = "Water pump repair", description = "Motor diagnosis",
        priceAmount = 899.0, durationMinutes = 60
    )
    private val bathroomPackage = ProviderServicePackage(
        id = "bathroom", name = "Bathroom refresh", description = "Complete fitting care",
        packagePriceAmount = 499.0, durationMinutes = 90, items = listOf(tapRepair)
    )
    private val catalog = ListingServiceCatalog(
        listingId = "listing", providerId = "provider", serviceTypeId = "plumber",
        offerings = listOf(tapRepair, pumpRepair), packages = listOf(bathroomPackage)
    )

    @Test
    fun `catalog groups individual work by provider attribute`() {
        val result = customerCatalogSearch(catalog, query = "")

        assertEquals(listOf("Bathroom fittings", "Individual services"), result.offeringGroups.keys.toList())
        assertEquals(tapRepair, result.offeringGroups.getValue("Bathroom fittings").single())
    }

    @Test
    fun `search covers package details item details and provider context`() {
        assertEquals(listOf(tapRepair), customerCatalogSearch(catalog, "leakage").offeringGroups.values.flatten())
        assertEquals(listOf(bathroomPackage), customerCatalogSearch(catalog, "Tap repair").packages)

        val providerSearch = customerCatalogSearch(catalog, "plumber", providerContext = "Ankush plumber New Town")
        assertEquals(2, providerSearch.offeringGroups.values.flatten().size)
        assertEquals(1, providerSearch.packages.size)
    }

    @Test
    fun `package service and price filters exclude non matching results`() {
        val packagesOnly = customerCatalogSearch(catalog, "", mode = "PACKAGES")
        assertTrue(packagesOnly.offeringGroups.isEmpty())
        assertEquals(1, packagesOnly.packages.size)

        val affordableServices = customerCatalogSearch(catalog, "", mode = "SERVICES", maximumPrice = 500.0)
        assertTrue(affordableServices.packages.isEmpty())
        assertEquals(listOf(tapRepair), affordableServices.offeringGroups.values.flatten())
    }
}
