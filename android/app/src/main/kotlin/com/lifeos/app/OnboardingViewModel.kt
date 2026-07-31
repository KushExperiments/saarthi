package com.lifeos.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.common.DispatcherProvider
import com.lifeos.app.core.interaction.VoiceEngine
import com.lifeos.app.core.memory.MemoryCategory
import com.lifeos.app.core.memory.MemoryRepository
import com.lifeos.app.core.memory.MemorySource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val voiceEngine: VoiceEngine,
    private val memoryRepository: MemoryRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    fun greet() {
        voiceEngine.speak(
            "Hello. I'm LifeOS, your voice companion. " +
                "I can remind you about medicines, help you call family, and more, all with your voice. " +
                "Let's get started.",
        )
    }

    /** Called once, when the elder (or the family member setting up the phone) finishes onboarding. */
    fun rememberName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch(dispatchers.io) {
            memoryRepository.remember(
                category = MemoryCategory.IDENTITY,
                label = "preferred name",
                valueText = trimmed,
                source = MemorySource.USER_STATED,
                sourceDetail = "Entered during onboarding",
                now = System.currentTimeMillis(),
            )
        }
    }
}
