package com.lifeos.app.core.ai

interface AiApiKeyStore {
    fun getApiKey(): String?

    fun setApiKey(key: String)

    fun clearApiKey()
}
