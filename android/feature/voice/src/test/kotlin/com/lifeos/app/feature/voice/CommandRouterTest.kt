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

    @Test
    fun `remember that extracts the statement verbatim, preserving original casing`() {
        val result = CommandRouter.route("remember that my daughter lives in Pune", contacts)

        assertEquals(VoiceCommand.Remember("my daughter lives in Pune"), result)
    }

    @Test
    fun `remember without a leading that still extracts the statement`() {
        val result = CommandRouter.route("please remember my locker code is 4471", contacts)

        assertEquals(VoiceCommand.Remember("my locker code is 4471"), result)
    }

    @Test
    fun `what do you remember about X extracts the topic, not ShowMemories`() {
        val result = CommandRouter.route("what do you remember about my daughter", contacts)

        assertEquals(VoiceCommand.RecallAbout("my daughter"), result)
    }

    @Test
    fun `what do you remember about me maps to ShowMemories, not a literal topic`() {
        val result = CommandRouter.route("what do you remember about me", contacts)

        assertEquals(VoiceCommand.ShowMemories, result)
    }

    @Test
    fun `show me my important memories maps to ShowMemories`() {
        val result = CommandRouter.route("show me my important memories", contacts)

        assertEquals(VoiceCommand.ShowMemories, result)
    }

    @Test
    fun `forget that with no topic maps to ForgetLast`() {
        assertEquals(VoiceCommand.ForgetLast, CommandRouter.route("forget that", contacts))
    }

    @Test
    fun `forget about a topic maps to ForgetAbout with that topic`() {
        val result = CommandRouter.route("forget about my locker code", contacts)

        assertEquals(VoiceCommand.ForgetAbout("my locker code"), result)
    }

    @Test
    fun `why did you remember maps to WhyRemembered`() {
        assertEquals(VoiceCommand.WhyRemembered, CommandRouter.route("why did you remember that", contacts))
    }
}
