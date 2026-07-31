package com.lifeos.app.feature.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.ui.DecisionExplanationSheet
import com.lifeos.app.core.ui.LifeOSCard

@Composable
fun SettingsScreen(
    aiSettingsViewModel: AiSettingsViewModel = hiltViewModel(),
    decisionExplanationViewModel: DecisionExplanationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var alwaysListening by remember { mutableStateOf(VoiceSettingsPrefs.isAlwaysListeningEnabled(context)) }
    val micPermission = rememberMicrophonePermissionState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        LifeOSCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.padding(end = 16.dp)) {
                    Text(text = "Always listen for \"LifeOS\"", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "LifeOS listens in the background so you can just say its name, " +
                            "even with the screen off. Shows a notification the whole time this is on.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = alwaysListening,
                    onCheckedChange = { enabled ->
                        if (enabled && !micPermission.granted) {
                            micPermission.request()
                        } else {
                            alwaysListening = enabled
                            VoiceSettingsPrefs.setAlwaysListeningEnabled(context, enabled)
                        }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AiAssistantSetupCard(aiSettingsViewModel)

        Spacer(modifier = Modifier.height(16.dp))

        DecisionExplanationCard(decisionExplanationViewModel)
    }
}

@Composable
private fun DecisionExplanationCard(viewModel: DecisionExplanationViewModel) {
    val explanation by viewModel.explanation.collectAsStateWithLifecycle()
    val hasRecentDecision by viewModel.hasRecentDecision.collectAsStateWithLifecycle()

    LifeOSCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Why did LifeOS do that?", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "See the reasoning behind LifeOS's most recent decision.",
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
                    text = "Nothing to show yet — talk to LifeOS first.",
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

@Composable
private fun AiAssistantSetupCard(viewModel: AiSettingsViewModel) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()

    LifeOSCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "AI Assistant Setup", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add a free Groq key (console.groq.com) to let LifeOS understand any " +
                    "language freely and answer questions — optional, everything else works without it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = viewModel::onApiKeyChanged,
                label = { Text("Groq API key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = viewModel::testConnection,
                enabled = apiKey.isNotBlank() && testState !is ConnectionTestState.Testing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when (testState) {
                        ConnectionTestState.Testing -> "Testing…"
                        else -> "Test Connection"
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            when (val state = testState) {
                ConnectionTestState.Succeeded ->
                    Text(
                        text = "Connected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                is ConnectionTestState.Failed ->
                    Text(
                        text = "Couldn't connect: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                else -> Unit
            }
        }
    }
}
