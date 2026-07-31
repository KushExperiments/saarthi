package com.lifeos.app.core.interaction

sealed interface ConversationEvent {
    data object UserStartedSpeaking : ConversationEvent
    data class TranscriptReceived(val transcript: String) : ConversationEvent
    data object ClarificationNeeded : ConversationEvent
    data class IntentResolved(val action: String) : ConversationEvent
    data class PlanReady(val action: String) : ConversationEvent
    data object ExecutionStarted : ConversationEvent
    data object ExecutionDone : ConversationEvent
    data object ReminderFired : ConversationEvent
    data object EmergencyDetected : ConversationEvent
    data class InterruptionReceived(val reason: String) : ConversationEvent
    data object UserCancelled : ConversationEvent
    data object ConversationEnded : ConversationEvent
}
