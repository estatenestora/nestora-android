package com.estatenestora.app.ui.screens

import androidx.compose.ui.graphics.Color
import com.estatenestora.app.ui.theme.RoyalTheme

/**
 * Keeps the selected home-menu card visually in the foreground. The values
 * are intentionally presentation-only, so all home pages share one motion
 * contract without coupling their business navigation state.
 */
internal data class TopMenuLayer(
    val scale: Float,
    val verticalOffsetDp: Int,
    val zIndex: Float,
    val alpha: Float
)

internal fun topMenuLayer(isSelected: Boolean): TopMenuLayer =
    if (isSelected) {
        TopMenuLayer(scale = 1f, verticalOffsetDp = 0, zIndex = 2f, alpha = 1f)
    } else {
        TopMenuLayer(scale = 1f, verticalOffsetDp = 0, zIndex = 1f, alpha = 0.92f)
    }

/**
 * The header base uses the first (deeper) theme colour. The selected
 * destination uses the normal/foreground theme shade so it remains visibly
 * selected on every dynamic theme without becoming a white tile.
 */
internal fun selectedMenuSurface(theme: RoyalTheme): Color =
    theme.backgroundGradient.last()

/** The selected tab must read as part of its page canvas, never as a white tile. */
internal fun topMenuTabBackground(theme: RoyalTheme, isSelected: Boolean): Color =
    if (isSelected) selectedMenuSurface(theme) else theme.inactiveTabCardBg
