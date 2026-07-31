package com.lifeos.app.feature.voice

import com.lifeos.app.feature.contacts.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRouterTest {

    private val beta = Contact(id = "1", name = "Beta", phone = "+911234567890")
    private val contacts = listOf(beta)

    @Test
    fun `call beta resolves the matching contact`() {
        val result = CommandRouter.route("call beta", contacts)

        assertEquals(VoiceCommand.Call(beta), result)
    }

    @Test
    fun `whatsapp beta routes to WhatsApp not a plain call`() {
        val result = CommandRouter.route("whatsapp beta", contacts)

        assertEquals(VoiceCommand.WhatsApp(beta), result)
    }

    @Test
    fun `hindi call keyword also resolves the contact`() {
        val result = CommandRouter.route("beta ko call karo", contacts)

        assertEquals(VoiceCommand.Call(beta), result)
    }

    @Test
    fun `calling an unknown name asks to add the contact first`() {
        val result = CommandRouter.route("call someone-not-saved", contacts)

        assertTrue(result is VoiceCommand.NeedsContact)
    }

    @Test
    fun `I took it routes to MedicineTaken`() {
        assertEquals(VoiceCommand.MedicineTaken, CommandRouter.route("I took it", contacts))
        assertEquals(VoiceCommand.MedicineTaken, CommandRouter.route("khaya", contacts))
    }

    @Test
    fun `medicine mention without taken keyword opens the medicines list`() {
        assertEquals(VoiceCommand.OpenMedicines, CommandRouter.route("show my medicines", contacts))
    }

    @Test
    fun `settings mention opens settings`() {
        assertEquals(VoiceCommand.OpenSettings, CommandRouter.route("open settings", contacts))
    }

    @Test
    fun `unmatched speech is Unrecognized, never silently guessed`() {
        val result = CommandRouter.route("what's the weather like", contacts)

        assertTrue(result is VoiceCommand.Unrecognized)
    }
}
