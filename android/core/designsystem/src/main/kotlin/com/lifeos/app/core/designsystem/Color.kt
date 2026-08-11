package com.lifeos.app.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * LifeOS's brand palette. Codifies the warm green + cream identity that
 * already emerged organically in the web/native prototypes — this is the
 * single source of truth going forward, not a new invention.
 */
object LifeOSColors {
    // Brand green
    val BrandGreen = Color(0xFF159A6B)
    val BrandGreenLight = Color(0xFF37C98F)
    val BrandGreenDark = Color(0xFF0E7A53)

    // Voice orb accent — was a standalone periwinkle-purple palette,
    // deliberately kept separate from the green brand. Shifted to share
    // hues with the web app's Juno gradient identity (brand green ->
    // accent blue -> accent violet) instead: same brand across platforms,
    // not two unrelated color languages that happen to be under one name.
    val VoiceAccentPale = Color(0xFFEFFBF5)
    val VoiceAccentLight = Color(0xFF9FE6C9)
    val VoiceAccent = Color(0xFF4A90D9)
    val VoiceAccentDeep = Color(0xFF8A7BFF)
    val VoiceAccentDark = Color(0xFF0E7A53)

    // Light theme
    val LightBackground = Color(0xFFF3F7F4)
    val LightSurface = Color(0xFFFFFFFF)
    val LightInk = Color(0xFF14231B)
    val LightMuted = Color(0xFF5B6B62)

    // Dark theme — neutral near-black graphite, not the old dark-green
    // tint, matching the same shift made on the web app: color should live
    // in the gradient accents, not the base background.
    val DarkBackground = Color(0xFF0A0C0B)
    val DarkSurface = Color(0xFF15191B)
    val DarkInk = Color(0xFFF3F6F4)
    val DarkMuted = Color(0xFF9AA39E)

    // Semantic — kept distinct from the brand accent per accessibility
    // guidance: an error must never be confused with "this is the brand."
    val Danger = Color(0xFFD64545)
    val Warn = Color(0xFFE0A100)
}
