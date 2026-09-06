package com.estatenestora.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullScreenModalSheetTest {
    @Test
    fun `short slow drawer drag returns to expanded position`() {
        assertFalse(
            shouldDismissFilterDrawer(
                offsetY = 80f,
                dismissThresholdPx = 120f,
                velocityY = 900f
            )
        )
    }

    @Test
    fun `drawer dismisses at distance threshold or fast downward swipe`() {
        assertTrue(shouldDismissFilterDrawer(120f, 120f, 0f))
        assertTrue(shouldDismissFilterDrawer(20f, 120f, 1_900f))
    }

    @Test
    fun `fast upward swipe never dismisses drawer`() {
        assertFalse(shouldDismissFilterDrawer(20f, 120f, -2_500f))
    }
}
