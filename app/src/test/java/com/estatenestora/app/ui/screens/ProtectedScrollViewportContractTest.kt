package com.estatenestora.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProtectedScrollViewportContractTest {
    private fun source(fileName: String): String = listOf(
        File("app/src/main/java/com/estatenestora/app/ui/screens/$fileName"),
        File("src/main/java/com/estatenestora/app/ui/screens/$fileName")
    ).firstOrNull { it.exists() }?.readText() ?: error("$fileName was not found")

    @Test
    fun `provider dashboard list is clipped below its fixed carousel`() {
        val dashboard = source("ProviderDashboardScreen.kt")
        val content = dashboard.substringAfter("fun ProviderDashboardContent(")
            .substringBefore("private fun androidx.compose.foundation.lazy.LazyListScope.providerDashboardItems")

        assertTrue(content.contains(".clipToBounds()"))
    }

    @Test
    fun `active booking details are clipped below the fixed map`() {
        val booking = source("BookingDetailScreen.kt")
        val activeDetails = booking.substringAfter("// 2. Scrollable details area")
            .substringBefore("// Invisible click interceptor")

        assertTrue(activeDetails.contains(".clipToBounds()"))
    }

    @Test
    fun `completed booking owns one status bar inset without double padding`() {
        val booking = source("BookingDetailScreen.kt")
        val completed = booking.substringAfter("// 2A. Swiggy RATE & REVIEW FULL SCREEN UI")
            .substringBefore("// 2B. Swiggy MAP & TRACKING STATUS VIEW")
        val backButton = completed.substringAfter("// Back button overlaid")
            .substringBefore("Spacer(modifier = Modifier.height(16.dp))")

        assertTrue(completed.contains(".statusBarsPadding()"))
        assertFalse(backButton.contains(".statusBarsPadding()"))
    }
}
