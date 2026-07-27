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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saarthi.app.core.ui.NumericKeypad

private const val PIN_LENGTH = 4

@Composable
fun LockScreen(errorFromViewModel: String?, onSubmit: (String) -> Unit) {
    var currentInput by remember { mutableStateOf("") }

    // A wrong PIN clears the local input so the person can try again
    // without needing to manually clear anything themselves.
    LaunchedEffect(errorFromViewModel) {
        if (errorFromViewModel != null) currentInput = ""
    }

    fun onDigit(digit: String) {
        if (currentInput.length >= PIN_LENGTH) return
        currentInput += digit
        if (currentInput.length == PIN_LENGTH) {
            onSubmit(currentInput)
        }
    }

    fun onBackspace() {
        if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Enter your PIN", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))
        PinDots(enteredLength = currentInput.length)
        errorFromViewModel?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(32.dp))
        NumericKeypad(onDigit = ::onDigit, onBackspace = ::onBackspace)
    }
}
