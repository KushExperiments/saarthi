package com.lifeos.app.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LifeOSColors.EmberDeep,
    onPrimary = LifeOSColors.LightSurface,
    background = LifeOSColors.LightBackground,
    surface = LifeOSColors.LightSurface,
    onBackground = LifeOSColors.LightInk,
    onSurface = LifeOSColors.LightInk,
    onSurfaceVariant = LifeOSColors.LightMuted,
    error = LifeOSColors.Danger,
)

private val DarkColors = darkColorScheme(
    primary = LifeOSColors.Ember,
    onPrimary = LifeOSColors.DarkBackground,
    background = LifeOSColors.DarkBackground,
    surface = LifeOSColors.DarkSurface,
    onBackground = LifeOSColors.DarkInk,
    onSurface = LifeOSColors.DarkInk,
    onSurfaceVariant = LifeOSColors.DarkMuted,
    error = LifeOSColors.Danger,
)

/**
 * Juno's single design-system entry point. Every screen in every feature
 * module wraps its content in this — there is no second theme.
 *
 * Defaults to dark rather than following the system setting — matches the
 * web app's identity and the approved 2026-08-12 redesign (warm umber
 * near-black, not the earlier neutral/blue-black — see docs/adr/0001).
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
