package com.saarthi.app.feature.voice

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saarthi.app.core.designsystem.SaarthiColors

/**
 * The ambient, glowing presence at the center of Saarthi's voice screen —
 * this IS the app, not a button that happens to sit on a screen. A slow
 * continuous spin plus a slow breathing pulse at rest reads as "alive but
 * calm"; a faster spin and pulse with rippling sound-wave rings while
 * listening reads as "hearing you."
 */
@Composable
fun VoiceOrb(
    listening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 220.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceOrb")

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (listening) 700 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ripple",
    )

    // Slow continuous spin — the off-center gradient below means rotating
    // the sphere visibly sweeps its "light source" around the surface,
    // rather than a static pulse being the only sign of life.
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (listening) 4000 else 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )

    val glowBrush = remember(listening) {
        Brush.radialGradient(
            colors = listOf(
                SaarthiColors.VoiceAccentLight.copy(alpha = if (listening) 0.38f else 0.22f),
                Color.Transparent,
            ),
        )
    }

    Box(modifier = modifier.size(orbSize), contentAlignment = Alignment.Center) {
        if (listening) {
            Canvas(modifier = Modifier.size(orbSize)) {
                val maxRadius = size.minDimension / 2f
                repeat(2) { index ->
                    val phase = (rippleProgress + index * 0.5f) % 1f
                    drawCircle(
                        color = SaarthiColors.VoiceAccentLight.copy(alpha = (1f - phase) * 0.4f),
                        radius = maxRadius * (0.5f + phase * 0.5f),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(orbSize * 0.864f)
                .scale(breathe)
                .background(brush = glowBrush, shape = CircleShape),
        )

        val coreSize = orbSize * 0.673f
        Box(
            modifier = Modifier
                .size(coreSize)
                .scale(breathe)
                .rotate(rotation)
                .clip(CircleShape)
                .clickable(
                    onClickLabel = if (listening) "Stop listening" else "Tap and talk to Saarthi",
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(coreSize)) {
                val sphereRadius = size.minDimension / 2f
                val sphereCenter = Offset(size.width / 2f, size.height / 2f)

                // One soft, off-center radial gradient — the palest stop
                // sits where a light source would be, which alone reads as
                // a highlight without a separate overlaid blob. Rotating
                // this whole circle (see `rotation` above) sweeps that
                // highlight slowly around the sphere.
                val coreBrush = Brush.radialGradient(
                    colors = listOf(
                        SaarthiColors.VoiceAccentPale,
                        SaarthiColors.VoiceAccentLight,
                        SaarthiColors.VoiceAccent,
                        SaarthiColors.VoiceAccentDeep,
                        SaarthiColors.VoiceAccentDark,
                    ),
                    center = Offset(size.width * 0.32f, size.height * 0.26f),
                    radius = size.minDimension * 0.95f,
                )
                drawCircle(brush = coreBrush, radius = sphereRadius, center = sphereCenter)
            }
        }
    }
}
