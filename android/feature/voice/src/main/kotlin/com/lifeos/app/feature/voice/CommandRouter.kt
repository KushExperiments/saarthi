package com.lifeos.app.feature.voice

import com.lifeos.app.feature.contacts.Contact

/**
 * Deterministic keyword matching — the guaranteed floor tier from
 * Architecture §10/§11's fallback ladder, not an AI call. Pure logic, no
 * Android framework dependency, so it's trivially unit-testable.
 *
 * Memory intents (M-002) are handled here, deliberately, rather than
 * falling through to the AI/DecisionEngine path: the 2026-08-12 audit
 * found the live AI system prompt has no safety guardrails and no
 * uncertainty handling, which is exactly wrong for "remember a personal
 * fact" — a deterministic router either matches the phrase and stores
 * *exactly* what the user said, or doesn't match at all. It never
 * paraphrases, infers, or invents.
 */
sealed interface VoiceCommand {
    data class Call(val contact: Contact) : VoiceCommand
    data class WhatsApp(val contact: Contact) : VoiceCommand
    data object MedicineTaken : VoiceCommand
    data object OpenMedicines : VoiceCommand
    data object OpenSettings : VoiceCommand
    data class NeedsContact(val spokenName: String?) : VoiceCommand
    data class Remember(val statement: String) : VoiceCommand
    data class RecallAbout(val topic: String) : VoiceCommand
    data object ShowMemories : VoiceCommand
    data object ForgetLast : VoiceCommand
    data class ForgetAbout(val topic: String) : VoiceCommand
    data object WhyRemembered : VoiceCommand
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

    private val REMEMBER_TRIGGERS = listOf("remember that ", "please remember that ", "remember ")
    private val RECALL_ABOUT_TRIGGERS = listOf(
        "what do you remember about ", "do you remember ", "what do you know about ", "tell me about ",
    )
    // Deliberately just these two, bare — "what do you remember about X"
    // must go through RECALL_ABOUT_TRIGGERS' extraction below instead, or
    // this substring would shadow it for every topic, not just "about me".
    private val SHOW_MEMORIES = listOf("show me my memories", "show my memories", "show me my important memories")
    private val FORGET_ABOUT_TRIGGERS = listOf("forget about ", "forget what you know about ")
    private val FORGET_LAST = listOf("forget that", "forget it", "forget this")
    private val WHY_REMEMBERED = listOf(
        "why did you remember", "why do you remember", "why is that remembered",
    )

    fun route(transcript: String, contacts: List<Contact>): VoiceCommand {
        val text = normalize(transcript)

        if (has(text, MEDICINE_TAKEN)) return VoiceCommand.MedicineTaken
        if (has(text, MEDICINES)) return VoiceCommand.OpenMedicines
        if (has(text, SETTINGS)) return VoiceCommand.OpenSettings

        // Order matters: extraction must run before the bare SHOW_MEMORIES
        // check, or "what do you remember" (a substring of "what do you
        // remember about X") would shadow every topic, not just "about me".
        // Extraction reads from the *original* transcript (case-insensitive
        // match, but original casing preserved in what's returned) — proper
        // nouns like "Pune" shouldn't get stored lowercased just because
        // routing needed a normalized copy to detect the trigger phrase.
        extractAfter(transcript, RECALL_ABOUT_TRIGGERS)?.let { topic ->
            return if (topic.isBlank() || topic.equals("me", ignoreCase = true)) VoiceCommand.ShowMemories else VoiceCommand.RecallAbout(topic)
        }
        if (has(text, SHOW_MEMORIES)) return VoiceCommand.ShowMemories
        extractAfter(transcript, FORGET_ABOUT_TRIGGERS)?.let { topic ->
            return if (topic.isBlank()) VoiceCommand.ForgetLast else VoiceCommand.ForgetAbout(topic)
        }
        if (has(text, FORGET_LAST)) return VoiceCommand.ForgetLast
        if (has(text, WHY_REMEMBERED)) return VoiceCommand.WhyRemembered
        extractAfter(transcript, REMEMBER_TRIGGERS)?.let { statement ->
            if (statement.isNotBlank()) return VoiceCommand.Remember(statement)
        }

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

    /**
     * The text after the first matching trigger phrase in [triggers], with
     * the original casing preserved (only the *search* is case-insensitive
     * — matches against a lowercased copy, but the returned slice comes
     * from [original]). Plain `.lowercase()` doesn't change string length
     * for the alphabets this app handles, so the index found in the
     * lowercased copy maps directly back onto [original].
     */
    private fun extractAfter(original: String, triggers: List<String>): String? {
        val lower = original.lowercase()
        val trigger = triggers.firstOrNull { lower.contains(it) } ?: return null
        val index = lower.indexOf(trigger)
        return original.substring(index + trigger.length).trim().trimEnd('.', ',', '!', '?')
    }

    private fun findContact(t: String, contacts: List<Contact>): Contact? =
        contacts.filter { it.name.isNotBlank() && t.contains(it.name.lowercase()) }
            .maxByOrNull { it.name.length }
}
