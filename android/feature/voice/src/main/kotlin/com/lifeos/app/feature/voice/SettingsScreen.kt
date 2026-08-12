package com.lifeos.app.feature.voice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lifeos.app.core.ui.DecisionExplanationSheet
import com.lifeos.app.core.ui.LifeOSCard

/**
 * Reachable by voice ("open settings"), not by a button on the home
 * screen — the elder shouldn't have to go hunting through a menu for
 * anything Juno can just do. "Always listen" moved out of here entirely,
 * into a one-time big-screen prompt right after onboarding
 * (HandsFreePromptScreen) instead of a small switch nobody would find
 * without being told to look for it.
 */
@Composable
fun SettingsScreen(navController: NavHostController, decisionExplanationViewModel: DecisionExplanationViewModel = hiltViewModel()) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        MemoryCard(onOpen = { navController.navigate(MemoryRoute.route) })
        Spacer(modifier = Modifier.height(16.dp))
        DecisionExplanationCard(decisionExplanationViewModel)
    }
}

@Composable
private fun MemoryCard(onOpen: () -> Unit) {
    LifeOSCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "What I Remember", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "See, correct, or forget anything Juno remembers about you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Text("Open")
            }
        }
    }
}

@Composable
private fun DecisionExplanationCard(viewModel: DecisionExplanationViewModel) {
    val explanation by viewModel.explanation.collectAsStateWithLifecycle()
    val hasRecentDecision by viewModel.hasRecentDecision.collectAsStateWithLifecycle()

    LifeOSCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Why did Juno do that?", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "See the reasoning behind Juno's most recent decision.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = viewModel::show, modifier = Modifier.fillMaxWidth()) {
                Text("Show last decision")
            }
            if (!hasRecentDecision) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nothing to show yet — talk to Juno first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    val currentExplanation = explanation
    if (currentExplanation != null) {
        DecisionExplanationSheet(explanation = currentExplanation, onDismiss = viewModel::dismiss)
    }
}
