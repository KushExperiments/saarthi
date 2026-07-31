package com.lifeos.app.core.cognitive

/**
 * Cognitive OS §11 — a fixed, non-runtime-alterable priority ordering.
 * There is deliberately no setter anywhere in this file: user autonomy
 * outranks caregiver convenience, truthfulness is never traded away, and
 * emergency override is narrow and never triggered by anything but
 * [DecisionContext.isEmergency].
 */
object EthicalPolicy {
    /** Actions that always require an ESCALATE verdict, never a bare APPROVE, regardless of Scorer confidence. */
    val ALWAYS_ESCALATE = setOf("sms", "whatsapp")

    /** Never true — no code path in this object can set this to true. Documents the hard constraint explicitly. */
    const val PERMITS_FABRICATED_MEDICAL_ADVICE = false
}
