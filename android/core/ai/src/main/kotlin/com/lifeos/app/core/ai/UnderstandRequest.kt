package com.lifeos.app.core.ai

/**
 * [conversationContext] is an unused seam today — core:interaction's
 * Dialogue Manager (a later phase) will populate it so the LLM can see
 * recent turns, not just the latest transcript.
 */
data class UnderstandRequest(
    val transcript: String,
    val knownPeople: List<String>,
    val languageHint: String,
    val conversationContext: List<String> = emptyList(),
)
