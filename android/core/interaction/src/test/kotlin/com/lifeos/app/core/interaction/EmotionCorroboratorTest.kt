package com.lifeos.app.core.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmotionCorroboratorTest {

    private val corroborator = EmotionCorroborator(TextHeuristicEmotionDetector())

    @Test
    fun `a single mention never fires — one signal alone is never enough`() {
        val result = corroborator.corroborate("I feel so lonely today", recentTranscripts = emptyList())

        assertNull(result)
    }

    @Test
    fun `the same emotion echoed in recent history corroborates and fires`() {
        val result = corroborator.corroborate(
            transcript = "nobody calls me anymore",
            recentTranscripts = listOf("I've been feeling so lonely lately"),
        )

        assertEquals(EmotionSignal.LONELINESS, result)
    }

    @Test
    fun `history containing a DIFFERENT emotion does not corroborate`() {
        val result = corroborator.corroborate(
            transcript = "I feel so lonely today",
            recentTranscripts = listOf("that made me so happy"),
        )

        assertNull(result)
    }

    @Test
    fun `no detectable emotion in the transcript at all short-circuits before checking history`() {
        val result = corroborator.corroborate(
            transcript = "what time is it",
            recentTranscripts = listOf("I feel so lonely today"),
        )

        assertNull(result)
    }
}
