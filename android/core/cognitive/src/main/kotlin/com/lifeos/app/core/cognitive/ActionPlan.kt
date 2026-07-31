package com.lifeos.app.core.cognitive

/**
 * [verdict] is what a caller must branch on, not [explanation] — the
 * explanation is human-readable audit text, never the thing that gates
 * whether an action executes unattended. A caller that executes [action]
 * without checking `verdict == Verdict.APPROVE` first is the exact bug
 * this field exists to make hard to write by accident.
 */
data class ActionPlan(
    val action: String,
    val params: Map<String, String>,
    val confidence: Float,
    val verdict: Verdict,
    val explanation: String,
    val alternatives: List<Candidate>,
)
