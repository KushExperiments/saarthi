package com.lifeos.app.core.cognitive

enum class CandidateSource {
    RULE,
    LLM,
}

/**
 * One possible action, before validation/scoring picks (or rejects) it.
 * [confidenceHint] is the generator's own self-reported confidence — the
 * [Scorer] weighs it, never trusts it blindly.
 */
data class Candidate(
    val action: String,
    val params: Map<String, String> = emptyMap(),
    val source: CandidateSource,
    val confidenceHint: Float,
)
