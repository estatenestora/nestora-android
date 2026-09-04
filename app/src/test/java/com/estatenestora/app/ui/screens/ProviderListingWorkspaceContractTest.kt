package com.estatenestora.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProviderListingWorkspaceContractTest {
    private fun source(fileName: String): String = listOf(
        File("app/src/main/java/com/estatenestora/app/ui/screens/$fileName"),
        File("src/main/java/com/estatenestora/app/ui/screens/$fileName")
    ).firstOrNull { it.exists() }?.readText() ?: error("$fileName was not found")

    @Test
    fun `listing card is concise and uses one authoritative booking total`() {
        val card = source("RegisterChoiceScreen.kt")
            .substringAfter("fun ProviderListingCard(")

        assertTrue(card.contains("listing.totalBookingCount"))
        assertFalse(card.contains("listing.openBookingCount + listing.requestedBookingCount"))
        assertFalse(card.contains("text = listing.description"))
    }

    @Test
    fun `preview edit and availability are separate destinations`() {
        val source = source("RegisterChoiceScreen.kt")
        val preview = source.substringAfter("private fun ProviderListingPreviewPage(")
            .substringBefore("private fun ProviderListingDetailSection(")
        val editor = source.substringAfter("// Dedicated listing editor.")
            .substringBefore("val pageSurface = remember")

        assertTrue(preview.contains("About this listing"))
        assertTrue(preview.contains("onClick = onAvailability"))
        assertTrue(preview.contains("onClick = onEdit"))
        assertFalse(editor.contains("Work Availability Settings"))
        assertFalse(editor.contains("Set Availability"))
        assertTrue(editor.contains("ListingEditorUpdate("))
    }

    @Test
    fun `listing editor limits pricing to database-supported choices`() {
        val editor = source("RegisterChoiceScreen.kt")
            .substringAfter("// Dedicated listing editor.")
            .substringBefore("val pageSurface = remember")

        assertTrue(editor.contains("listingPricingModelOptions.forEach"))
        assertFalse(editor.contains("onValueChange = { dPricingModel = it.uppercase() }"))
    }

    @Test
    fun `listings and bookings place filters before result content`() {
        val listings = source("RegisterChoiceScreen.kt")
        val listingWorkspace = listings.substringAfter("\"listings\" -> {")
            .substringBefore("\"availability\" -> {")
        assertTrue(listingWorkspace.indexOf("Horizontal Filter Track") < listingWorkspace.indexOf("LazyColumn Container for Discovery Cards"))

        val bookings = source("BookingsScreen.kt")
        assertTrue(bookings.indexOf("Filter buttons row") < bookings.indexOf("// 5. Tab content"))
    }
}
