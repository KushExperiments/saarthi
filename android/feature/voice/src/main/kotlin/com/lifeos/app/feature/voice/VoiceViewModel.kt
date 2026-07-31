package com.lifeos.app.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.cognitive.ActionPlan
import com.lifeos.app.core.cognitive.Verdict
import com.lifeos.app.core.common.DispatcherProvider
import com.lifeos.app.core.interaction.ConversationEvent
import com.lifeos.app.core.interaction.ConversationState
import com.lifeos.app.core.interaction.ConversationStateMachine
import com.lifeos.app.core.interaction.DialogueContext
import com.lifeos.app.core.interaction.DialogueManager
import com.lifeos.app.core.interaction.DialogueResult
import com.lifeos.app.core.interaction.NamedEntity
import com.lifeos.app.core.interaction.VoiceEngine
import com.lifeos.app.feature.contacts.Contact
import com.lifeos.app.feature.contacts.ContactRepository
import com.lifeos.app.feature.medicines.MedicineRepository
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
    data object NavigateToSettings : VoiceUiEffect
}

/** One line of the running conversation shown in [ConversationOverlay]. */
data class ConversationTurn(val fromUser: Boolean, val text: String)

private const val MAX_HISTORY_TURNS = 6

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceEngine: VoiceEngine,
    private val medicineRepository: MedicineRepository,
    private val contactRepository: ContactRepository,
    private val dialogueManager: DialogueManager,
    private val stateMachine: ConversationStateMachine,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening

    private val _heard = MutableStateFlow("")
    val heard: StateFlow<String> = _heard

    private val _effect = MutableStateFlow<VoiceUiEffect?>(null)
    val effect: StateFlow<VoiceUiEffect?> = _effect

    private val _conversation = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val conversation: StateFlow<List<ConversationTurn>> = _conversation

    private val _overlayVisible = MutableStateFlow(false)
    val overlayVisible: StateFlow<Boolean> = _overlayVisible

    val conversationState: StateFlow<ConversationState> = stateMachine.state

    private val contacts: StateFlow<List<Contact>> = contactRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun consumeEffect() {
        _effect.value = null
    }

    fun dismissOverlay() {
        _overlayVisible.value = false
    }

    fun startListening() {
        _overlayVisible.value = true
        _listening.value = true
        _heard.value = ""
        stateMachine.transition(ConversationEvent.UserStartedSpeaking)
        voiceEngine.listen(
            onResult = { transcript -> handleTranscript(transcript) },
            onError = { message -> _heard.value = message },
            onDone = { _listening.value = false },
        )
    }

    fun say(text: String) {
        _heard.value = text
        _conversation.value = _conversation.value + ConversationTurn(fromUser = false, text = text)
        voiceEngine.speak(text)
    }

    private fun handleTranscript(transcript: String) {
        _heard.value = "“$transcript”"
        _conversation.value = _conversation.value + ConversationTurn(fromUser = true, text = transcript)
        stateMachine.transition(ConversationEvent.TranscriptReceived(transcript))

        viewModelScope.launch(dispatchers.io) {
            val resolvedAction = when (val command = CommandRouter.route(transcript, contacts.value)) {
                is VoiceCommand.Call -> {
                    say("Calling ${command.contact.name}.")
                    _effect.value = VoiceUiEffect.PlaceCall(command.contact)
                    "call"
                }
                is VoiceCommand.WhatsApp -> {
                    say("Opening WhatsApp for ${command.contact.name}.")
                    _effect.value = VoiceUiEffect.OpenWhatsApp(command.contact)
                    "whatsapp"
                }
                is VoiceCommand.NeedsContact -> {
                    say("Who should I contact? Please add them in Call Someone first.")
                    "needs_contact"
                }
                VoiceCommand.MedicineTaken -> {
                    val confirmed = markEarliestDue()
                    say(if (confirmed) "Well done. I marked your medicine as taken." else "I don't see a medicine due right now.")
                    "medicine_taken"
                }
                VoiceCommand.OpenMedicines -> {
                    say("Opening your medicines.")
                    _effect.value = VoiceUiEffect.NavigateToMedicines
                    "open_medicines"
                }
                VoiceCommand.OpenSettings -> {
                    say("Opening settings.")
                    _effect.value = VoiceUiEffect.NavigateToSettings
                    "open_settings"
                }
                is VoiceCommand.Unrecognized -> handleViaDialogueManager(transcript)
            }
            settleState(resolvedAction)
        }
    }

    /**
     * CommandRouter didn't recognize it — fall through to Cognitive OS via
     * the DialogueManager. Returns the resolved action name to settle the
     * state machine with, or null if the turn already settled itself (a
     * clarification question leaves the conversation in CLARIFYING,
     * awaiting the user's answer, rather than resetting to idle).
     */
    private suspend fun handleViaDialogueManager(transcript: String): String? {
        val recentHistory = _conversation.value.takeLast(MAX_HISTORY_TURNS).map { it.text }
        val knownEntities = contacts.value.map { NamedEntity(id = it.id, name = it.name, kind = "contact") }

        return when (val result = dialogueManager.handle(transcript, DialogueContext(knownEntities, recentHistory))) {
            is DialogueResult.Clarify -> {
                stateMachine.transition(ConversationEvent.ClarificationNeeded)
                say(result.question)
                null
            }
            is DialogueResult.Respond -> {
                say(result.text)
                "answer"
            }
            is DialogueResult.Act -> {
                handleActionPlan(result.plan)
                if (result.plan.verdict == Verdict.ESCALATE) {
                    // The turn isn't actually done — it's waiting on the
                    // elder to confirm, so don't settle all the way to IDLE.
                    settleToWaiting()
                    null
                } else {
                    result.plan.action
                }
            }
            DialogueResult.Unhandled -> {
                say("Sorry, I didn't understand. You can say things like “call beta” or “I took my medicine”.")
                "none"
            }
        }
    }

    /** [ActionPlan.verdict] is what gates execution — an ESCALATE never fires its effect silently. */
    private suspend fun handleActionPlan(plan: ActionPlan) {
        if (plan.verdict == Verdict.REJECT) {
            say("Sorry, I didn't understand. You can say things like “call beta” or “I took my medicine”.")
            return
        }
        if (plan.verdict == Verdict.ESCALATE) {
            say("${plan.explanation.substringAfter(": ")} Please confirm by tapping the screen.")
            return
        }

        when (plan.action) {
            "call", "whatsapp" -> {
                val contact = plan.params["person"]?.let { name -> contacts.value.find { it.name == name } }
                if (contact == null) {
                    say("Who should I contact? Please add them in Call Someone first.")
                } else if (plan.action == "call") {
                    say("Calling ${contact.name}.")
                    _effect.value = VoiceUiEffect.PlaceCall(contact)
                } else {
                    say("Opening WhatsApp for ${contact.name}.")
                    _effect.value = VoiceUiEffect.OpenWhatsApp(contact)
                }
            }
            "medicine_taken" -> {
                val confirmed = markEarliestDue()
                say(if (confirmed) "Well done. I marked your medicine as taken." else "I don't see a medicine due right now.")
            }
            "open_medicines" -> {
                say("Opening your medicines.")
                _effect.value = VoiceUiEffect.NavigateToMedicines
            }
            "open_settings" -> {
                say("Opening settings.")
                _effect.value = VoiceUiEffect.NavigateToSettings
            }
            "help" -> say("I'm here. Tell me what's wrong, or say a family member's name to call them.")
            else -> say("I understand, but I can't do that yet.")
        }
    }

    /** Walks UNDERSTANDING -> THINKING -> EXECUTING -> IDLE now that a turn has resolved to [resolvedAction]. */
    private fun settleState(resolvedAction: String?) {
        if (resolvedAction == null) return
        if (stateMachine.state.value == ConversationState.UNDERSTANDING) {
            stateMachine.transition(ConversationEvent.IntentResolved(resolvedAction))
        }
        if (stateMachine.state.value == ConversationState.THINKING) {
            stateMachine.transition(ConversationEvent.PlanReady(resolvedAction))
        }
        if (stateMachine.state.value == ConversationState.EXECUTING) {
            stateMachine.transition(ConversationEvent.ExecutionDone)
        }
    }

    /** Like [settleState], but stops at WAITING — used when the turn is escalated, not actually finished. */
    private fun settleToWaiting() {
        if (stateMachine.state.value == ConversationState.UNDERSTANDING) {
            stateMachine.transition(ConversationEvent.IntentResolved(""))
        }
        if (stateMachine.state.value == ConversationState.THINKING) {
            stateMachine.transition(ConversationEvent.PlanReady(""))
        }
        if (stateMachine.state.value == ConversationState.EXECUTING) {
            stateMachine.transition(ConversationEvent.ExecutionStarted)
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
