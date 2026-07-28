package com.saarthi.app.feature.voice

import com.saarthi.app.core.testing.MainDispatcherRule
import com.saarthi.app.core.testing.TestDispatcherProvider
import com.saarthi.app.feature.contacts.Contact
import com.saarthi.app.feature.medicines.Medicine
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VoiceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val medicineRepository = LocalFakeMedicineRepository()
    private val contactRepository = LocalFakeContactRepository()
    private val voiceEngine = mockk<VoiceEngine>(relaxed = true)

    private fun viewModel() = VoiceViewModel(
        voiceEngine,
        medicineRepository,
        contactRepository,
        TestDispatcherProvider(),
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
}
