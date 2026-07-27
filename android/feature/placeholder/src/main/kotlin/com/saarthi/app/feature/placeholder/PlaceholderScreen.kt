package com.saarthi.app.feature.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.saarthi.app.core.ui.SaarthiCard

/**
 * The only screen in the M-001 skeleton. Not a feature — proof that
 * DI, Navigation, and the Design System resolve correctly at runtime.
 * Real screens (Medicines, Contacts, Setup) arrive in later modules.
 */
@Composable
fun PlaceholderScreen(viewModel: PlaceholderViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("placeholder_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SaarthiCard {
            Text(
                text = viewModel.message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("placeholder_message"),
            )
        }
    }
}
