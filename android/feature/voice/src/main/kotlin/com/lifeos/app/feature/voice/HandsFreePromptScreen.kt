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
 * A one-time, full-screen decision instead of a small switch buried in a
 * Settings screen nobody would find without being told to look — "Always
 * listen" is a big enough capability (a persistent background microphone)
 * that it deserves a deliberate yes/no moment, not a row in a list. Shown
 * once, right after onboarding; the elder never has to go dig through a
 * menu to make this choice — Juno asks once, up front.
 */
@Composable
fun HandsFreePromptScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val micPermission = rememberMicrophonePermissionState()
    val notificationPermission = rememberNotificationPermissionState()
    val batteryOptimization = rememberBatteryOptimizationState()
    var pendingEnable by remember { mutableStateOf(false) }

    // Same permission chain SettingsScreen used to run — mic, then
    // notifications, then a best-effort battery-optimization exemption
    // (declining that one doesn't block anything, it just means the
    // service is more exposed to being killed by aggressive OEM battery
    // managers), then actually start the service.
    LaunchedEffect(pendingEnable, micPermission.granted, notificationPermission.granted) {
        if (!pendingEnable) return@LaunchedEffect
        when {
            !micPermission.granted -> micPermission.request()
            !notificationPermission.granted -> notificationPermission.request()
            else -> {
                pendingEnable = false
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
            text = "Listen for \"Juno\"?",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Juno can listen in the background, even with the screen off, so you " +
                "can just say \"Juno\" any time you need help — no tapping needed. " +
                "Shows a notification the whole time this is on, and you can turn it " +
                "off any time by saying \"open settings\".",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(40.dp))
        LifeOSButton(
            text = "Yes, listen for me",
            onClick = { pendingEnable = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                VoiceSettingsPrefs.setAlwaysListeningEnabled(context, false)
                HandsFreePromptPrefs.markSeen(context)
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("No thanks, I'll tap to talk")
        }
    }
}
