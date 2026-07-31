package com.lifeos.app.core.ai

/**
 * Constructor-injected instead of hardcoded inside [GroqAiProvider] so tests
 * can point at a MockWebServer without a mutable var on a @Singleton
 * instance — production gets [AiModule]'s default-valued binding, tests
 * construct their own [GroqEndpoints] directly (they bypass Hilt entirely
 * already, see GroqAiProviderTest).
 */
data class GroqEndpoints(
    val sttUrl: String = "https://api.groq.com/openai/v1/audio/transcriptions",
    val chatUrl: String = "https://api.groq.com/openai/v1/chat/completions",
    val modelsUrl: String = "https://api.groq.com/openai/v1/models",
)
