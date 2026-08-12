package com.lifeos.app.feature.voice

import com.lifeos.app.core.interaction.ConversationState

/**
 * Pure derivation of what Juno's Presence should show — never a second
 * source of truth about what Juno is *doing* (that's [ConversationState]
 * and [com.lifeos.app.core.interaction.VoiceEngine] alone), only a mapping
 * from those real signals to a visual state. Kept as a standalone,
 * Android-free function so it's testable without a ViewModel or a
 * Composable.
 */
object PresenceStateMapper {

    fun map(
        conversationState: ConversationState,
        speaking: Boolean,
        online: Boolean,
        transientFlash: PresenceState?,
    ): PresenceState {
        if (conversationState == ConversationState.EMERGENCY) return PresenceState.EMERGENCY
        if (transientFlash != null) return transientFlash
        if (speaking) return PresenceState.SPEAKING

        val mapped = when (conversationState) {
            ConversationState.LISTENING, ConversationState.CLARIFYING -> PresenceState.LISTENING
            ConversationState.UNDERSTANDING, ConversationState.THINKING -> PresenceState.THINKING
            ConversationState.EXECUTING -> PresenceState.PROCESSING_ACTION
            ConversationState.REMINDING -> PresenceState.SPEAKING
            ConversationState.IDLE,
            ConversationState.WAITING,
            ConversationState.INTERRUPTED,
            ConversationState.CANCELLED,
            ConversationState.GOODBYE,
            ConversationState.EMERGENCY,
            -> PresenceState.IDLE
        }

        // Offline only ever overrides an otherwise-idle Presence — a live
        // conversation turn shouldn't be interrupted by a connectivity
        // flicker mid-turn.
        return if (mapped == PresenceState.IDLE && !online) PresenceState.OFFLINE else mapped
    }
}
