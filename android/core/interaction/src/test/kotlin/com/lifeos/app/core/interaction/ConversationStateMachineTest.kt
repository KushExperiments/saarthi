package com.lifeos.app.core.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationStateMachineTest {

    @Test
    fun `starts in IDLE`() {
        val machine = ConversationStateMachine()

        assertEquals(ConversationState.IDLE, machine.state.value)
    }

    @Test
    fun `the full happy path walks Idle through Understanding to Idle again`() {
        val machine = ConversationStateMachine()

        assertTrue(machine.transition(ConversationEvent.UserStartedSpeaking))
        assertEquals(ConversationState.LISTENING, machine.state.value)

        assertTrue(machine.transition(ConversationEvent.TranscriptReceived("call beta")))
        assertEquals(ConversationState.UNDERSTANDING, machine.state.value)

        assertTrue(machine.transition(ConversationEvent.IntentResolved("call")))
        assertEquals(ConversationState.THINKING, machine.state.value)

        assertTrue(machine.transition(ConversationEvent.PlanReady("call")))
        assertEquals(ConversationState.EXECUTING, machine.state.value)

        assertTrue(machine.transition(ConversationEvent.ExecutionDone))
        assertEquals(ConversationState.IDLE, machine.state.value)
    }

    @Test
    fun `an illegal transition is rejected and the state does not change`() {
        val machine = ConversationStateMachine()

        // Can't go straight from IDLE to THINKING — must pass through Listening/Understanding first.
        val accepted = machine.transition(ConversationEvent.PlanReady("call"))

        assertFalse(accepted)
        assertEquals(ConversationState.IDLE, machine.state.value)
    }

    @Test
    fun `emergency preempts whatever state the conversation was in`() {
        val machine = ConversationStateMachine()
        machine.transition(ConversationEvent.UserStartedSpeaking)
        machine.transition(ConversationEvent.TranscriptReceived("help"))

        val accepted = machine.transition(ConversationEvent.EmergencyDetected)

        assertTrue(accepted)
        assertEquals(ConversationState.EMERGENCY, machine.state.value)
    }

    @Test
    fun `a clarification round-trips back through Listening, not straight to Understanding`() {
        val machine = ConversationStateMachine()
        machine.transition(ConversationEvent.UserStartedSpeaking)
        machine.transition(ConversationEvent.TranscriptReceived("call someone"))
        machine.transition(ConversationEvent.ClarificationNeeded)
        assertEquals(ConversationState.CLARIFYING, machine.state.value)

        assertTrue(machine.transition(ConversationEvent.UserStartedSpeaking))
        assertEquals(ConversationState.LISTENING, machine.state.value)
    }

    @Test
    fun `a reminder can fire from Idle and settles back to Idle once acknowledged`() {
        val machine = ConversationStateMachine()

        machine.transition(ConversationEvent.ReminderFired)
        assertEquals(ConversationState.REMINDING, machine.state.value)

        machine.transition(ConversationEvent.ExecutionDone)
        assertEquals(ConversationState.IDLE, machine.state.value)
    }

    @Test
    fun `WAITING can restart listening rather than being stuck forever with no way out`() {
        val machine = ConversationStateMachine()
        machine.transition(ConversationEvent.UserStartedSpeaking)
        machine.transition(ConversationEvent.TranscriptReceived("call beta"))
        machine.transition(ConversationEvent.IntentResolved("call"))
        machine.transition(ConversationEvent.PlanReady("call"))
        machine.transition(ConversationEvent.ExecutionStarted)
        assertEquals(ConversationState.WAITING, machine.state.value)

        assertTrue(machine.transition(ConversationEvent.UserStartedSpeaking))
        assertEquals(ConversationState.LISTENING, machine.state.value)
    }

    @Test
    fun `cancellation from a terminal state does not re-trigger itself`() {
        val machine = ConversationStateMachine()
        machine.transition(ConversationEvent.UserCancelled)
        assertEquals(ConversationState.CANCELLED, machine.state.value)

        val acceptedAgain = machine.transition(ConversationEvent.UserCancelled)

        assertFalse(acceptedAgain)
        assertEquals(ConversationState.CANCELLED, machine.state.value)
    }
}
