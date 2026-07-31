package com.lifeos.app.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing

/**
 * LifeOS's shared motion language — generalized from what [VoiceOrb] already
 * proved out (slow continuous rotation, a gentle breathing pulse) so other
 * screens can reuse "calm" timing instead of Material's snappier defaults.
 * The whole point of this app's motion is to lower anxiety, not signal
 * responsiveness the way a typical productivity app's motion does.
 */
object LifeOSMotion {
    /** A held sphere breathing at rest — slow, barely-there. */
    const val BREATHE_IDLE_MS = 2400
    const val BREATHE_ACTIVE_MS = 700

    /** The orb's slow ambient spin — a sign of life, not a loading spinner. */
    const val SPIN_IDLE_MS = 11000
    const val SPIN_ACTIVE_MS = 4000

    /** Sound-wave ripple cadence while actively listening. */
    const val RIPPLE_MS = 1600

    /** Everyday UI transitions (a card appearing, a screen settling) — calmer than Material's ~150-300ms defaults. */
    const val TRANSITION_MS = 400
    const val TRANSITION_EMPHASIZED_MS = 600

    val CalmEasing: Easing = FastOutSlowInEasing
    val LinearSpin: Easing = LinearEasing

    /** A softer ease-out than Material's default — settles rather than snaps into place. */
    val SettleEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}
