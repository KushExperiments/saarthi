package com.lifeos.app.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeOSElevationTest {

    private val darkSurface = Color(0f, 0f, 0f)

    @Test
    fun `light theme never tints the surface, regardless of elevation`() {
        val tint = LifeOSElevation.surfaceTint(darkTheme = false, elevation = LifeOSElevation.Overlay, baseSurface = darkSurface)

        assertEquals(darkSurface, tint)
    }

    @Test
    fun `dark theme at zero elevation leaves the surface untouched`() {
        val tint = LifeOSElevation.surfaceTint(darkTheme = true, elevation = 0.dp, baseSurface = darkSurface)

        assertEquals(darkSurface, tint)
    }

    @Test
    fun `dark theme at a real elevation lightens the surface toward white`() {
        val tint = LifeOSElevation.surfaceTint(darkTheme = true, elevation = LifeOSElevation.Raised, baseSurface = darkSurface)

        assertTrue("expected the tint to be lighter than pure black", tint.red > darkSurface.red)
    }

    @Test
    fun `higher elevation lightens the surface more than lower elevation, not less`() {
        val resting = LifeOSElevation.surfaceTint(darkTheme = true, elevation = LifeOSElevation.Resting, baseSurface = darkSurface)
        val overlay = LifeOSElevation.surfaceTint(darkTheme = true, elevation = LifeOSElevation.Overlay, baseSurface = darkSurface)

        assertTrue("overlay elevation should read lighter than resting elevation", overlay.red > resting.red)
    }
}
