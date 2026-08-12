package com.lifeos.app.feature.voice

import com.lifeos.app.core.memory.MemoryCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryCategoryGuesserTest {

    @Test
    fun `health words guess HEALTH`() {
        assertEquals(MemoryCategory.HEALTH, MemoryCategoryGuesser.guessCategory("I'm allergic to penicillin"))
        assertEquals(MemoryCategory.HEALTH, MemoryCategoryGuesser.guessCategory("my blood pressure medicine is in the kitchen"))
    }

    @Test
    fun `relationship words guess RELATIONSHIPS`() {
        assertEquals(MemoryCategory.RELATIONSHIPS, MemoryCategoryGuesser.guessCategory("my daughter lives in Pune"))
    }

    @Test
    fun `occasion words guess OCCASIONS`() {
        assertEquals(MemoryCategory.OCCASIONS, MemoryCategoryGuesser.guessCategory("Priya's birthday is in March"))
    }

    @Test
    fun `practical safety words win over a relationship word in the same sentence`() {
        // "spare key" is a safety concern even though "neighbor" is also present —
        // category priority checks safety before relationships on purpose.
        assertEquals(
            MemoryCategory.PRACTICAL_SAFETY,
            MemoryCategoryGuesser.guessCategory("the spare key is with my neighbor"),
        )
    }

    @Test
    fun `unmatched statements default to LIFE_STORY, never an unrelated guess`() {
        assertEquals(MemoryCategory.LIFE_STORY, MemoryCategoryGuesser.guessCategory("I worked as a teacher for twenty years"))
    }

    @Test
    fun `label is a short leading snippet, not the whole statement`() {
        val label = MemoryCategoryGuesser.guessLabel("my daughter lives in Pune with her husband")
        assertEquals("my daughter lives in Pune with", label)
    }

    @Test
    fun `label trims trailing punctuation`() {
        assertEquals("hello there", MemoryCategoryGuesser.guessLabel("hello there."))
    }
}
