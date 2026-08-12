package com.lifeos.app.feature.voice

import com.lifeos.app.core.memory.MemoryCategory

/**
 * A deterministic, keyword-based guess at what category a freely-spoken
 * "remember that ..." statement belongs to — deliberately not routed
 * through the AI provider. Personal-memory writes are exactly the kind of
 * action where a wrong guess is cheap to correct later (via [MemoryScreen])
 * but a *silently invented* category or, worse, a fabricated fact would
 * not be — see docs/adr/0001's sibling reasoning and the 2026-08-12 audit's
 * finding that the live AI system prompt has no safety guardrails. The
 * category guess is honest about being a heuristic: callers should treat
 * it as a starting point, correctable in [MemoryScreen], never as certain.
 */
object MemoryCategoryGuesser {

    private val healthWords = listOf(
        "medicine", "medicines", "doctor", "allerg", "pain", "diagnos", "blood pressure",
        "diabetes", "pill", "hospital", "surgery", "condition", "prescription", "dawai", "दवा",
    )
    private val relationshipWords = listOf(
        "daughter", "son", "wife", "husband", "brother", "sister", "friend", "grandchild",
        "grandson", "granddaughter", "family", "neighbor", "beta", "बेटा", "बेटी",
    )
    private val occasionWords = listOf(
        "birthday", "anniversary", "wedding", "festival", "diwali", "holiday", "celebration",
    )
    private val practicalSafetyWords = listOf(
        "address", "lives at", "emergency", "spare key", "locker code", "safe combination",
    )
    private val dailyRoutineWords = listOf(
        "every day", "every morning", "every evening", "usually", "routine", "walk", "prayer", "yoga",
    )
    private val interestWords = listOf(
        "favorite", "favourite", "like to", "enjoy", "hobby", "song", "movie", "book",
    )
    private val belongingWords = listOf(
        "keys", "glasses", "wallet", "important papers", "documents",
    )

    /** The best-guess category for [statement] — never a claim of certainty, always correctable. */
    fun guessCategory(statement: String): MemoryCategory {
        val text = statement.lowercase()
        return when {
            healthWords.any { text.contains(it) } -> MemoryCategory.HEALTH
            practicalSafetyWords.any { text.contains(it) } -> MemoryCategory.PRACTICAL_SAFETY
            relationshipWords.any { text.contains(it) } -> MemoryCategory.RELATIONSHIPS
            occasionWords.any { text.contains(it) } -> MemoryCategory.OCCASIONS
            dailyRoutineWords.any { text.contains(it) } -> MemoryCategory.DAILY_ROUTINE
            interestWords.any { text.contains(it) } -> MemoryCategory.INTERESTS
            belongingWords.any { text.contains(it) } -> MemoryCategory.BELONGINGS
            else -> MemoryCategory.LIFE_STORY
        }
    }

    /** A short, human-readable label for [statement] — a display title, not a claim about meaning. */
    fun guessLabel(statement: String): String {
        val words = statement.trim().split(Regex("\\s+"))
        return words.take(6).joinToString(" ").trimEnd('.', ',', '!', '?')
    }
}
