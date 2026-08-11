package com.lifeos.app.feature.voice

import com.lifeos.app.core.cognitive.ActionPlan
import com.lifeos.app.core.cognitive.Verdict
import com.lifeos.app.core.interaction.ConversationState
import com.lifeos.app.core.interaction.ConversationStateMachine
import com.lifeos.app.core.interaction.DialogueManager
import com.lifeos.app.core.interaction.DialogueResult
import com.lifeos.app.core.interaction.VoiceEngine
import com.lifeos.app.core.interaction.WakeSignal
import com.lifeos.app.core.testing.MainDispatcherRule
import com.lifeos.app.core.testing.TestDispatcherProvider
import com.lifeos.app.feature.contacts.Contact
import com.lifeos.app.feature.medicines.Medicine
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VoiceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val medicineRepository = LocalFakeMedicineRepository()
    private val contactRepository = LocalFakeContactRepository()
    private val voiceEngine = mockk<VoiceEngine>(relaxed = true)
    private val dialogueManager = mockk<DialogueManager>()

    private fun viewModel() = VoiceViewModel(
        voiceEngine,
        medicineRepository,
        contactRepository,
        dialogueManager,
        ConversationStateMachine(),
        TestDispatcherProvider(),
        WakeSignal(),
    )

    @Test
    fun `startListening routes a recognized call command to a PlaceCall effect`() = runBlocking {
        contactRepository.save(Contact(id = "1", name = "Beta", phone = "+911234567890"))
        val onResultSlot = slot<(String) -> Unit>()
        every { voiceEngine.listen(capture(onResultSlot), any(), any()) } answers {
            onResultSlot.captured.invoke("call beta")
        }

        val viewModel = viewModel()
        viewModel.startListening()

        assertTrue(viewModel.effect.value is VoiceUiEffect.PlaceCall)
        assertEquals("Beta", (viewModel.effect.value as VoiceUiEffect.PlaceCall).contact.name)
    }

    @Test
    fun `consumeEffect clears the pending effect`() = runBlocking {
        contactRepository.save(Contact(id = "1", name = "Beta", phone = "+911234567890"))
        val onResultSlot = slot<(String) -> Unit>()
        every { voiceEngine.listen(capture(onResultSlot), any(), any()) } answers {
            onResultSlot.captured.invoke("call beta")
        }
        val viewModel = viewModel()
        viewModel.startListening()

        viewModel.consumeEffect()

        assertEquals(null, viewModel.effect.value)
    }

    @Test
    fun `startListening opens the overlay and records both sides of the conversation`() = runBlocking {
        contactRepository.save(Contact(id = "1", name = "Beta", phone = "+911234567890"))
        val onResultSlot = slot<(String) -> Unit>()
        every { voiceEngine.listen(capture(onResultSlot), any(), any()) } answers {
            onResultSlot.captured.invoke("call beta")
        }

        val viewModel = viewModel()
        assertFalse(viewModel.overlayVisible.value)

        viewModel.startListening()

        assertTrue(viewModel.overlayVisible.value)
        assertTrue(viewModel.conversation.value.any { it.fromUser && it.text.contains("call beta") })
        assertTrue(viewModel.conversation.value.any { !it.fromUser && it.text.contains("Calling Beta") })

        viewModel.dismissOverlay()
        assertFalse(viewModel.overlayVisible.value)
    }

    @Test
    fun `I took it marks the earliest due, unconfirmed dose taken`() = runBlocking {
        medicineRepository.save(
            Medicine(id = "m1", name = "Aspirin", times = listOf("00:01"), confirmedDate = null, confirmedTimes = emptyList()),
        )
        val onResultSlot = slot<(String) -> Unit>()
        every { voiceEngine.listen(capture(onResultSlot), any(), any()) } answers {
            onResultSlot.captured.invoke("I took it")
        }

        val viewModel = viewModel()
        viewModel.startListening()

        val updated = medicineRepository.observeAll().value.single()
        assertTrue(updated.isConfirmed("00:01"))
    }

    @Test
    fun `unrecognized transcripts fall through to the DialogueManager, not silently ignored`() = runBlocking {
        val onResultSlot = slot<(String) -> Unit>()
        every { voiceEngine.listen(capture(onResultSlot), any(), any()) } answers {
            onResultSlot.captured.invoke("what's the weather like")
        }
        coEvery { dialogueManager.handle(any(), any()) } returns DialogueResult.Respond("I'm not sure, sorry.")

        val viewModel = viewModel()
        viewModel.startListening()

        assertTrue(viewModel.conversation.value.any { !it.fromUser && it.text == "I'm not sure, sorry." })
    }

    @Test
    fun `an escalated ActionPlan asks for confirmation instead of firing its effect`() = runBlocking {
        val onResultSlot = slot<(String) -> Unit>()
        every { voiceEngine.listen(capture(onResultSlot), any(), any()) } answers {
            onResultSlot.captured.invoke("send a message to beta")
        }
        val plan = ActionPlan(
            action = "sms",
            params = mapOf("person" to "Beta"),
            confidence = 0.8f,
            verdict = Verdict.ESCALATE,
            explanation = "Chose \"sms\": always requires human confirmation (Ethical Policy)",
            alternatives = emptyList(),
        )
        coEvery { dialogueManager.handle(any(), any()) } returns DialogueResult.Act(plan)

        val viewModel = viewModel()
        viewModel.startListening()

        assertEquals(null, viewModel.effect.value)
        assertTrue(viewModel.conversation.value.any { !it.fromUser && it.text.contains("confirm") })
        // The turn isn't actually finished — it's waiting on the elder to confirm,
        // so the conversation must not silently settle all the way back to idle.
        assertEquals(ConversationState.WAITING, viewModel.conversationState.value)
    }
}
