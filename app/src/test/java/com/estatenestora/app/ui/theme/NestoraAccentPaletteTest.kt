package com.estatenestora.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class NestoraAccentPaletteTest {
    @Test
    fun `legacy theme keeps the original Nestora mint colour`() {
        val legacy = RoyalThemeRepository.legacyMintTheme

        assertEquals(Color(0xFF00382B), legacy.backgroundGradient.first())
        assertEquals(Color(0xFF00382B), legacy.backgroundGradient.last())
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
