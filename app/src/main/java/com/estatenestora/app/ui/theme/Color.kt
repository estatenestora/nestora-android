package com.estatenestora.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Nestora Brand Colors — Light Mint Theme (Figma-Inspired)
/**
 * Compatibility bridge for older screens that still use the original named
 * colours. The app chooses a RoyalTheme once, then this palette makes those
 * existing controls use that same family without each screen making a second
 * and potentially different choice.
 */
internal object NestoraAccentPalette {
    private var activeTheme: RoyalTheme = RoyalThemeRepository.legacyMintTheme

    fun apply(theme: RoyalTheme) {
        activeTheme = theme
    }

    val primary: Color get() = activeTheme.backgroundGradient.first()
    val secondary: Color get() = activeTheme.backgroundGradient.last()
    val subtle: Color get() = lerp(primary, Color.White, 0.90f)
    val soft: Color get() = lerp(primary, Color.White, 0.95f)
    val border: Color get() = lerp(primary, Color.White, 0.78f)
}

// These getters keep legacy screens in the session's selected colour family.
val NestoraMint: Color get() = NestoraAccentPalette.primary
val NestoraMintLight: Color get() = NestoraAccentPalette.primary
val NestoraMintDark: Color get() = NestoraAccentPalette.primary
val NestoraMintBg: Color get() = NestoraAccentPalette.subtle
val NestoraGreenSoft = Color(0xFF6FCF97)     // Soft green accent

// Legacy color aliases kept so existing screens don't break
val NestoraViolet = Color(0xFF7C3AED)
val NestoraCyan: Color get() = NestoraAccentPalette.primary
val NestoraEmerald: Color get() = NestoraAccentPalette.primary
val NestoraAmber = Color(0xFFFFB830)
val NestoraRose = Color(0xFFFF5C7A)

// Light Theme Base
val NestoraBg: Color get() = NestoraAccentPalette.soft
val NestoraCardWhite = Color(0xFFFFFFFF)     // Pure white cards
val NestoraCardSoft: Color get() = NestoraAccentPalette.soft
val NestoraBorderLight: Color get() = NestoraAccentPalette.border
val NestoraTextDark = Color(0xFF0D1A13)      // Near black text
val NestoraTextBody = Color(0xFF3D5A4F)      // Body text
val NestoraTextMuted = Color(0xFF8CAAA0)     // Muted/placeholder text

// Dark BG aliases for auth screen
val NestoraDarkBg = Color(0xFF0B1A14)
val NestoraCardDark = Color(0xFF122A1F)
val NestoraBorderDark = Color(0xFF1E3D2E)
val NestoraTextWhite = Color(0xFFF8FAFC)

// Semantic
val NestoraSuccess: Color get() = NestoraAccentPalette.primary
val NestoraError = Color(0xFFFF5C7A)
val NestoraWarning = Color(0xFFFFB830)
val NestoraInfo = Color(0xFF38BDF8)

/**
 * Encapsulates contrast-safe styling rules derived dynamically by the Rotation Engine.
 */
data class RoyalTheme(
    val name: String,
    val backgroundGradient: List<Color>,
    val activeTabCardBg: Color,
    val inactiveTabCardBg: Color,
    val searchBarAlphaTint: Color
)

/**
 * Scalable data structure representing a navigation menu item.
 */
data class NestoraTab(
    val id: String,
    val label: String,
    val iconEmoji: String,
    val visibleInHireMode: Boolean,
    val visibleInServeMode: Boolean
)

/**
 * Thread-safe Theme Matrix mapping Day-of-Week index to Royal Theme profiles.
 */
object RoyalThemeRepository {
    /** The production Nestora colour scheme, used unless dynamic preview theming is enabled. */
    val legacyMintTheme = RoyalTheme(
        name = "Nestora Mint",
        backgroundGradient = listOf(Color(0xFF064E3B), Color(0xFF0F766E)),
        activeTabCardBg = Color(0xFFE7F3EE),
        inactiveTabCardBg = Color(0x1FFFFFFF),
        searchBarAlphaTint = Color(0x0A000000)
    )

    val themes = listOf(
        RoyalTheme(
            name = "Imperial Sapphire",
            backgroundGradient = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B)),
            activeTabCardBg = Color(0xFFFFFFFF),
            inactiveTabCardBg = Color(0x1FFFFFFF),
            searchBarAlphaTint = Color(0x0A000000)
        ),
        RoyalTheme(
            name = "Royal Malachite",
            backgroundGradient = listOf(Color(0xFF0A2F1D), Color(0xFF144D32)),
            activeTabCardBg = Color(0xFFFFFFFF),
            inactiveTabCardBg = Color(0x1FFFFFFF),
            searchBarAlphaTint = Color(0x0A000000)
        ),
        RoyalTheme(
            name = "Burgundy Plum",
            backgroundGradient = listOf(Color(0xFF2B0F1A), Color(0xFF4A1E2F)),
            activeTabCardBg = Color(0xFFFFFFFF),
            inactiveTabCardBg = Color(0x1FFFFFFF),
            searchBarAlphaTint = Color(0x0A000000)
        ),
        RoyalTheme(
            name = "Obsidian Amber",
            backgroundGradient = listOf(Color(0xFF111111), Color(0xFF1F1A12)),
            activeTabCardBg = Color(0xFFFFFFFF),
            inactiveTabCardBg = Color(0x26FFFFFF),
            searchBarAlphaTint = Color(0x14FFFFFF)
        ),
        RoyalTheme(
            name = "Midnight Indigo",
            backgroundGradient = listOf(Color(0xFF130F26), Color(0xFF221C44)),
            activeTabCardBg = Color(0xFFFFFFFF),
            inactiveTabCardBg = Color(0x1FFFFFFF),
            searchBarAlphaTint = Color(0x0A000000)
        ),
        RoyalTheme(
            name = "Classic Teal Velvet",
            backgroundGradient = listOf(Color(0xFF08252B), Color(0xFF0F3A41)),
            activeTabCardBg = Color(0xFFFFFFFF),
            inactiveTabCardBg = Color(0x1FFFFFFF),
            searchBarAlphaTint = Color(0x0A000000)
        ),
        RoyalTheme(
            name = "Deep Amethyst",
            backgroundGradient = listOf(Color(0xFF1C1124), Color(0xFF2E1E3A)),
            activeTabCardBg = Color(0xFFFFFFFF),
            inactiveTabCardBg = Color(0x1FFFFFFF),
            searchBarAlphaTint = Color(0x0A000000)
        )
    )

    /**
     * Resolves the active theme using a deterministic day-of-week index.
     * Guaranteed safe across background process lifecycle deaths.
     */
    fun getThemeForToday(): RoyalTheme {
        val dayValue = java.time.LocalDate.now().dayOfWeek.value // Range: 1 to 7
        return themes[(dayValue - 1) % themes.size]
    }
}
