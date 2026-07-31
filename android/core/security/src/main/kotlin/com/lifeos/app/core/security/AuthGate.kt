package com.lifeos.app.core.security

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The single entry point every later feature module's screens sit behind.
 * `unlockedContent` is only ever composed once [AuthUiState.Unlocked] is
 * reached — nothing behind the lock is ever in the composition beforehand.
 */
@Composable
fun AuthGate(
    viewModel: AuthViewModel = hiltViewModel(),
    unlockedContent: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        AuthUiState.Loading -> Box(modifier = Modifier.fillMaxSize())
        AuthUiState.NeedsSetup -> PinSetupScreen(viewModel = viewModel)
        is AuthUiState.Locked -> LockScreen(errorFromViewModel = current.error, onSubmit = viewModel::attemptUnlock)
        AuthUiState.Unlocked -> unlockedContent()
    }
}
