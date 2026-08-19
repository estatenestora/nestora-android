package com.estatenestora.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NestoraCyan,
    secondary = NestoraViolet,
    tertiary = NestoraEmerald,
    background = NestoraDarkBg,
    surface = NestoraCardDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = NestoraTextWhite,
    onSurface = NestoraTextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = NestoraViolet,
    secondary = NestoraCyan,
    tertiary = NestoraEmerald,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun NestoraTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode matching user preference
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
