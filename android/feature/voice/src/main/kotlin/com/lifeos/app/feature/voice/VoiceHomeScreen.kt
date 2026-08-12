package com.lifeos.app.feature.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lifeos.app.core.designsystem.LifeOSColors
import com.lifeos.app.core.designsystem.LifeOSShapes
import com.lifeos.app.feature.contacts.ContactActions
import com.lifeos.app.feature.medicines.MedicinesRoute
import java.time.LocalTime

/**
 * Juno's presence-first home — see docs/adr/0001 and the approved
 * 2026-08-12 redesign spec. No button grid, no feature dashboard: a
 * time-of-day greeting (using the elder's own stored name, not a
 * placeholder), the Presence itself, and — only when something is
 * actually true and useful today — one contextual line. On a day with
 * nothing due, that line is absent entirely; presence alone is the
 * default, not the exception.
 */
@Composable
fun VoiceHomeScreen(navController: NavHostController, viewModel: VoiceViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val micPermission = rememberMicrophonePermissionState()
    val presence by viewModel.presence.collectAsStateWithLifecycle()
    val heard by viewModel.heard.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()
    val overlayVisible by viewModel.overlayVisible.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val greetingName by viewModel.greetingName.collectAsStateWithLifecycle()
    val nextDueToday by viewModel.nextDueToday.collectAsStateWithLifecycle()

    val handsFreeOn = remember { VoiceSettingsPrefs.isAlwaysListeningEnabled(context) }
    val greeting = remember(greetingName) { timeOfDayGreeting(LocalTime.now().hour, greetingName) }

    LaunchedEffect(Unit) {
        viewModel.say(
            if (handsFreeOn) "Hello. I am Juno. Just say my name any time." else "Hello. I am Juno. Tap the circle and talk to me.",
        )
    }

    LaunchedEffect(effect) {
        val current = effect
        when (current) {
            is VoiceUiEffect.PlaceCall -> ContactActions.dial(context, current.contact)
            is VoiceUiEffect.OpenWhatsApp -> ContactActions.openWhatsApp(context, current.contact)
            VoiceUiEffect.NavigateToMedicines -> navController.navigate(MedicinesRoute.route)
            VoiceUiEffect.NavigateToSettings -> navController.navigate(SettingsRoute.route)
            VoiceUiEffect.NavigateToMemory -> navController.navigate(MemoryRoute.route)
            null -> Unit
        }
        if (current != null) viewModel.consumeEffect()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(LifeOSColors.Ember.copy(alpha = 0.08f), Color.Transparent),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(40.dp))

            JunoPresence(
                state = presence,
                onClick = {
                    if (micPermission.granted) viewModel.startListening() else micPermission.request()
                },
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "I'm here.",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = when {
                    !micPermission.granted -> "Tap the circle, then allow the microphone"
                    presence == PresenceState.LISTENING -> "Listening…"
                    handsFreeOn -> "Just say \"Juno\" — or tap the circle"
                    else -> "Tap the circle and talk to me"
                },
                style = MaterialTheme.typography.titleMedium,
            )

            if (nextDueToday != null) {
                Spacer(modifier = Modifier.height(28.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), LifeOSShapes.large)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = nextDueToday.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (heard.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = heard,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    ConversationOverlay(
        visible = overlayVisible,
        presence = presence,
        conversation = conversation,
        onOrbClick = {
            if (micPermission.granted) viewModel.startListening() else micPermission.request()
        },
        onDismiss = { viewModel.dismissOverlay() },
    )
}
