package com.lifeos.app.core.ai

/** In-memory fake — no Android framework, no Keystore, fully deterministic. */
class FakeAiApiKeyStore(initialKey: String? = "test-key") : AiApiKeyStore {
    private var stored: String? = initialKey

    override fun getApiKey(): String? = stored

    override fun setApiKey(key: String) {
        stored = key
    }

    override fun clearApiKey() {
        stored = null
    }
}
