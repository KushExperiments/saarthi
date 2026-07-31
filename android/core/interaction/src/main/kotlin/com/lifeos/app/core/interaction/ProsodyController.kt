package com.lifeos.app.core.interaction

import javax.inject.Inject

/**
 * Interaction OS §17's breath-pause insertion, done via punctuation, not
 * SSML — OEM TextToSpeech engines' SSML support is inconsistent across
 * devices, but every engine already respects comma/period pacing (this is
 * exactly what [VoiceEngine] already relies on), so this stays reliable
 * without adding a markup dependency.
 */
class ProsodyController @Inject constructor() {

    fun insertBreathPauses(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return trimmed

        val sentences = trimmed.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        // Every sentence gets its own terminal punctuation — that's the actual
        // pause opportunity most TTS engines honor; an un-terminated fragment
        // (common when text is composed by string concatenation elsewhere)
        // reads as one run-on breath instead of two calm sentences.
        return sentences.joinToString(" ") { sentence ->
            if (sentence.last() in TERMINATORS) sentence else "$sentence."
        }
    }

    private companion object {
        val TERMINATORS = setOf('.', '!', '?')
    }
}
