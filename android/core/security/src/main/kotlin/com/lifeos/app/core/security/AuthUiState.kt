package com.lifeos.app.core.security

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object NeedsSetup : AuthUiState
    data class Locked(val error: String? = null) : AuthUiState
    data object Unlocked : AuthUiState
}
