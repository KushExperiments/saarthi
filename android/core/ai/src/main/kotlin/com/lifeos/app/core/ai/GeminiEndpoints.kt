package com.lifeos.app.core.ai

/**
 * Constructor-injected instead of hardcoded inside [GeminiAiProvider] so tests
 * can point at a MockWebServer without a mutable var on a @Singleton
 * instance — production gets [AiModule]'s default-valued binding, tests
 * construct their own [GeminiEndpoints] directly (they bypass Hilt entirely
 * already, see GeminiAiProviderTest).
 *
 * Gemini's REST API takes one endpoint (generateContent) for both
 * transcription and understanding — the difference is only the request
 * body's `parts` (inline audio vs. plain text) — plus a `models` list
 * endpoint used as the cheap auth/connectivity check for [verifyKey].
 * The API key travels as a `?key=` query param, not a Bearer header.
 */
data class GeminiEndpoints(
    val generateContentUrl: String =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
    val modelsUrl: String = "https://generativelanguage.googleapis.com/v1beta/models",
)
