package com.lifeos.app.core.interaction

import javax.inject.Inject

interface EmotionDetector {
    fun detect(transcript: String): EmotionSignal?
}

/**
 * v1 scope: text/word-choice signal only. Full prosody-from-raw-audio
 * analysis (Interaction OS §8's other cited signal sources) is a
 * genuinely heavy, separate ML undertaking — deferred, not skipped. A
 * text-only signal source is still a valid, honest partial implementation
 * given [EmotionCorroborator] never acts on a single signal alone.
 */
class TextHeuristicEmotionDetector @Inject constructor() : EmotionDetector {

    private val lexicon: Map<EmotionSignal, List<String>> = mapOf(
        EmotionSignal.JOY to listOf("happy", "great", "wonderful", "glad", "khush"),
        EmotionSignal.SADNESS to listOf("sad", "upset", "crying", "udaas"),
        EmotionSignal.ANGER to listOf("angry", "furious", "annoyed", "gussa"),
        EmotionSignal.FEAR to listOf("scared", "afraid", "worried", "dar"),
        EmotionSignal.CONFUSION to listOf("confused", "don't understand", "samajh nahi"),
        EmotionSignal.LONELINESS to listOf("lonely", "alone", "nobody calls", "akela"),
        EmotionSignal.PRIDE to listOf("proud", "so happy for", "garv"),
        EmotionSignal.FRUSTRATION to listOf("frustrated", "why won't", "not working"),
    )

    override fun detect(transcript: String): EmotionSignal? {
        val text = transcript.lowercase()
        return lexicon.entries.firstOrNull { (_, words) -> words.any { text.contains(it) } }?.key
    }
}
