package com.saarthi.app.core.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.saarthi.app.core.ui.NumericKeypad

private const val PIN_LENGTH = 4

/**
 * First-run: choose a PIN, then confirm it. Never shows the digits typed —
 * only [PinDots] progress — and resets to stage one on a mismatch rather
 * than asking the person to "just try again," which would be confusing
 * about which stage they're actually on.
 */
@Composable
fun PinSetupScreen(viewModel: AuthViewModel = hiltViewModel()) {
    var firstPin by remember { mutableStateOf<String?>(null) }
    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isConfirmStage = firstPin != null
    val title = if (isConfirmStage) "Type it again to confirm" else "Choose a PIN to lock Saarthi"

    fun onDigit(digit: String) {
        if (currentInput.length >= PIN_LENGTH) return
        errorMessage = null
        currentInput += digit
        if (currentInput.length == PIN_LENGTH) {
            if (!isConfirmStage) {
                firstPin = currentInput
                currentInput = ""
            } else {
                val result = viewModel.onPinCreated(pin = firstPin.orEmpty(), confirmPin = currentInput)
                if (result != null) {
                    errorMessage = result
                    firstPin = null
                    currentInput = ""
                }
            }
        }
    }

    fun onBackspace() {
        errorMessage = null
        if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        PinDots(enteredLength = currentInput.length)
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(32.dp))
        NumericKeypad(onDigit = ::onDigit, onBackspace = ::onBackspace)
    }
}
