package com.lifeos.app.feature.voice

import com.lifeos.app.feature.contacts.Contact

/**
 * Deterministic keyword matching — the guaranteed floor tier from
 * Architecture §10/§11's fallback ladder, not an AI call. Pure logic, no
 * Android framework dependency, so it's trivially unit-testable.
 */
sealed interface VoiceCommand {
    data class Call(val contact: Contact) : VoiceCommand
    data class WhatsApp(val contact: Contact) : VoiceCommand
    data object MedicineTaken : VoiceCommand
    data object OpenMedicines : VoiceCommand
    data object OpenSettings : VoiceCommand
    data class NeedsContact(val spokenName: String?) : VoiceCommand
    data class Unrecognized(val heard: String) : VoiceCommand
}

object CommandRouter {

    private val CALL = listOf(
        "call", "phone", "dial", "ring",
        "कॉल", "फोन", "फ़ोन", "बुलाओ",
    )
    private val WHATSAPP = listOf("whatsapp", "whats app", "व्हाट्सएप", "वॉट्सऐप")
    private val MEDICINE_TAKEN = listOf(
        "took", "taken", "done", "khaya", "खा लिया", "ले लिया", "खा ली",
    )
    private val MEDICINES = listOf("medicine", "medicines", "pill", "dawai", "दवा", "दवाई", "गोली")
    private val SETTINGS = listOf("settings", "setting", "सेटिंग", "सेटिंग्स")

    fun route(transcript: String, contacts: List<Contact>): VoiceCommand {
        val text = normalize(transcript)

        if (has(text, MEDICINE_TAKEN)) return VoiceCommand.MedicineTaken
        if (has(text, MEDICINES)) return VoiceCommand.OpenMedicines
        if (has(text, SETTINGS)) return VoiceCommand.OpenSettings

        val wantsWhatsApp = has(text, WHATSAPP)
        val wantsCall = wantsWhatsApp || has(text, CALL)
        if (wantsCall) {
            val contact = findContact(text, contacts)
                ?: return VoiceCommand.NeedsContact(spokenName = null)
            return if (wantsWhatsApp) VoiceCommand.WhatsApp(contact) else VoiceCommand.Call(contact)
        }

        return VoiceCommand.Unrecognized(transcript)
    }

    private fun normalize(s: String) = " " + s.lowercase().replace(Regex("[.,!?]"), " ").replace(Regex("\\s+"), " ") + " "

    private fun has(t: String, words: List<String>) = words.any { t.contains(it) }

    private fun findContact(t: String, contacts: List<Contact>): Contact? =
        contacts.filter { it.name.isNotBlank() && t.contains(it.name.lowercase()) }
            .maxByOrNull { it.name.length }
}
