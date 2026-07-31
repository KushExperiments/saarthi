package com.lifeos.app.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.lifeos.app.core.designsystem.MinTouchTarget

/**
 * An icon-only action that meets [MinTouchTarget] — plain IconButton
 * defaults to 40dp, too small for this app's bar. Uses a large emoji/glyph
 * as the icon, matching this app's established convention (no Material
 * icon-library dependency; big familiar glyphs read without relying on
 * icon-meaning literacy).
 */
@Composable
fun LifeOSIconButton(
    glyph: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(MinTouchTarget).semantics { this.contentDescription = contentDescription },
    ) {
        Text(text = glyph, style = MaterialTheme.typography.titleLarge)
    }
}
