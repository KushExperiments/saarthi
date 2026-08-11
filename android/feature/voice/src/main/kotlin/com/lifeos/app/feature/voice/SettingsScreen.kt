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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.ui.DecisionExplanationSheet
import com.lifeos.app.core.ui.LifeOSCard

@Composable
fun SettingsScreen(decisionExplanationViewModel: DecisionExplanationViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var alwaysListening by remember { mutableStateOf(VoiceSettingsPrefs.isAlwaysListeningEnabled(context)) }
    val micPermission = rememberMicrophonePermissionState()
    val notificationPermission = rememberNotificationPermissionState()
    val batteryOptimization = rememberBatteryOptimizationState()
    var pendingEnable by remember { mutableStateOf(false) }

    // Finishes turning hands-free on once every permission it needs is
    // actually granted — this is exactly the previously-known bug ("Always
    // listen" didn't turn on after granting mic permission): tapping the
    // switch only requested one permission and stopped there, nothing
    // watched for the grant to actually complete the enable. This chains
    // through mic -> notifications -> starting the real service, re-running
    // whenever a permission's granted state changes.
    //
    // Battery optimization is requested but NOT gated on — declining it
    // doesn't block hands-free from turning on, it just means the service
    // is more likely to get killed in the background on aggressive OEM
    // skins (Xiaomi/Oppo/Vivo/Samsung), same tradeoff this app's medicine
    // reminders already document in android/README.md.
    LaunchedEffect(pendingEnable, micPermission.granted, notificationPermission.granted) {
        if (!pendingEnable) return@LaunchedEffect
        when {
            !micPermission.granted -> micPermission.request()
            !notificationPermission.granted -> notificationPermission.request()
            else -> {
                pendingEnable = false
                alwaysListening = true
                VoiceSettingsPrefs.setAlwaysListeningEnabled(context, true)
                if (!batteryOptimization.ignoring) requestIgnoreBatteryOptimizations(context, batteryOptimization)
                WakeWordService.start(context)
            }
        }
    }

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
                    Text(text = "Always listen for \"Juno\"", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Juno listens in the background so you can just say its name, " +
                            "even with the screen off. Shows a notification the whole time this is on. " +
                            "On some phones/Android versions, saying the name opens the app directly; " +
                            "on others you may need to tap that notification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = alwaysListening,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            pendingEnable = true
                        } else {
                            pendingEnable = false
                            alwaysListening = false
                            VoiceSettingsPrefs.setAlwaysListeningEnabled(context, false)
                            WakeWordService.stop(context)
                        }
                    },
                )
            }
        }

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
