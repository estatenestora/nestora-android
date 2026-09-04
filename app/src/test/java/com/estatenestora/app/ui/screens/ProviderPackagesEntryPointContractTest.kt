package com.estatenestora.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProviderPackagesEntryPointContractTest {
    private fun registerChoiceSource(): String {
        return listOf(
            File("app/src/main/java/com/estatenestora/app/ui/screens/RegisterChoiceScreen.kt"),
            File("src/main/java/com/estatenestora/app/ui/screens/RegisterChoiceScreen.kt")
        ).firstOrNull { it.exists() }?.readText() ?: error("RegisterChoiceScreen.kt was not found")
    }

    private fun mainActivitySource(): String {
        return listOf(
            File("app/src/main/java/com/estatenestora/app/MainActivity.kt"),
            File("src/main/java/com/estatenestora/app/MainActivity.kt")
        ).firstOrNull { it.exists() }?.readText() ?: error("MainActivity.kt was not found")
    }

    @Test
    fun `listings header keeps a visible management action and package route`() {
        val source = registerChoiceSource()

        assertTrue(source.contains("actionLabel = \"Manage\""))
        assertTrue(source.contains("onAction = { activeListingsSubTab = \"manage\" }"))
        assertTrue(source.contains("activeListingsSubTab = \"packages\""))
        assertTrue(source.contains("activeListingsSubTab = \"availability\""))
    }

    @Test
    fun `package editor state changes do not use early composable returns`() {
        val source = registerChoiceSource()
        val workspace = source.substringAfter("private fun ProviderPackagesWorkspace(")
            .substringBefore("private fun ProviderCatalogEmptyState(")

        assertTrue(workspace.contains("when (section)"))
        assertFalse(workspace.contains("return@Column"))
    }

    @Test
    fun `serve listings route wires package catalog reads and writes`() {
        val source = mainActivitySource()
        val listingsRoute = source.substringAfter("ProviderListingsScreen(")
            .substringBefore("\"register_choice\" -> {")

        assertTrue(listingsRoute.contains("onFetchProviderServiceCatalog"))
        assertTrue(listingsRoute.contains("onSaveProviderServiceOffering"))
        assertTrue(listingsRoute.contains("onSaveProviderServicePackage"))
        assertTrue(listingsRoute.contains("onFetchServiceAttributes"))
    }
}
