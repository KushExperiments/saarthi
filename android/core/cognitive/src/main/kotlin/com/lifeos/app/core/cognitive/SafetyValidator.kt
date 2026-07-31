package com.lifeos.app.core.cognitive

import javax.inject.Inject

enum class Verdict {
    APPROVE,
    ESCALATE,
    REJECT,
}

data class StageResult(val verdict: Verdict, val reason: String)

interface ValidationStage {
    val name: String
    fun validate(candidate: Candidate, context: DecisionContext): StageResult
}

private object SchemaStage : ValidationStage {
    override val name = "Schema"
    override fun validate(candidate: Candidate, context: DecisionContext): StageResult =
        if (candidate.action.isBlank()) {
            StageResult(Verdict.REJECT, "blank action")
        } else {
            StageResult(Verdict.APPROVE, "well-formed")
        }
}

private object PolicyAllowlistStage : ValidationStage {
    override val name = "PolicyAllowlist"
    override fun validate(candidate: Candidate, context: DecisionContext): StageResult =
        if (PolicyAllowlist.isAllowed(candidate.action)) {
            StageResult(Verdict.APPROVE, "on allowlist")
        } else {
            StageResult(Verdict.REJECT, "\"${candidate.action}\" is not an allowed action")
        }
}

private object RiskTierStage : ValidationStage {
    override val name = "RiskTier"
    override fun validate(candidate: Candidate, context: DecisionContext): StageResult {
        val tier = RiskAssessment.elevate(RiskAssessment.baseTier(candidate.action), candidate.action, context)
        // HIGH, not just CRITICAL, requires confirmation (Cognitive OS §9's own
        // tier table) — e.g. confirming medicine was taken in the middle of the night.
        return if (tier >= RiskTier.HIGH) {
            StageResult(Verdict.ESCALATE, "risk tier elevated to $tier")
        } else {
            StageResult(Verdict.APPROVE, "risk tier $tier")
        }
    }
}

private object ContextAppropriatenessStage : ValidationStage {
    override val name = "ContextAppropriateness"
    override fun validate(candidate: Candidate, context: DecisionContext): StageResult = when {
        context.isEmergency && candidate.action != "help" ->
            StageResult(Verdict.ESCALATE, "non-emergency action requested during an active emergency context")
        // Cognitive OS §8's own worked example: a late-night call/message is
        // context-inappropriate even though its risk tier alone only reaches MEDIUM.
        context.isNightTime && candidate.action in setOf("call", "whatsapp", "sms") ->
            StageResult(Verdict.ESCALATE, "\"${candidate.action}\" requested late at night")
        else -> StageResult(Verdict.APPROVE, "context-appropriate")
    }
}

private object HumanConfirmationRequirementStage : ValidationStage {
    override val name = "HumanConfirmationRequirement"
    override fun validate(candidate: Candidate, context: DecisionContext): StageResult =
        if (candidate.action in EthicalPolicy.ALWAYS_ESCALATE) {
            StageResult(Verdict.ESCALATE, "\"${candidate.action}\" always requires human confirmation (Ethical Policy)")
        } else {
            StageResult(Verdict.APPROVE, "no confirmation required")
        }
}

/**
 * Cognitive OS §8's staged pipeline. A REJECT from any stage short-circuits
 * immediately (fail closed); an ESCALATE is remembered but later stages
 * still run, since a later REJECT must still be able to override it.
 */
class SafetyValidator @Inject constructor() {
    private val stages: List<ValidationStage> = listOf(
        SchemaStage,
        PolicyAllowlistStage,
        RiskTierStage,
        ContextAppropriatenessStage,
        HumanConfirmationRequirementStage,
    )

    fun validate(candidate: Candidate, context: DecisionContext): StageResult {
        var escalation: StageResult? = null
        for (stage in stages) {
            val result = stage.validate(candidate, context)
            when (result.verdict) {
                Verdict.REJECT -> return result
                Verdict.ESCALATE -> escalation = escalation ?: result
                Verdict.APPROVE -> Unit
            }
        }
        return escalation ?: StageResult(Verdict.APPROVE, "passed all stages")
    }
}
