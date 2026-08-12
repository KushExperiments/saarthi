package com.lifeos.app.feature.voice

import com.lifeos.app.core.interaction.ConversationState
import org.junit.Assert.assertEquals
import org.junit.Test

class PresenceStateMapperTest {

    @Test
    fun `emergency always wins, even over a transient flash`() {
        val result = PresenceStateMapper.map(
            conversationState = ConversationState.EMERGENCY,
            speaking = true,
            online = true,
            transientFlash = PresenceState.SUCCESS,
        )
        assertEquals(PresenceState.EMERGENCY, result)
    }

    @Test
    fun `a transient flash wins over the mapped conversation state`() {
        val result = PresenceStateMapper.map(
            conversationState = ConversationState.EXECUTING,
            speaking = false,
            online = true,
            transientFlash = PresenceState.WAKE_WORD,
        )
        assertEquals(PresenceState.WAKE_WORD, result)
    }

    @Test
    fun `speaking wins over the mapped conversation state when no flash is active`() {
        val result = PresenceStateMapper.map(
            conversationState = ConversationState.EXECUTING,
            speaking = true,
            online = true,
            transientFlash = null,
        )
        assertEquals(PresenceState.SPEAKING, result)
    }

    @Test
    fun `listening and clarifying both map to Listening`() {
        assertEquals(
            PresenceState.LISTENING,
            PresenceStateMapper.map(ConversationState.LISTENING, speaking = false, online = true, transientFlash = null),
        )
        assertEquals(
            PresenceState.LISTENING,
            PresenceStateMapper.map(ConversationState.CLARIFYING, speaking = false, online = true, transientFlash = null),
        )
    }

    @Test
    fun `understanding and thinking both map to Thinking`() {
        assertEquals(
            PresenceState.THINKING,
            PresenceStateMapper.map(ConversationState.UNDERSTANDING, speaking = false, online = true, transientFlash = null),
        )
        assertEquals(
            PresenceState.THINKING,
            PresenceStateMapper.map(ConversationState.THINKING, speaking = false, online = true, transientFlash = null),
        )
    }

    @Test
    fun `executing maps to ProcessingAction`() {
        val result = PresenceStateMapper.map(ConversationState.EXECUTING, speaking = false, online = true, transientFlash = null)
        assertEquals(PresenceState.PROCESSING_ACTION, result)
    }

    @Test
    fun `offline only overrides an otherwise-idle presence, never mid-turn`() {
        val idleOffline = PresenceStateMapper.map(ConversationState.IDLE, speaking = false, online = false, transientFlash = null)
        assertEquals(PresenceState.OFFLINE, idleOffline)

        val executingOffline = PresenceStateMapper.map(ConversationState.EXECUTING, speaking = false, online = false, transientFlash = null)
        assertEquals(
            "a live turn should not be interrupted by a connectivity flicker",
            PresenceState.PROCESSING_ACTION,
            executingOffline,
        )
    }

    @Test
    fun `idle and online maps to Idle, not Offline`() {
        val result = PresenceStateMapper.map(ConversationState.IDLE, speaking = false, online = true, transientFlash = null)
        assertEquals(PresenceState.IDLE, result)
    }
}
