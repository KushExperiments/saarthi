package com.lifeos.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.LifeOSColors

enum class LifeOSBadgeTone { NEUTRAL, WARNING, DANGER }

/** A small status pill — e.g. "overdue" on a reminder — consistent instead of ad hoc per screen. */
@Composable
fun LifeOSBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: LifeOSBadgeTone = LifeOSBadgeTone.NEUTRAL,
) {
    val (background, foreground) = when (tone) {
        LifeOSBadgeTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        LifeOSBadgeTone.WARNING -> LifeOSColors.Warn.copy(alpha = 0.18f) to LifeOSColors.Warn
        LifeOSBadgeTone.DANGER -> LifeOSColors.Danger.copy(alpha = 0.18f) to LifeOSColors.Danger
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        modifier = modifier
            .background(background, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
