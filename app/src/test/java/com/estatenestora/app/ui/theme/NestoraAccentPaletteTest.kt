package com.estatenestora.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class NestoraAccentPaletteTest {
    @Test
    fun `default theme uses the fixed professional Nestora palette`() {
        val legacy = RoyalThemeRepository.legacyMintTheme

        assertEquals(Color(0xFF064E3B), legacy.backgroundGradient.first())
        assertEquals(Color(0xFF0F766E), legacy.backgroundGradient.last())
        assertEquals(Color(0xFFE7F3EE), legacy.activeTabCardBg)
    }

    @Test
    fun `legacy accent getters follow the selected royal theme`() {
        val selected = RoyalThemeRepository.themes.first { it.name == "Burgundy Plum" }

        NestoraAccentPalette.apply(selected)

        assertEquals(selected.backgroundGradient.first(), NestoraMint)
        assertEquals(selected.backgroundGradient.first(), NestoraMintDark)
        assertEquals(selected.backgroundGradient.last(), NestoraAccentPalette.secondary)
        assertEquals(Color.White, NestoraCardWhite)
    }
}
