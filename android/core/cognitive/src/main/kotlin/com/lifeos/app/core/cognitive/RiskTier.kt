package com.lifeos.app.core.cognitive

/** Cognitive OS §9. */
enum class RiskTier {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

/**
 * A per-action default tier, elevated by live context — a static lookup
 * table alone can't express "calling family is Low at noon, Medium at 3am."
 */
object RiskAssessment {
    private val DEFAULT_TIER = mapOf(
        "call" to RiskTier.LOW,
        "whatsapp" to RiskTier.LOW,
        "sms" to RiskTier.LOW,
        "torch_on" to RiskTier.LOW,
        "torch_off" to RiskTier.LOW,
        "youtube" to RiskTier.LOW,
        "volume_up" to RiskTier.LOW,
        "volume_down" to RiskTier.LOW,
        "volume_max" to RiskTier.LOW,
        "medicine_taken" to RiskTier.MEDIUM,
        "open_medicines" to RiskTier.LOW,
        "open_settings" to RiskTier.LOW,
        "help" to RiskTier.LOW,
        "answer" to RiskTier.LOW,
        "needs_contact" to RiskTier.LOW,
        "none" to RiskTier.LOW,
    )

    fun baseTier(action: String): RiskTier = DEFAULT_TIER[action] ?: RiskTier.HIGH

    /**
     * Context-modulated elevation — never de-escalates below the base tier.
     * "help" is exempt from the blanket emergency escalation: it's the
     * escape hatch itself, so it must never be the thing that gets held up
     * for confirmation during an emergency.
     */
    fun elevate(base: RiskTier, action: String, context: DecisionContext): RiskTier {
        var tier = base
        if (context.isEmergency && action != "help") return RiskTier.CRITICAL
        if (context.isNightTime && action in setOf("call", "whatsapp", "sms")) {
            tier = maxOf(tier, RiskTier.MEDIUM)
        }
        if (action == "medicine_taken" && context.isNightTime) {
            tier = maxOf(tier, RiskTier.HIGH)
        }
        return tier
    }
}
