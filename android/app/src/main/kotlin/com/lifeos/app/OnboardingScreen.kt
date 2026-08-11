package com.lifeos.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifeos.app.core.designsystem.LifeOSMotion
import com.lifeos.app.core.ui.LifeOSButton
import com.lifeos.app.feature.voice.VoiceOrb

/**
 * The very first thing anyone sees, before the PIN lock exists — a warm,
 * spoken introduction rather than a silent splash screen. Shown exactly
 * once (see [OnboardingPrefs]); everything after this sits behind AuthGate.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        viewModel.greet()
    }

    // A calm fade-in rather than an abrupt appearance — the very first
    // impression of the app, so it should feel settled, not snappy.
    val entranceAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = LifeOSMotion.TRANSITION_EMPHASIZED_MS, easing = LifeOSMotion.SettleEasing),
        label = "onboardingEntrance",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .alpha(entranceAlpha),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VoiceOrb(listening = false, onClick = { viewModel.greet() })

        Spacer(modifier = Modifier.height(40.dp))
        Text(text = "Hello 👋", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "I'm Juno. I can remind you about medicines, help you call " +
                "family, and more — all with your voice.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("What should I call you?") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))
        LifeOSButton(
            text = "Get Started",
            onClick = {
                viewModel.rememberName(name)
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
