package com.estatenestora.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Nestora Design System – Typography
 *
 * A single source of truth for all text sizes, inspired by Urban Company's
 * consistent visual hierarchy. All screens must use these tokens instead of
 * raw `.sp` literals so that the entire app stays visually coherent.
 *
 * Token reference:
 *   displayLarge   – Hero numbers / large prices           (40sp, Black)
 *   displayMedium  – Balance / wallet amount               (32sp, Bold)
 *   headlineLarge  – Screen top-bar titles                 (22sp, Bold)
 *   headlineMedium – Card/section main titles              (18sp, Bold)
 *   headlineSmall  – Sub-headings, chip labels             (16sp, SemiBold)
 *   titleMedium    – Item titles, list-row labels          (15sp, Medium)
 *   titleSmall     – Secondary labels, address text        (14sp, Medium)
 *   bodyLarge      – Primary body copy                     (14sp, Normal)
 *   bodyMedium     – Description, bullet text              (13sp, Normal)
 *   bodySmall      – Timestamps, help text, meta           (12sp, Normal)
 *   labelLarge     – Button labels / CTA text              (15sp, Bold)
 *   labelMedium    – Badges, chips, tag labels             (10sp, Bold)
 *   labelSmall     – Fine print / legal                    (9sp,  Normal)
 */
val NestoraTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontSize = 10.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp
    )
)
