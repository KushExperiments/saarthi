package com.lifeos.app.core.cognitive

/**
 * Cognitive OS §14 — bounded tunables, never model retraining. Each has an
 * explicit adaptable range with a hard floor: [NagInterval] can never adapt
 * down to "stop reminding," that's a floor enforced at construction, not a
 * suggestion.
 */
data class NagInterval(val minutes: Int) {
    init {
        require(minutes in MIN_MINUTES..MAX_MINUTES) {
            "Nag interval must be between $MIN_MINUTES and $MAX_MINUTES minutes, got $minutes"
        }
    }

    companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 5
        val DEFAULT = NagInterval(2)

        /** Clamps rather than throws — for callers reading a possibly-stale/corrupted stored value. */
        fun coerce(minutes: Int): NagInterval = NagInterval(minutes.coerceIn(MIN_MINUTES, MAX_MINUTES))
    }
}
