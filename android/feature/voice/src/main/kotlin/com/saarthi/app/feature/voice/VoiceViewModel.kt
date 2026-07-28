package com.saarthi.app.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saarthi.app.core.common.DispatcherProvider
import com.saarthi.app.feature.contacts.Contact
import com.saarthi.app.feature.contacts.ContactRepository
import com.saarthi.app.feature.medicines.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot instructions for the screen to actually perform (call/WhatsApp), then clear. */
sealed interface VoiceUiEffect {
    data class PlaceCall(val contact: Contact) : VoiceUiEffect
    data class OpenWhatsApp(val contact: Contact) : VoiceUiEffect
    data object NavigateToMedicines : VoiceUiEffect
}

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceEngine: VoiceEngine,
    private val medicineRepository: MedicineRepository,
    private val contactRepository: ContactRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening

    private val _heard = MutableStateFlow("")
    val heard: StateFlow<String> = _heard

    private val _effect = MutableStateFlow<VoiceUiEffect?>(null)
    val effect: StateFlow<VoiceUiEffect?> = _effect

    private val contacts: StateFlow<List<Contact>> = contactRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun consumeEffect() {
        _effect.value = null
    }

    fun startListening() {
        _listening.value = true
        _heard.value = ""
        voiceEngine.listen(
            onResult = { transcript -> handleTranscript(transcript) },
            onError = { message -> _heard.value = message },
            onDone = { _listening.value = false },
        )
    }

    fun say(text: String) {
        _heard.value = text
        voiceEngine.speak(text)
    }

    private fun handleTranscript(transcript: String) {
        _heard.value = "“$transcript”"
        viewModelScope.launch(dispatchers.io) {
            when (val command = CommandRouter.route(transcript, contacts.value)) {
                is VoiceCommand.Call -> {
                    say("Calling ${command.contact.name}.")
                    _effect.value = VoiceUiEffect.PlaceCall(command.contact)
                }
                is VoiceCommand.WhatsApp -> {
                    say("Opening WhatsApp for ${command.contact.name}.")
                    _effect.value = VoiceUiEffect.OpenWhatsApp(command.contact)
                }
                is VoiceCommand.NeedsContact ->
                    say("Who should I contact? Please add them in Call Someone first.")
                VoiceCommand.MedicineTaken -> {
                    val confirmed = markEarliestDue()
                    say(if (confirmed) "Well done. I marked your medicine as taken." else "I don't see a medicine due right now.")
                }
                VoiceCommand.OpenMedicines -> {
                    say("Opening your medicines.")
                    _effect.value = VoiceUiEffect.NavigateToMedicines
                }
                is VoiceCommand.Unrecognized ->
                    say("Sorry, I didn't understand. You can say things like “call beta” or “I took my medicine”.")
            }
        }
    }

    private suspend fun markEarliestDue(): Boolean {
        val now = java.time.LocalTime.now().toString().take(5)
        val due = medicineRepository.observeAll().first()
            .flatMap { medicine -> medicine.times.map { medicine to it } }
            .filter { (medicine, time) -> !medicine.isConfirmed(time) && time <= now }
            .minByOrNull { (_, time) -> time }
        val (medicine, time) = due ?: return false
        medicineRepository.markTaken(medicine.id, time)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
    }
}
