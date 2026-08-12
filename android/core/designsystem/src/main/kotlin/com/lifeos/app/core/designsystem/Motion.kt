package com.lifeos.app.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing

/**
 * Juno's shared motion language. The whole point is to lower anxiety, not
 * signal responsiveness the way a typical productivity app's motion does —
 * see docs/adr/0001 for why the Presence itself no longer rotates: a
 * physically-lit sphere under a fixed light looks identical at every
 * rotation angle, so "spin" was never actually communicating anything.
 * Motion instead comes from breathing (scale), a small amplitude-driven
 * pulse on Listening/Speaking, and — Thinking/Processing only — light
 * drifting inside the volume.
 */
object LifeOSMotion {
    /** One full breathe cycle, per Presence state — see JunoPresence. */
    const val BREATHE_IDLE_MS = 2600
    const val BREATHE_LISTENING_MS = 900
    const val BREATHE_SPEAKING_MS = 340
    const val BREATHE_THINKING_MS = 2200
    const val BREATHE_PROCESSING_MS = 2000
    const val BREATHE_ERROR_MS = 1800
    const val BREATHE_EMERGENCY_MS = 1200

    /** One-shot illumination pulses — not loops. */
    const val FLASH_WAKE_MS = 450
    const val FLASH_SUCCESS_MS = 500

    /** Internal volumetric light drift while Thinking/Processing — never a surface rotation. */
    const val INNER_GLOW_THINKING_MS = 5200
    const val INNER_GLOW_PROCESSING_MS = 2400

    /** Everyday UI transitions (a card appearing, a screen settling) — calmer than Material's ~150-300ms defaults. */
    const val TRANSITION_MS = 400
    const val TRANSITION_EMPHASIZED_MS = 600

    val CalmEasing: Easing = FastOutSlowInEasing
    val LinearSpin: Easing = LinearEasing

    /** A softer ease-out than Material's default — settles rather than snaps into place. */
    val SettleEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}
