package com.lifeos.app.core.cognitive

/**
 * Cognitive OS §9's Risk Assessment needs live context to modulate a risk
 * tier ("call daughter" Low -> Medium at 3am) — a static per-action lookup
 * table alone isn't enough.
 */
data class DecisionContext(
    val now: Long,
    val isNightTime: Boolean = false,
    val familyPresent: Boolean = false,
    val isEmergency: Boolean = false,
)

/** What the Decision Engine is being asked to decide about. */
data class DecisionInput(
    val transcript: String,
    val knownPeople: List<String> = emptyList(),
    val languageHint: String = "en",
    val context: DecisionContext,
)
