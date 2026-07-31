package com.lifeos.app.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln

/**
 * A real elevation scale — previously every card/sheet just hardcoded its
 * own `elevation = 1.dp` inline. Named levels give every surface a
 * consistent sense of depth instead of an arbitrary per-screen number.
 */
object LifeOSElevation {
    val Resting: Dp = 1.dp
    val Raised: Dp = 4.dp
    val Overlay: Dp = 8.dp

    /**
     * On a near-black background, a drop shadow barely registers — Material's
     * own dark-theme guidance is to lighten the surface instead of deepening
     * the shadow. In light mode elevation still reads fine via shadow alone,
     * so this only does something in dark mode.
     */
    fun surfaceTint(darkTheme: Boolean, elevation: Dp, baseSurface: Color): Color {
        if (!darkTheme || elevation <= 0.dp) return baseSurface
        // Approximates Material's elevation-overlay alpha curve — logarithmic,
        // not linear, so the jump from resting to raised is more perceptible
        // than raised to overlay.
        val overlayAlpha = ((4.5f * ln(elevation.value + 1f) + 2f) / 100f).coerceIn(0f, 0.16f)
        return lerp(baseSurface, Color.White, overlayAlpha)
    }
}
