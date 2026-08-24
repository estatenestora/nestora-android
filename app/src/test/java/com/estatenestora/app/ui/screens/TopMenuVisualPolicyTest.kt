package com.estatenestora.app.ui.screens

import androidx.compose.ui.graphics.Color
import com.estatenestora.app.ui.theme.RoyalThemeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopMenuVisualPolicyTest {
    @Test
    fun `selected tab stays aligned while rendering in front of inactive tabs`() {
        val selected = topMenuLayer(isSelected = true)
        val inactive = topMenuLayer(isSelected = false)

        assertEquals(selected.scale, inactive.scale, 0f)
        assertEquals(selected.verticalOffsetDp, inactive.verticalOffsetDp)
        assertTrue(selected.zIndex > inactive.zIndex)
        assertEquals(1f, selected.alpha, 0f)
        assertTrue(inactive.alpha < selected.alpha)
    }

    @Test
    fun `selected page surface uses the foreground shade of the current theme`() {
        val theme = RoyalThemeRepository.legacyMintTheme
        val surface = selectedMenuSurface(theme)

        assertTrue(surface != Color.White)
        assertEquals(theme.backgroundGradient.last(), surface)
        assertEquals(surface, topMenuTabBackground(theme, isSelected = true))
    }

    @Test
    fun `dynamic themes keep the active tab distinct from recessed menu items`() {
        val theme = RoyalThemeRepository.themes.first()

        assertEquals(theme.backgroundGradient.last(), selectedMenuSurface(theme))
        assertEquals(theme.inactiveTabCardBg, topMenuTabBackground(theme, isSelected = false))
        assertTrue(topMenuTabBackground(theme, isSelected = true) != topMenuTabBackground(theme, isSelected = false))
    }
}
