package com.lifeos.app.core.cognitive

import javax.inject.Inject

/**
 * Deterministic keyword matching — the guaranteed floor tier, generalized
 * from feature:voice's CommandRouter but living here (core:cognitive can't
 * depend on feature:voice/feature:contacts — dependencies flow one
 * direction) so it's usable without an AI call from any caller. Produces
 * generic [Candidate]s over a plain `knownPeople: List<String>`, not a
 * `Contact` type — the concrete feature-level mapping happens at the call
 * site (feature:voice's DialogueManager, a later phase).
 */
class RuleBasedCandidateGenerator @Inject constructor() : CandidateGenerator {

    private val callWords = listOf("call", "phone", "dial", "ring", "कॉल", "फोन", "फ़ोन", "बुलाओ")
    private val whatsappWords = listOf("whatsapp", "whats app", "व्हाट्सएप", "वॉट्सऐप")
    private val medicineTakenWords = listOf("took", "taken", "done", "khaya", "खा लिया", "ले लिया", "खा ली")
    private val medicinesWords = listOf("medicine", "medicines", "pill", "dawai", "दवा", "दवाई", "गोली")
    private val settingsWords = listOf("settings", "setting", "सेटिंग", "सेटिंग्स")
    private val helpWords = listOf("help", "emergency", "मदद")

    override suspend fun generate(input: DecisionInput): List<Candidate> {
        val text = normalize(input.transcript)

        if (has(text, helpWords)) return listOf(rule("help", confidence = 0.9f))
        if (has(text, medicineTakenWords)) return listOf(rule("medicine_taken", confidence = 0.9f))
        if (has(text, medicinesWords)) return listOf(rule("open_medicines", confidence = 0.85f))
        if (has(text, settingsWords)) return listOf(rule("open_settings", confidence = 0.85f))

        val wantsWhatsApp = has(text, whatsappWords)
        val wantsCall = wantsWhatsApp || has(text, callWords)
        if (wantsCall) {
            val person = input.knownPeople.filter { text.contains(it.lowercase()) }.maxByOrNull { it.length }
            return if (person != null) {
                listOf(rule(if (wantsWhatsApp) "whatsapp" else "call", confidence = 0.85f, params = mapOf("person" to person)))
            } else {
                listOf(rule("needs_contact", confidence = 0.6f))
            }
        }

        return emptyList()
    }

    private fun rule(action: String, confidence: Float, params: Map<String, String> = emptyMap()) =
        Candidate(action = action, params = params, source = CandidateSource.RULE, confidenceHint = confidence)

    private fun normalize(s: String) =
        " " + s.lowercase().replace(Regex("[.,!?]"), " ").replace(Regex("\\s+"), " ") + " "

    private fun has(t: String, words: List<String>) = words.any { t.contains(it) }
}
