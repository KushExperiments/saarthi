package com.lifeos.app.core.cognitive

/** A read-side, plain-data view of the most recent decision — for surfacing "why did I do that" in the UI. */
data class RecentDecision(
    val action: String,
    val confidence: Float,
    val contextSummary: String,
    val reasoningTypesFired: List<String>,
    val rejectedAlternatives: List<String>,
)

interface DecisionTraceReader {
    suspend fun mostRecent(): RecentDecision?
}
