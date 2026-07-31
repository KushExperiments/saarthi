package com.lifeos.app.core.memory

import com.lifeos.app.core.data.MemoryNodeEntity
import javax.inject.Inject
import kotlin.math.max

private const val MILLIS_PER_DAY = 86_400_000L

private const val WEIGHT_SEMANTIC = 0.3f
private const val WEIGHT_CONFIDENCE = 0.3f
private const val WEIGHT_RECENCY = 0.2f
private const val WEIGHT_CONTEXT = 0.2f

data class ScoredMemory(
    val node: MemoryNodeEntity,
    val score: Float,
    val breakdown: Map<String, Float>,
)

/**
 * Memory §8's multi-signal ranking — semantic relevance ([Embedder]) is one
 * weighted input among several, never the sole determinant. A node with
 * high text similarity but low confidence and stale/out-of-context can
 * still rank below a more relevant, fresher, confirmed one.
 *
 * Scoped to [MemoryNodeEntity] candidates for v1 — Memory §8 also mentions
 * Life Timeline "significance" as a signal; that naturally extends this
 * engine once Life Timeline retrieval is unified with Knowledge Graph
 * retrieval, not silently dropped.
 */
class RetrievalEngine @Inject constructor(
    private val embedder: Embedder,
) {
    fun rank(
        query: String,
        candidates: List<MemoryNodeEntity>,
        categoryHint: MemoryCategory? = null,
        now: Long,
    ): List<ScoredMemory> = candidates.map { node ->
        val semantic = embedder.similarity(query, "${node.label} ${node.valueText}")
        val confidence = node.confidence
        val recency = recencyScore(node.updatedAt, now)
        val context = if (categoryHint != null && node.category == categoryHint.name) 1f else 0.3f

        val score = WEIGHT_SEMANTIC * semantic +
            WEIGHT_CONFIDENCE * confidence +
            WEIGHT_RECENCY * recency +
            WEIGHT_CONTEXT * context

        ScoredMemory(
            node = node,
            score = score,
            breakdown = mapOf(
                "semantic" to semantic,
                "confidence" to confidence,
                "recency" to recency,
                "context" to context,
            ),
        )
    }.sortedByDescending { it.score }

    private fun recencyScore(updatedAt: Long, now: Long): Float {
        val ageDays = max(0L, (now - updatedAt) / MILLIS_PER_DAY)
        return 1f / (1f + ageDays.toFloat())
    }
}
