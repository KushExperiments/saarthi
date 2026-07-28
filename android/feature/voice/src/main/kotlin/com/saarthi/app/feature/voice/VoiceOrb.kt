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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.saarthi.app.core.designsystem.SaarthiColors

/**
 * The ambient, glowing presence at the center of Saarthi's voice screen —
 * this IS the app, not a button that happens to sit on a screen. A slow
 * breathing pulse at rest reads as "alive but calm"; a brighter, faster
 * pulse with rippling sound-wave rings while listening reads as "hearing
 * you." A glossy highlight + rim-shading layer gives the sphere real
 * depth (a flat single-gradient circle read as flat, not glass-like).
 */
@Composable
fun VoiceOrb(
    listening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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

    val glowBrush = remember(listening) {
        Brush.radialGradient(
            colors = if (listening) {
                listOf(
                    SaarthiColors.VoiceAccentLight.copy(alpha = 0.45f),
                    SaarthiColors.VoiceAccent.copy(alpha = 0.15f),
                    Color.Transparent,
                )
            } else {
                listOf(
                    SaarthiColors.VoiceAccentLight.copy(alpha = 0.30f),
                    SaarthiColors.VoiceAccent.copy(alpha = 0.08f),
                    Color.Transparent,
                )
            },
        )
    }

    val coreBrush = remember {
        Brush.radialGradient(
            colors = listOf(SaarthiColors.VoiceAccentLight, SaarthiColors.VoiceAccent, SaarthiColors.VoiceAccentDark),
        )
    }

    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        if (listening) {
            Canvas(modifier = Modifier.size(220.dp)) {
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
                .size(190.dp)
                .scale(breathe)
                .background(brush = glowBrush, shape = CircleShape),
        )

        val coreSize = 148.dp
        Box(
            modifier = Modifier
                .size(coreSize)
                .scale(breathe)
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
                val highlightCenter = Offset(size.width * 0.36f, size.height * 0.32f)
                val highlightRadius = size.minDimension * 0.42f

                // Base sphere.
                drawCircle(brush = coreBrush, radius = sphereRadius, center = sphereCenter)

                // Glossy specular highlight, offset toward the upper-left light
                // source — large and bright, matching the reference's glass-like
                // sphere rather than a small pinpoint reflection.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.25f), Color.Transparent),
                        center = highlightCenter,
                        radius = highlightRadius,
                    ),
                    radius = highlightRadius,
                    center = highlightCenter,
                )

                // Rim-shading vignette so the edge reads as curved glass, not a flat disc.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, SaarthiColors.VoiceAccentDark.copy(alpha = 0.4f)),
                        center = sphereCenter,
                        radius = sphereRadius,
                    ),
                    radius = sphereRadius,
                    center = sphereCenter,
                )
            }
        }
    }
}
