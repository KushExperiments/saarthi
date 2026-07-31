package com.lifeos.app.core.cognitive

/**
 * Cognitive OS §5 — hardcoded, not user-tunable. Ordinal order IS the
 * priority order: Safety beats Health beats Emotional wellbeing beats
 * Social beats Convenience, always.
 */
enum class Goal {
    SAFETY,
    HEALTH,
    EMOTIONAL,
    SOCIAL,
    CONVENIENCE,
}

object GoalHierarchy {
    /** Higher is more important. */
    fun weightOf(goal: Goal): Float = when (goal) {
        Goal.SAFETY -> 1.0f
        Goal.HEALTH -> 0.8f
        Goal.EMOTIONAL -> 0.6f
        Goal.SOCIAL -> 0.4f
        Goal.CONVENIENCE -> 0.2f
    }

    fun goalFor(action: String): Goal = when (action) {
        "medicine_taken", "open_medicines" -> Goal.HEALTH
        "call", "whatsapp", "sms" -> Goal.SOCIAL
        "help" -> Goal.SAFETY
        "answer" -> Goal.EMOTIONAL
        else -> Goal.CONVENIENCE
    }
}
