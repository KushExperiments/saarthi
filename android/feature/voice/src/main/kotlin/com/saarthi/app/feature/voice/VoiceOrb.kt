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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saarthi.app.core.designsystem.SaarthiColors

/**
 * The ambient, glowing presence at the center of Saarthi's voice screen —
 * this IS the app, not a button that happens to sit on a screen. A slow
 * breathing pulse at rest reads as "alive but calm"; a brighter, faster
 * pulse with rippling sound waves while listening reads as "hearing you"
 * — the same visual language real assistant apps use, sized and paced
 * for someone who wants calm, not flashy.
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
                    SaarthiColors.BrandGreenLight.copy(alpha = 0.45f),
                    SaarthiColors.BrandGreen.copy(alpha = 0.15f),
                    Color.Transparent,
                )
            } else {
                listOf(
                    SaarthiColors.BrandGreenLight.copy(alpha = 0.30f),
                    SaarthiColors.BrandGreen.copy(alpha = 0.08f),
                    Color.Transparent,
                )
            },
        )
    }

    val coreBrush = remember {
        Brush.radialGradient(
            colors = listOf(SaarthiColors.BrandGreenLight, SaarthiColors.BrandGreen, SaarthiColors.BrandGreenDark),
        )
    }

    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        if (listening) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val maxRadius = size.minDimension / 2f
                repeat(2) { index ->
                    val phase = (rippleProgress + index * 0.5f) % 1f
                    drawCircle(
                        color = SaarthiColors.BrandGreenLight.copy(alpha = (1f - phase) * 0.4f),
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

        Box(
            modifier = Modifier
                .size(148.dp)
                .scale(breathe)
                .clip(CircleShape)
                .background(brush = coreBrush, shape = CircleShape)
                .clickable(
                    onClickLabel = if (listening) "Stop listening" else "Tap and talk to Saarthi",
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = if (listening) "⏹" else "🎤", fontSize = 56.sp)
        }
    }
}
