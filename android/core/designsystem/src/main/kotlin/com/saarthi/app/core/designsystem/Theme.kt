package com.saarthi.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SaarthiColors.BrandGreen,
    onPrimary = SaarthiColors.LightSurface,
    background = SaarthiColors.LightBackground,
    surface = SaarthiColors.LightSurface,
    onBackground = SaarthiColors.LightInk,
    onSurface = SaarthiColors.LightInk,
    onSurfaceVariant = SaarthiColors.LightMuted,
    error = SaarthiColors.Danger,
)

private val DarkColors = darkColorScheme(
    primary = SaarthiColors.BrandGreenLight,
    onPrimary = SaarthiColors.DarkBackground,
    background = SaarthiColors.DarkBackground,
    surface = SaarthiColors.DarkSurface,
    onBackground = SaarthiColors.DarkInk,
    onSurface = SaarthiColors.DarkInk,
    onSurfaceVariant = SaarthiColors.DarkMuted,
    error = SaarthiColors.Danger,
)

/**
 * Saarthi's single design-system entry point. Every screen in every
 * feature module wraps its content in this — there is no second theme.
 */
@Composable
fun SaarthiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = SaarthiTypography,
        shapes = SaarthiShapes,
        content = content,
    )
}
