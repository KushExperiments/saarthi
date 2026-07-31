package com.lifeos.app.core.cognitive

data class ScoredCandidate(
    val candidate: Candidate,
    val score: Float,
    val riskTier: RiskTier,
)

/**
 * Cognitive OS §7's most important piece: a PURE, deterministic function —
 * no coroutines, no I/O, no dependency on [com.lifeos.app.core.ai.AiProvider].
 * This is where "the LLM is a candidate generator only, never the final
 * arbiter" actually lives — an LLM-sourced candidate gets exactly the same
 * risk/goal/policy scrutiny as a rule-based one, no source-based trust
 * shortcut. Mirrors ReminderScheduler's own stated principle: never an
 * AI/runtime judgment call about the outcome that actually fires.
 */
object Scorer {
    private const val WEIGHT_CONFIDENCE = 0.5f
    private const val WEIGHT_GOAL = 0.3f
    private const val RULE_DETERMINISM_BONUS = 0.05f

    private fun riskPenalty(tier: RiskTier): Float = when (tier) {
        RiskTier.LOW -> 0f
        RiskTier.MEDIUM -> 0.1f
        RiskTier.HIGH -> 0.3f
        RiskTier.CRITICAL -> 0.6f
    }

    /** Highest score first. Candidates failing the policy allowlist are dropped entirely, never scored. */
    fun rank(candidates: List<Candidate>, context: DecisionContext): List<ScoredCandidate> = candidates
        .filter { PolicyAllowlist.isAllowed(it.action) }
        .map { candidate ->
            val riskTier = RiskAssessment.elevate(RiskAssessment.baseTier(candidate.action), candidate.action, context)
            val goalWeight = GoalHierarchy.weightOf(GoalHierarchy.goalFor(candidate.action))
            val determinismBonus = if (candidate.source == CandidateSource.RULE) RULE_DETERMINISM_BONUS else 0f
            val score = candidate.confidenceHint * WEIGHT_CONFIDENCE +
                goalWeight * WEIGHT_GOAL +
                determinismBonus -
                riskPenalty(riskTier)
            ScoredCandidate(candidate, score, riskTier)
        }
        .sortedByDescending { it.score }
}
