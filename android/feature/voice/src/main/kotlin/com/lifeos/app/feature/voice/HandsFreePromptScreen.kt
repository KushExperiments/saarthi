package com.lifeos.app.feature.voice

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.lifeos.app.core.ui.LifeOSButton

private const val PREFS_NAME = "hands_free_prompt_prefs"
private const val KEY_SEEN = "has_seen_hands_free_prompt"

/** Whether the one-time big-screen hands-free prompt has already run. */
object HandsFreePromptPrefs {
    fun hasSeenPrompt(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false)

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putBoolean(KEY_SEEN, true) }
    }
}

/**
 * Real voice recognition, not tap-to-talk, is the point — direct feedback:
 * "I don't want tap to speak, I want voice recognition." This screen no
 * longer asks yes/no; it requests the permissions hands-free listening
 * needs immediately, automatically, the moment it appears — the only taps
 * involved are the OS's own unavoidable permission dialogs, not an
 * in-app choice to opt in or out. If the elder (or whoever's holding the
 * phone) denies microphone access at the OS level, a fallback appears so
 * they're not stuck on this screen forever — recovery, not a first-class
 * "no thanks" option.
 */
@Composable
fun HandsFreePromptScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val micPermission = rememberMicrophonePermissionState()
    val notificationPermission = rememberNotificationPermissionState()
    val batteryOptimization = rememberBatteryOptimizationState()
    var micRequested by remember { mutableStateOf(false) }

    LaunchedEffect(micPermission.granted, notificationPermission.granted) {
        when {
            !micPermission.granted -> {
                micRequested = true
                micPermission.request()
            }
            !notificationPermission.granted -> notificationPermission.request()
            else -> {
                VoiceSettingsPrefs.setAlwaysListeningEnabled(context, true)
                if (!batteryOptimization.ignoring) requestIgnoreBatteryOptimizations(context, batteryOptimization)
                WakeWordService.start(context)
                HandsFreePromptPrefs.markSeen(context)
                onDone()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Setting up voice recognition…",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Juno listens for you, even with the screen off — just say \"Juno\" any " +
                "time. Shows a notification the whole time this is on, and you can turn it " +
                "off any time by saying \"open settings\".",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Only appears if the microphone permission was actually denied at
        // the OS level — a rescue path so a denial doesn't strand the
        // elder on this screen forever, not an invitation to opt out.
        if (micRequested && !micPermission.granted) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Juno needs microphone access to listen for you.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LifeOSButton(
                text = "Try again",
                onClick = { micPermission.request() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    VoiceSettingsPrefs.setAlwaysListeningEnabled(context, false)
                    HandsFreePromptPrefs.markSeen(context)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue without voice recognition for now")
            }
        }
    }
}
