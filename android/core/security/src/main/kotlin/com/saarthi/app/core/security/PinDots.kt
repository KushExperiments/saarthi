package com.saarthi.app.core.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private const val MAX_VISIBLE_DOTS = 6

/** Filled/unfilled dots — never shows the actual digits entered. */
@Composable
fun PinDots(enteredLength: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(MAX_VISIBLE_DOTS) { index ->
            val filled = index < enteredLength
            val color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
