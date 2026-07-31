package com.lifeos.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.designsystem.LifeOSElevation

/** The shared card surface — generous rounding and padding, warm not clinical. */
@Composable
fun LifeOSCard(
    modifier: Modifier = Modifier,
    elevation: Dp = LifeOSElevation.Resting,
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val containerColor = LifeOSElevation.surfaceTint(darkTheme, elevation, MaterialTheme.colorScheme.surface)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}
