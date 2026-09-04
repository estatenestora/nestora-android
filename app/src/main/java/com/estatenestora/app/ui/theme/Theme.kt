package com.estatenestora.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * The single source of truth for the colour family selected when Nestora starts.
 * Screens that need a bespoke shade can read this rather than introducing a new
 * fixed accent colour.
 */
val LocalRoyalTheme = staticCompositionLocalOf { RoyalThemeRepository.getThemeForToday() }

private fun lightScheme(theme: RoyalTheme) = lightColorScheme(
    primary = theme.backgroundGradient.first(),
    onPrimary = Color.White,
    primaryContainer = lerp(theme.backgroundGradient.first(), Color.White, 0.88f),
    onPrimaryContainer = theme.backgroundGradient.first(),
    secondary = theme.backgroundGradient.last(),
    onSecondary = Color.White,
    secondaryContainer = lerp(theme.backgroundGradient.last(), Color.White, 0.88f),
    onSecondaryContainer = theme.backgroundGradient.last(),
    tertiary = theme.backgroundGradient.first(),
    onTertiary = Color.White,
    background = Color(0xFFF6F8F7),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = lerp(theme.backgroundGradient.last(), Color.White, 0.94f),
    outline = lerp(theme.backgroundGradient.first(), Color.White, 0.68f)
)

private fun darkScheme(theme: RoyalTheme) = darkColorScheme(
    primary = theme.backgroundGradient.last(),
    onPrimary = Color.White,
    secondary = theme.backgroundGradient.first(),
    onSecondary = Color.White,
    tertiary = theme.backgroundGradient.last(),
    background = NestoraDarkBg,
    onBackground = NestoraTextWhite,
    surface = NestoraCardDark,
    onSurface = NestoraTextWhite
)

@Composable
fun NestoraTheme(
    darkTheme: Boolean = true,
    royalTheme: RoyalTheme = RoyalThemeRepository.legacyMintTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkScheme(royalTheme) else lightScheme(royalTheme)
    val view = LocalView.current

    // Keep older named-colour reads synchronized before child content draws.
    NestoraAccentPalette.apply(royalTheme)

    // Give screens without a special header the same status-bar colour as the
    // selected Nestora colour family. Overlay screens can still override this
    // locally while they are open.
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = royalTheme.backgroundGradient.first().toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    CompositionLocalProvider(LocalRoyalTheme provides royalTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NestoraTypography,
            content = content
        )
    }
}
