package com.lifeos.app.core.security

import com.lifeos.app.core.testing.MainDispatcherRule
import com.lifeos.app.core.testing.TestDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(repo: FakeAuthRepository = FakeAuthRepository()) =
        AuthViewModel(repo, TestDispatcherProvider())

    @Test
    fun `starts in NeedsSetup when no PIN has ever been set`() {
        val viewModel = viewModel()

        assertEquals(AuthUiState.NeedsSetup, viewModel.state.value)
    }

    @Test
    fun `starts in Locked when a PIN already exists`() {
        val repo = FakeAuthRepository().apply { setPin("4821") }

        val viewModel = viewModel(repo)

        assertTrue(viewModel.state.value is AuthUiState.Locked)
    }

    @Test
    fun `onPinCreated rejects a mismatched confirmation and stays in NeedsSetup`() {
        val viewModel = viewModel()

        val error = viewModel.onPinCreated(pin = "4821", confirmPin = "1234")

        assertNotNull(error)
        assertEquals(AuthUiState.NeedsSetup, viewModel.state.value)
    }

    @Test
    fun `onPinCreated accepts a matching PIN and unlocks`() {
        val viewModel = viewModel()

        val error = viewModel.onPinCreated(pin = "4821", confirmPin = "4821")

        assertNull(error)
        assertEquals(AuthUiState.Unlocked, viewModel.state.value)
    }

    @Test
    fun `attemptUnlock with the correct PIN unlocks`() {
        val repo = FakeAuthRepository().apply { setPin("4821") }
        val viewModel = viewModel(repo)

        viewModel.attemptUnlock("4821")

        assertEquals(AuthUiState.Unlocked, viewModel.state.value)
    }

    @Test
    fun `attemptUnlock with the wrong PIN stays locked with an error`() {
        val repo = FakeAuthRepository().apply { setPin("4821") }
        val viewModel = viewModel(repo)

        viewModel.attemptUnlock("0000")

        val state = viewModel.state.value
        assertTrue(state is AuthUiState.Locked)
        assertNotNull((state as AuthUiState.Locked).error)
    }
}
