package com.lifeos.app.core.cognitive

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cognitive OS §7 — runs the rule-based and LLM candidate generators in
 * parallel, drops anything [SafetyValidator] rejects, hands what survives
 * to the pure [Scorer], and logs the full trace. The LLM never gets a
 * shortcut past validation/scoring just because it's the more "confident"
 * source — that's the load-bearing safety property of this whole class.
 */
@Singleton
class DecisionEngine @Inject constructor(
    @RuleGenerator private val ruleGenerator: CandidateGenerator,
    @LlmGenerator private val llmGenerator: CandidateGenerator,
    private val safetyValidator: SafetyValidator,
    private val traceLogger: DecisionTraceLogger,
) {
    suspend fun decide(input: DecisionInput): ActionPlan {
        val candidates = coroutineScope {
            val rule = async { ruleGenerator.generate(input) }
            val llm = async { llmGenerator.generate(input) }
            rule.await() + llm.await()
        }

        val validations = candidates.associateWith { safetyValidator.validate(it, input.context) }
        val rejected = candidates.filter { validations.getValue(it).verdict == Verdict.REJECT }
        val survivors = candidates.filter { validations.getValue(it).verdict != Verdict.REJECT }
        val ranked = Scorer.rank(survivors, input.context)
        val winner = ranked.firstOrNull()
        val runnersUp = ranked.drop(1).map { it.candidate }

        val plan = if (winner == null) {
            ActionPlan(
                action = "none",
                params = emptyMap(),
                confidence = 0f,
                verdict = Verdict.REJECT,
                explanation = "No safe, allowed candidate action was found for this input.",
                alternatives = emptyList(),
            )
        } else {
            val verdict = validations.getValue(winner.candidate)
            ActionPlan(
                action = winner.candidate.action,
                params = winner.candidate.params,
                confidence = winner.score.coerceIn(0f, 1f),
                verdict = verdict.verdict,
                explanation = "Chose \"${winner.candidate.action}\" " +
                    "(source=${winner.candidate.source}, risk=${winner.riskTier}): ${verdict.reason}",
                alternatives = runnersUp,
            )
        }

        traceLogger.log(
            timestamp = input.context.now,
            reasoningTypesFired = candidates.map { it.source.name }.distinct(),
            contextSummary = "night=${input.context.isNightTime} " +
                "familyPresent=${input.context.familyPresent} emergency=${input.context.isEmergency}",
            memoryProvenanceIds = emptyList(),
            rejectedAlternatives = rejected,
            chosenAction = plan.action,
            chosenConfidence = plan.confidence,
        )

        return plan
    }
}
