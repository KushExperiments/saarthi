package com.lifeos.app.core.cognitive

enum class UncertaintySource {
    UNKNOWN_FACT,
    CONFLICTING_MEMORY,
    INCOMPLETE_CONTEXT,
    SPEECH_AMBIGUITY,
    MEDICAL_UNCERTAINTY,
}

enum class ResponseStrategy {
    CLARIFY,
    PROCEED_WITH_CAVEAT,
    DEFER,
}

/**
 * Cognitive OS §10 — a pure lookup table, not a model call. Medical
 * uncertainty always defers (never guesses at a health-related action);
 * everything else maps to the doc's stated default strategy.
 */
object UncertaintyEngine {
    fun strategyFor(source: UncertaintySource): ResponseStrategy = when (source) {
        UncertaintySource.UNKNOWN_FACT -> ResponseStrategy.CLARIFY
        UncertaintySource.CONFLICTING_MEMORY -> ResponseStrategy.CLARIFY
        UncertaintySource.INCOMPLETE_CONTEXT -> ResponseStrategy.PROCEED_WITH_CAVEAT
        UncertaintySource.SPEECH_AMBIGUITY -> ResponseStrategy.CLARIFY
        UncertaintySource.MEDICAL_UNCERTAINTY -> ResponseStrategy.DEFER
    }
}
