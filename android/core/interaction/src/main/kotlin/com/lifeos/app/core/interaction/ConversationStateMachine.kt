package com.lifeos.app.core.interaction

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Interaction OS §5's explicit state diagram. Illegal transitions are
 * rejected (return false), not silently allowed — replaces the bare
 * `Boolean listening` a ViewModel used to track this with directly.
 *
 * Not `@Singleton` — one instance per conversation session (feature:voice's
 * ViewModel scope), since Hilt creates a fresh instance per injection site
 * without that annotation.
 */
class ConversationStateMachine @Inject constructor() {

    private val _state = MutableStateFlow(ConversationState.IDLE)
    val state: StateFlow<ConversationState> = _state

    fun transition(event: ConversationEvent): Boolean {
        val next = nextState(_state.value, event) ?: return false
        _state.value = next
        return true
    }

    private fun nextState(current: ConversationState, event: ConversationEvent): ConversationState? {
        // Emergency preempts almost any state — the one thing that must always be heard.
        if (event is ConversationEvent.EmergencyDetected && current != ConversationState.EMERGENCY) {
            return ConversationState.EMERGENCY
        }
        if (event is ConversationEvent.UserCancelled && current !in TERMINAL_STATES) {
            return ConversationState.CANCELLED
        }
        if (event is ConversationEvent.InterruptionReceived && current !in TERMINAL_STATES + ConversationState.EMERGENCY) {
            return ConversationState.INTERRUPTED
        }

        return when (current) {
            ConversationState.IDLE -> when (event) {
                is ConversationEvent.UserStartedSpeaking -> ConversationState.LISTENING
                is ConversationEvent.ReminderFired -> ConversationState.REMINDING
                else -> null
            }
            ConversationState.LISTENING -> when (event) {
                is ConversationEvent.TranscriptReceived -> ConversationState.UNDERSTANDING
                else -> null
            }
            ConversationState.UNDERSTANDING -> when (event) {
                is ConversationEvent.ClarificationNeeded -> ConversationState.CLARIFYING
                is ConversationEvent.IntentResolved -> ConversationState.THINKING
                else -> null
            }
            ConversationState.CLARIFYING -> when (event) {
                is ConversationEvent.UserStartedSpeaking -> ConversationState.LISTENING
                else -> null
            }
            ConversationState.THINKING -> when (event) {
                is ConversationEvent.PlanReady -> ConversationState.EXECUTING
                else -> null
            }
            ConversationState.EXECUTING -> when (event) {
                is ConversationEvent.ExecutionStarted -> ConversationState.WAITING
                is ConversationEvent.ExecutionDone -> ConversationState.IDLE
                else -> null
            }
            ConversationState.WAITING -> when (event) {
                is ConversationEvent.ExecutionDone -> ConversationState.IDLE
                // The elder speaking again — e.g. to answer a pending
                // confirmation — must be able to restart listening rather
                // than getting stuck waiting forever with no way back in.
                is ConversationEvent.UserStartedSpeaking -> ConversationState.LISTENING
                else -> null
            }
            ConversationState.REMINDING -> when (event) {
                is ConversationEvent.ExecutionStarted -> ConversationState.WAITING
                is ConversationEvent.ExecutionDone -> ConversationState.IDLE
                else -> null
            }
            ConversationState.EMERGENCY -> when (event) {
                is ConversationEvent.ExecutionDone -> ConversationState.IDLE
                else -> null
            }
            ConversationState.INTERRUPTED, ConversationState.CANCELLED -> when (event) {
                is ConversationEvent.ConversationEnded -> ConversationState.IDLE
                else -> null
            }
            ConversationState.GOODBYE -> when (event) {
                is ConversationEvent.ConversationEnded -> ConversationState.IDLE
                else -> null
            }
        }
    }

    private companion object {
        val TERMINAL_STATES = setOf(ConversationState.CANCELLED, ConversationState.INTERRUPTED, ConversationState.GOODBYE)
    }
}
