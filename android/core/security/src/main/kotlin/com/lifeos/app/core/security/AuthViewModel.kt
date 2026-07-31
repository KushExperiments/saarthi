package com.lifeos.app.core.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.common.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_PIN_LENGTH = 4

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            val next = if (authRepository.isPinSet()) AuthUiState.Locked() else AuthUiState.NeedsSetup
            _state.value = next
        }
    }

    /** Returns a validation error message, or null if the PIN was accepted and stored. */
    fun onPinCreated(pin: String, confirmPin: String): String? {
        if (pin.length < MIN_PIN_LENGTH) return "Please use at least $MIN_PIN_LENGTH digits."
        if (pin != confirmPin) return "The PINs don't match. Please try again."
        viewModelScope.launch(dispatchers.io) {
            authRepository.setPin(pin)
            _state.value = AuthUiState.Unlocked
        }
        return null
    }

    fun attemptUnlock(pin: String) {
        viewModelScope.launch(dispatchers.io) {
            if (authRepository.verifyPin(pin)) {
                _state.value = AuthUiState.Unlocked
            } else {
                _state.value = AuthUiState.Locked(error = "That PIN isn't right. Please try again.")
            }
        }
    }
}
