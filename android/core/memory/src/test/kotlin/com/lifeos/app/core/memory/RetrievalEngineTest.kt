package com.lifeos.app.core.memory

import com.lifeos.app.core.data.MemoryNodeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY = 86_400_000L

/** Always returns a fixed similarity regardless of text, so tests control the semantic signal precisely. */
private class FixedEmbedder(private val value: Float) : Embedder {
    override fun similarity(query: String, candidate: String): Float = value
}

class RetrievalEngineTest {

    private fun node(id: String, confidence: Float, updatedAt: Long, category: MemoryCategory) = MemoryNodeEntity(
        id = id,
        category = category.name,
        label = "label-$id",
        valueText = "value-$id",
        sensitivityTier = category.sensitivityTier.name,
        confidence = confidence,
        source = MemorySource.USER_STATED.name,
        createdAt = updatedAt,
        updatedAt = updatedAt,
        validFrom = updatedAt,
    )

    @Test
    fun `high semantic similarity alone does not guarantee first place`() {
        val engine = RetrievalEngine(FixedEmbedder(1f))
        val now = 100 * DAY

        // Same (maxed) semantic score for both — but the "stale, low confidence" one should lose.
        val fresh = node("fresh", confidence = 0.95f, updatedAt = now, category = MemoryCategory.HEALTH)
        val stale = node("stale", confidence = 0.1f, updatedAt = 0L, category = MemoryCategory.INTERESTS)

        val ranked = engine.rank("query", listOf(fresh, stale), categoryHint = MemoryCategory.HEALTH, now = now)

        assertEquals("fresh", ranked.first().node.id)
        assertEquals("stale", ranked.last().node.id)
    }

    @Test
    fun `a lower semantic match can still outrank a higher one on confidence, recency, and context together`() {
        val now = 100 * DAY
        val highSemanticButWeak = node("weak", confidence = 0.1f, updatedAt = 0L, category = MemoryCategory.INTERESTS)
        val lowerSemanticButStrong = node("strong", confidence = 0.95f, updatedAt = now, category = MemoryCategory.HEALTH)

        // Rank each candidate through an engine tuned to its own semantic score,
        // proving semantic similarity alone isn't decisive in either direction.
        val weakScore = RetrievalEngine(FixedEmbedder(1f))
            .rank("q", listOf(highSemanticButWeak), categoryHint = MemoryCategory.HEALTH, now = now).first().score
        val strongScore = RetrievalEngine(FixedEmbedder(0f))
            .rank("q", listOf(lowerSemanticButStrong), categoryHint = MemoryCategory.HEALTH, now = now).first().score

        assertTrue(
            "a fresh, confirmed, in-context memory should beat a stale, low-confidence one even with zero semantic overlap",
            strongScore > weakScore,
        )
    }

    @Test
    fun `breakdown exposes every signal for explainability`() {
        val engine = RetrievalEngine(FixedEmbedder(0.5f))
        val n = node("n", confidence = 0.8f, updatedAt = 0L, category = MemoryCategory.IDENTITY)

        val result = engine.rank("q", listOf(n), categoryHint = MemoryCategory.IDENTITY, now = 0L).first()

        assertEquals(setOf("semantic", "confidence", "recency", "context"), result.breakdown.keys)
    }
}
