package com.lifeos.app.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Juno's approved visual identity (2026-08-12 redesign — see docs/adr/0001
 * and progress/JUNO.md). Two deliberate brand hues, not one gradient
 * blended together: ember is Juno's own presence — warmth, attention,
 * action; verdigris is memory and trust — used for anything Juno
 * remembers or confirms. Warm umber/ivory neutrals replace the earlier
 * green + cool blue-violet palette, which read as visually
 * indistinguishable from Gemini/Copilot's own identity.
 */
object LifeOSColors {
    // Ember — presence, warmth, action. The Presence's hue at rest.
    val EmberPale = Color(0xFFF6D9A0)
    val Ember = Color(0xFFE0A044)
    val EmberDeep = Color(0xFFB97A26)
    val EmberBase = Color(0xFF7A4E12)

    // Verdigris — memory, trust, understanding. Presence hue while thinking.
    val VerdigrisPale = Color(0xFFB8E0D4)
    val Verdigris = Color(0xFF4FA490)
    val VerdigrisDeep = Color(0xFF2F6B5C)

    // Faint — used only for the Offline presence: dim, desaturated, clearly
    // "asleep," not a third brand hue.
    val FaintPale = Color(0xFF5A5248)
    val Faint = Color(0xFF3A342C)
    val FaintDeep = Color(0xFF241F19)

    // Light theme — warm ivory/linen, never a cold white.
    val LightBackground = Color(0xFFF6F0E4)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceRaised = Color(0xFFFBF7EE)
    val LightInk = Color(0xFF2A2118)
    val LightMuted = Color(0xFF5C5140)

    // Dark theme — warm umber near-black, not a neutral/blue-black.
    val DarkBackground = Color(0xFF1B1611)
    val DarkSurface = Color(0xFF251E17)
    val DarkSurfaceRaised = Color(0xFF2E251C)
    val DarkInk = Color(0xFFF3ECDD)
    val DarkMuted = Color(0xFFC7BBA5)

    // Semantic — kept distinct from both brand hues (Philosophy: an error
    // must never be confused with "this is the brand").
    val Danger = Color(0xFFA8432C)
    val Warn = Color(0xFFB9821F)
    val Success = Color(0xFF4C8C3B)
    val Emergency = Color(0xFF8F2E24)
}
