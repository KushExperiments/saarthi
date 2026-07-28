package com.saarthi.app.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Saarthi's brand palette. Codifies the warm green + cream identity that
 * already emerged organically in the web/native prototypes — this is the
 * single source of truth going forward, not a new invention.
 */
object SaarthiColors {
    // Brand green
    val BrandGreen = Color(0xFF159A6B)
    val BrandGreenLight = Color(0xFF37C98F)
    val BrandGreenDark = Color(0xFF0E7A53)

    // Voice orb accent — the orb's own jewel-tone identity (glossy violet
    // sphere, matching the AI-assistant reference look), deliberately kept
    // separate from the green brand used everywhere else in the app.
    val VoiceAccentLight = Color(0xFFB4A5FF)
    val VoiceAccent = Color(0xFF7B61FF)
    val VoiceAccentDark = Color(0xFF4B2FBE)

    // Light theme
    val LightBackground = Color(0xFFF3F7F4)
    val LightSurface = Color(0xFFFFFFFF)
    val LightInk = Color(0xFF14231B)
    val LightMuted = Color(0xFF5B6B62)

    // Dark theme
    val DarkBackground = Color(0xFF0F1512)
    val DarkSurface = Color(0xFF182320)
    val DarkInk = Color(0xFFEAF3EE)
    val DarkMuted = Color(0xFF9DB0A7)

    // Semantic — kept distinct from the brand accent per accessibility
    // guidance: an error must never be confused with "this is the brand."
    val Danger = Color(0xFFD64545)
    val Warn = Color(0xFFE0A100)
}
