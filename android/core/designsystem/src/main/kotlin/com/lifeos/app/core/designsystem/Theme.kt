package com.lifeos.app.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LifeOSColors.BrandGreen,
    onPrimary = LifeOSColors.LightSurface,
    background = LifeOSColors.LightBackground,
    surface = LifeOSColors.LightSurface,
    onBackground = LifeOSColors.LightInk,
    onSurface = LifeOSColors.LightInk,
    onSurfaceVariant = LifeOSColors.LightMuted,
    error = LifeOSColors.Danger,
)

private val DarkColors = darkColorScheme(
    primary = LifeOSColors.BrandGreenLight,
    onPrimary = LifeOSColors.DarkBackground,
    background = LifeOSColors.DarkBackground,
    surface = LifeOSColors.DarkSurface,
    onBackground = LifeOSColors.DarkInk,
    onSurface = LifeOSColors.DarkInk,
    onSurfaceVariant = LifeOSColors.DarkMuted,
    error = LifeOSColors.Danger,
)

/**
 * LifeOS's single design-system entry point. Every screen in every
 * feature module wraps its content in this — there is no second theme.
 *
 * Defaults to dark rather than following the system setting — matches the
 * web app's identity (dark near-black, Gemini-style, decided explicitly
 * after feedback that a light default didn't match), and this was one of
 * the concrete "doesn't match what I expected" gaps between the two apps.
 */
@Composable
fun LifeOSTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = LifeOSTypography,
        shapes = LifeOSShapes,
        content = content,
    )
}
