package com.lifeos.app.core.interaction

import javax.inject.Inject

/**
 * Interaction OS §8: "requires corroboration across >= 2 independent
 * signals before acting." A single detection (this transcript's word
 * choice alone) is never enough — it must be echoed by a second,
 * independent signal: here, the same emotion appearing in recent
 * conversation history too, not just this one utterance.
 */
class EmotionCorroborator @Inject constructor(
    private val detector: EmotionDetector,
) {
    fun corroborate(transcript: String, recentTranscripts: List<String>): EmotionSignal? {
        val primary = detector.detect(transcript) ?: return null
        val echoedInHistory = recentTranscripts.any { detector.detect(it) == primary }
        return if (echoedInHistory) primary else null
    }
}
