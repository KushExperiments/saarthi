package com.saarthi.app.feature.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.saarthi.app.feature.contacts.ContactActions
import com.saarthi.app.feature.medicines.MedicinesRoute

/**
 * The whole point of the voice-first redesign this project already went
 * through in its web prototype: no button grid, one big microphone. Any
 * other screen (Medicines, Contacts) is reached by voice, not by tapping
 * around a menu.
 */
@Composable
fun VoiceHomeScreen(navController: NavHostController, viewModel: VoiceViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val listening by viewModel.listening.collectAsStateWithLifecycle()
    val heard by viewModel.heard.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.say("Hello. I am Saarthi. Tap the microphone and talk to me.")
    }

    LaunchedEffect(effect) {
        when (val current = effect) {
            is VoiceUiEffect.PlaceCall -> ContactActions.dial(context, current.contact)
            is VoiceUiEffect.OpenWhatsApp -> ContactActions.openWhatsApp(context, current.contact)
            VoiceUiEffect.NavigateToMedicines -> navController.navigate(MedicinesRoute.route)
            null -> Unit
        }
        if (current != null) viewModel.consumeEffect()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Hello 👋", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.startListening() },
            shape = CircleShape,
            modifier = Modifier.size(140.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(text = if (listening) "🛑" else "🎤", style = MaterialTheme.typography.displayLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (listening) "Listening…" else "Tap and talk to me",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = heard, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
