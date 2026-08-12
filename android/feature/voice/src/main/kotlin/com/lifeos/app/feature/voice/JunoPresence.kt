package com.lifeos.app.feature.voice

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.LifeOSColors
import com.lifeos.app.core.designsystem.LifeOSMotion
import kotlin.math.cos
import kotlin.math.sin

private data class PresencePalette(val pale: Color, val mid: Color, val deep: Color)

private data class PresenceMotion(
    val breatheMs: Int,
    val breatheAmp: Float,
    val wobbleAmp: Float,
    val amplitudeDriven: Boolean,
    val illumination: Float,
    val innerGlow: Boolean,
    val innerGlowMs: Int,
    val flashOnEnter: Boolean,
    val flashMs: Int,
    val scale: Float,
)

private fun paletteFor(state: PresenceState): PresencePalette = when (state) {
    PresenceState.THINKING, PresenceState.PROCESSING_ACTION ->
        PresencePalette(LifeOSColors.VerdigrisPale, LifeOSColors.Verdigris, LifeOSColors.VerdigrisDeep)
    PresenceState.SUCCESS ->
        PresencePalette(LifeOSColors.VerdigrisPale, LifeOSColors.Success, LifeOSColors.Success)
    PresenceState.ERROR ->
        PresencePalette(LifeOSColors.EmberPale, LifeOSColors.Warn, LifeOSColors.Warn)
    PresenceState.OFFLINE ->
        PresencePalette(LifeOSColors.FaintPale, LifeOSColors.Faint, LifeOSColors.FaintDeep)
    PresenceState.EMERGENCY ->
        PresencePalette(LifeOSColors.EmberPale, LifeOSColors.Emergency, LifeOSColors.Emergency)
    PresenceState.IDLE, PresenceState.LISTENING, PresenceState.WAKE_WORD, PresenceState.SPEAKING ->
        PresencePalette(LifeOSColors.EmberPale, LifeOSColors.Ember, LifeOSColors.EmberDeep)
}

private fun motionFor(state: PresenceState): PresenceMotion = when (state) {
    PresenceState.IDLE ->
        PresenceMotion(LifeOSMotion.BREATHE_IDLE_MS, 0.02f, 0f, false, 1f, false, 0, false, 0, 1f)
    PresenceState.LISTENING ->
        PresenceMotion(LifeOSMotion.BREATHE_LISTENING_MS, 0.015f, 0.03f, true, 1f, false, 0, false, 0, 1.03f)
    PresenceState.WAKE_WORD ->
        PresenceMotion(600, 0.006f, 0f, false, 1f, false, 0, true, LifeOSMotion.FLASH_WAKE_MS, 1.05f)
    PresenceState.THINKING ->
        PresenceMotion(LifeOSMotion.BREATHE_THINKING_MS, 0.010f, 0f, false, 1f, true, LifeOSMotion.INNER_GLOW_THINKING_MS, false, 0, 0.97f)
    PresenceState.SPEAKING ->
        PresenceMotion(LifeOSMotion.BREATHE_SPEAKING_MS, 0.018f, 0.035f, true, 1f, false, 0, false, 0, 1.0f)
    PresenceState.PROCESSING_ACTION ->
        PresenceMotion(LifeOSMotion.BREATHE_PROCESSING_MS, 0.006f, 0f, false, 1f, true, LifeOSMotion.INNER_GLOW_PROCESSING_MS, false, 0, 0.9f)
    PresenceState.SUCCESS ->
        PresenceMotion(1, 0f, 0f, false, 1f, false, 0, true, LifeOSMotion.FLASH_SUCCESS_MS, 1.12f)
    PresenceState.ERROR ->
        PresenceMotion(LifeOSMotion.BREATHE_ERROR_MS, 0.008f, 0f, false, 0.85f, false, 0, false, 0, 0.85f)
    PresenceState.OFFLINE ->
        PresenceMotion(1, 0f, 0f, false, 0.4f, false, 0, false, 0, 0.9f)
    PresenceState.EMERGENCY ->
        PresenceMotion(LifeOSMotion.BREATHE_EMERGENCY_MS, 0.018f, 0f, false, 1f, false, 0, false, 0, 1.06f)
}

private fun presenceContentDescription(state: PresenceState): String = when (state) {
    PresenceState.IDLE -> "Juno is here, waiting"
    PresenceState.LISTENING -> "Juno is listening"
    PresenceState.WAKE_WORD -> "Juno heard you"
    PresenceState.THINKING -> "Juno is thinking"
    PresenceState.SPEAKING -> "Juno is speaking"
    PresenceState.PROCESSING_ACTION -> "Juno is doing that now"
    PresenceState.SUCCESS -> "Done"
    PresenceState.ERROR -> "Juno didn't understand"
    PresenceState.OFFLINE -> "Juno is offline"
    PresenceState.EMERGENCY -> "Emergency mode"
}

/**
 * Juno's living presence — the whole point of the voice-first design, not
 * a decorative ball. Lit by one fixed virtual light (upper-left-front)
 * that never moves, so the highlight and shaded terminator stay put
 * regardless of state or motion — see docs/adr/0001 for why the previous
 * version's rotating gradient read as a flat 2D layer instead of a lit
 * sphere, and why this one deliberately never spins. Life comes from
 * breathing (scale), a small amplitude-driven pulse while
 * Listening/Speaking, and — Thinking/Processing only — a soft glow
 * drifting inside the volume, independent of the fixed surface highlight.
 *
 * Renders with `drawCircle`/`Brush.radialGradient` only (diffuse body,
 * specular hotspot, rim light, ambient glow, soft shadow, all at the fixed
 * light position) rather than a baked per-pixel bitmap — an intentionally
 * lower-risk technique for this first Kotlin pass, since no local
 * build/emulator exists here to verify a `Bitmap`/`BlendMode`-based
 * implementation before it ships. Same physical-correctness property
 * either way: the highlight never moves with rotation, because nothing
 * ever rotates it.
 */
@Composable
fun JunoPresence(
    state: PresenceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 220.dp,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    var nowNanos by remember { mutableStateOf(System.nanoTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameNanos -> nowNanos = frameNanos }
        }
    }
    val enteredAtNanos = remember(state) { nowNanos }

    val palette = remember(state) { paletteFor(state) }
    val motion = remember(state) { motionFor(state) }
    val describeState = remember(state) { presenceContentDescription(state) }

    val elapsedMs = nowNanos / 1_000_000f
    val sinceEnterMs = (nowNanos - enteredAtNanos) / 1_000_000f

    val breathe = if (motion.breatheAmp > 0f && !reduceMotion) {
        1f + sin(elapsedMs / motion.breatheMs * (2f * Math.PI.toFloat())) * motion.breatheAmp
    } else {
        1f
    }

    val flashProgress = if (motion.flashOnEnter) {
        1f - (sinceEnterMs / motion.flashMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    val pulse = if (motion.amplitudeDriven && !reduceMotion) {
        val envelope = 0.5f + 0.3f * sin(elapsedMs / 280f) + 0.2f * sin(elapsedMs / 97f)
        1f + motion.wobbleAmp * envelope
    } else {
        1f
    }

    val scale = motion.scale * breathe * pulse * (1f + flashProgress * 0.05f)

    Box(modifier = modifier.size(orbSize), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(orbSize)
                .semantics { contentDescription = describeState }
                .clip(CircleShape)
                .clickable(
                    onClickLabel = if (state == PresenceState.LISTENING) "Stop listening" else "Tap and talk to Juno",
                    role = Role.Button,
                    onClick = onClick,
                ),
        ) {
            val radius = size.minDimension / 2f * scale
            val sphereCenter = Offset(size.width / 2f, size.height / 2f)

            // Fixed virtual light — upper-left-front. This angle is a
            // constant, never touched by scale/state/animation, which is
            // what keeps the highlight and terminator physically coherent.
            val lightAngle = Math.toRadians(-125.0)
            val lightDirection = Offset(
                x = cos(lightAngle).toFloat(),
                y = sin(lightAngle).toFloat(),
            )
            val highlightCenter = sphereCenter + lightDirection * (radius * 0.34f)

            // Soft shadow — implies the sphere floats above a surface lit
            // from the same upper-left source (shadow falls opposite it).
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.18f * motion.illumination), Color.Transparent),
                    center = sphereCenter - lightDirection * (radius * 0.22f),
                    radius = radius * 1.15f,
                ),
                radius = radius * 1.15f,
                center = sphereCenter - lightDirection * (radius * 0.22f),
            )

            // Ambient outer glow.
            val glowRadius = radius * (1.65f + flashProgress * 0.6f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.mid.copy(alpha = (0.30f * motion.illumination + flashProgress * 0.30f).coerceIn(0f, 1f)),
                        Color.Transparent,
                    ),
                    center = sphereCenter,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = sphereCenter,
            )

            // Diffuse sphere body — one radial gradient whose center is the
            // fixed highlight position. This is the "depth gradient" cue:
            // bright near the light, dark toward the far side.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.pale, palette.mid, palette.deep),
                    center = highlightCenter,
                    radius = radius * 1.35f,
                ),
                radius = radius,
                center = sphereCenter,
                alpha = motion.illumination,
            )

            // Internal volumetric light drift — Thinking/Processing only.
            // Independent of the fixed surface highlight: this is light
            // moving *inside* the volume, never a rotation of the sphere.
            if (motion.innerGlow && !reduceMotion) {
                val orbitAngle = elapsedMs / motion.innerGlowMs * (2f * Math.PI.toFloat())
                val innerCenter = sphereCenter + Offset(
                    x = cos(orbitAngle) * radius * 0.30f,
                    y = sin(orbitAngle * 0.72f) * radius * 0.18f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.pale.copy(alpha = 0.35f), Color.Transparent),
                        center = innerCenter,
                        radius = radius * 0.55f,
                    ),
                    radius = radius * 0.55f,
                    center = innerCenter,
                )
            }

            // Fresnel-style rim light — a soft stroke near the silhouette
            // edge, fixed like everything else above.
            drawCircle(
                color = palette.pale,
                radius = radius * 0.94f,
                center = sphereCenter,
                alpha = 0.30f * motion.illumination,
                style = Stroke(width = radius * 0.10f),
            )

            // Specular hotspot — small, bright, fixed at the light
            // position. Never moves with rotation, because nothing here
            // ever rotates.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (0.85f + flashProgress * 0.15f) * motion.illumination),
                        Color.White.copy(alpha = 0f),
                    ),
                    center = highlightCenter,
                    radius = radius * 0.30f,
                ),
                radius = radius * 0.30f,
                center = highlightCenter,
            )
        }
    }
}
