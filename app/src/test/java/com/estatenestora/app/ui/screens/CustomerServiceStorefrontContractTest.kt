package com.estatenestora.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CustomerServiceStorefrontContractTest {
    private fun source(fileName: String): String = listOf(
        File("app/src/main/java/com/estatenestora/app/ui/screens/$fileName"),
        File("src/main/java/com/estatenestora/app/ui/screens/$fileName")
    ).firstOrNull { it.exists() }?.readText() ?: error("$fileName was not found")

    private fun mainActivitySource(): String = listOf(
        File("app/src/main/java/com/estatenestora/app/MainActivity.kt"),
        File("src/main/java/com/estatenestora/app/MainActivity.kt")
    ).firstOrNull { it.exists() }?.readText() ?: error("MainActivity.kt was not found")

    @Test
    fun `provider storefront separates packages items and cart review`() {
        val storefront = source("CustomerServiceCatalogScreen.kt")

        assertTrue(storefront.contains("StorefrontSearchPanel("))
        assertTrue(storefront.contains("StorefrontItemsNavigator("))
        assertTrue(storefront.contains("StorefrontBlock.Package"))
        assertTrue(storefront.contains("StorefrontBlock.Offering"))
        assertTrue(storefront.contains("Text(\"ITEMS\""))
        assertTrue(storefront.contains("CustomerCartReview("))
        assertTrue(storefront.contains("StorefrontCartBar("))
    }

    @Test
    fun `explore and finder headers keep the cart entry visible when empty`() {
        val main = mainActivitySource()
        val header = source("OnboardingHeader.kt")
        val profile = source("ProfileScreen.kt")

        assertTrue(header.contains("contentDescription = \"Open cart\""))
        assertTrue(header.contains("if (onCartClick != null)"))
        assertTrue(header.contains("modifier = Modifier.size(46.dp)"))
        assertTrue(main.contains("cartItemCount = customerCart?.itemCount ?: 0"))
        assertTrue(main.contains("onCartClick = openCustomerCart"))
        assertTrue(main.contains("Your cart is empty. Open a provider to add services."))
        assertFalse(profile.contains("cartItemCount: Int"))
        assertFalse(main.contains("SmallFloatingActionButton("))
    }

    @Test
    fun `storefront checkout applies selection before showing live time choices`() {
        val booking = source("AdaptiveBookingSheet.kt")
        val startup = booking.substringAfter("LaunchedEffect(listing.id) {")
            .substringBefore("// Booking availability is short-lived data")

        assertTrue(startup.contains("val initialSelection = customerServiceSelectionPayload("))
        assertTrue(startup.contains("val savedSelection = onSetServiceSelection("))
        assertTrue(startup.contains("serviceSelectionApplied = true"))
        assertTrue(startup.contains("onFetchDraftAvailability("))
    }
}
