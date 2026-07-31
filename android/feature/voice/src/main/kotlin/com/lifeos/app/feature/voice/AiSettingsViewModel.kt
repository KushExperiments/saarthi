package com.lifeos.app.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.ai.AiApiKeyStore
import com.lifeos.app.core.ai.AiProvider
import com.lifeos.app.core.common.DispatcherProvider
import com.lifeos.app.core.common.Outcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data object Succeeded : ConnectionTestState
    data class Failed(val message: String) : ConnectionTestState
}

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val apiKeyStore: AiApiKeyStore,
    private val aiProvider: AiProvider,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _testState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val testState: StateFlow<ConnectionTestState> = _testState

    init {
        // Keystore-backed reads decrypt on disk — never do that on the
        // caller's thread (this used to run inline in the constructor).
        viewModelScope.launch(dispatchers.io) {
            _apiKey.value = apiKeyStore.getApiKey().orEmpty()
        }
    }

    /** Updates the field's in-memory value only — [testConnection] persists it. */
    fun onApiKeyChanged(key: String) {
        _apiKey.value = key
    }

    fun testConnection() {
        _testState.value = ConnectionTestState.Testing
        viewModelScope.launch(dispatchers.io) {
            // Persist here, not on every keystroke — an EncryptedSharedPreferences
            // write is a synchronous AES-GCM encrypt, too costly to run per character.
            apiKeyStore.setApiKey(_apiKey.value)
            _testState.value = when (val result = aiProvider.verifyKey()) {
                is Outcome.Success -> ConnectionTestState.Succeeded
                is Outcome.Failure -> ConnectionTestState.Failed(result.error.message ?: "Something went wrong.")
                Outcome.Loading -> ConnectionTestState.Testing
            }
        }
    }
}
