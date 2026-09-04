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
        TopMenuLayer(scale = 1f, verticalOffsetDp = 0, zIndex = 1f, alpha = 0.72f)
    }

/** A shape cue keeps selection clear even when two theme shades are similar. */
internal fun topMenuIndicatorColor(isSelected: Boolean): Color =
    if (isSelected) Color.White else Color.Transparent

/**
 * The header base uses the first (deeper) theme colour. The selected
 * destination uses the normal/foreground theme shade so it remains visibly
 * selected on every dynamic theme without becoming a white tile.
 */
internal fun selectedMenuSurface(theme: RoyalTheme): Color =
    theme.backgroundGradient.last()

/**
 * The selected tab continues into the selected page canvas. Inactive tabs are
 * intentionally darker/recessed so the destination is never mistaken for an
 * unselected card.
 */
internal fun topMenuTabBackground(theme: RoyalTheme, isSelected: Boolean): Color =
    if (isSelected) selectedMenuSurface(theme) else theme.inactiveTabCardBg
