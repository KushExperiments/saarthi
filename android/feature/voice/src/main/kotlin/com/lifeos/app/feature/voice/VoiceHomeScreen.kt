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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lifeos.app.core.designsystem.LifeOSColors
import com.lifeos.app.core.designsystem.LifeOSShapes
import com.lifeos.app.feature.contacts.ContactActions
import com.lifeos.app.feature.medicines.MedicinesRoute

/**
 * The whole point of the voice-first redesign this project already went
 * through in its web prototype: no button grid, one big microphone. Any
 * other screen (Medicines, Contacts) is reached by voice, not by tapping
 * around a menu. The glowing orb carries the "real assistant" feeling —
 * an ambient presence, not a UI control with an icon glued to it.
 */
@Composable
fun VoiceHomeScreen(navController: NavHostController, viewModel: VoiceViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val micPermission = rememberMicrophonePermissionState()
    val listening by viewModel.listening.collectAsStateWithLifecycle()
    val heard by viewModel.heard.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()
    val overlayVisible by viewModel.overlayVisible.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.say("Hello. I am Juno. Tap the circle and talk to me.")
    }

    LaunchedEffect(effect) {
        val current = effect
        when (current) {
            is VoiceUiEffect.PlaceCall -> ContactActions.dial(context, current.contact)
            is VoiceUiEffect.OpenWhatsApp -> ContactActions.openWhatsApp(context, current.contact)
            VoiceUiEffect.NavigateToMedicines -> navController.navigate(MedicinesRoute.route)
            VoiceUiEffect.NavigateToSettings -> navController.navigate(SettingsRoute.route)
            null -> Unit
        }
        if (current != null) viewModel.consumeEffect()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(LifeOSColors.BrandGreenLight.copy(alpha = 0.10f), Color.Transparent),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Hello 👋", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "I'm here whenever you want to talk.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            VoiceOrb(
                listening = listening,
                onClick = {
                    if (micPermission.granted) viewModel.startListening() else micPermission.request()
                },
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = when {
                    !micPermission.granted -> "Tap the circle, then allow the microphone"
                    listening -> "Listening…"
                    else -> "Tap the circle and talk to me"
                },
                style = MaterialTheme.typography.titleMedium,
            )

            if (heard.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, LifeOSShapes.large)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = heard,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    ConversationOverlay(
        visible = overlayVisible,
        listening = listening,
        conversation = conversation,
        onOrbClick = {
            if (micPermission.granted) viewModel.startListening() else micPermission.request()
        },
        onDismiss = { viewModel.dismissOverlay() },
    )
}
