package com.lifeos.app.core.ai

import com.lifeos.app.core.common.Outcome
import com.lifeos.app.core.testing.MainDispatcherRule
import com.lifeos.app.core.testing.TestDispatcherProvider
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class GeminiAiProviderTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private lateinit var apiKeyStore: FakeAiApiKeyStore
    private lateinit var provider: GeminiAiProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        apiKeyStore = FakeAiApiKeyStore()
        val endpoints = GeminiEndpoints(
            generateContentUrl = server.url("/models/gemini-2.0-flash:generateContent").toString(),
            modelsUrl = server.url("/models").toString(),
        )
        provider = GeminiAiProvider(apiKeyStore, OkHttpClient(), TestDispatcherProvider(), endpoints)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun candidateResponse(text: String): String {
        val escaped = org.json.JSONObject.quote(text)
        return """{"candidates":[{"content":{"parts":[{"text":$escaped}]}}]}"""
    }

    @Test
    fun `transcribe returns the recognized text on success`() = runTest {
        server.enqueue(MockResponse().setBody(candidateResponse("call beta")))
        val audio = File.createTempFile("lifeos_test", ".m4a").apply { writeText("fake audio") }

        val result = provider.transcribe(audio)

        assertEquals(Outcome.Success("call beta"), result)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertTrue(recorded.path?.contains("key=test-key") == true)
        assertTrue(recorded.body.readUtf8().contains("audio/aac"))
    }

    @Test
    fun `transcribe fails fast without a network call when no key is set`() = runTest {
        apiKeyStore.clearApiKey()
        val audio = File.createTempFile("lifeos_test", ".m4a").apply { writeText("fake audio") }

        val result = provider.transcribe(audio)

        assertTrue(result is Outcome.Failure && result.error is NoApiKeyException)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `understand strips markdown fences and parses the intent`() = runTest {
        val fenced = """
            ```json
            {"action":"call","person":"Beta","query":"","message":"","reply":"Calling Beta."}
            ```
        """.trimIndent()
        server.enqueue(MockResponse().setBody(candidateResponse(fenced)))

        val result = provider.understand(UnderstandRequest("call beta", listOf("Beta"), "en"))

        assertEquals(
            Outcome.Success(AiIntent(action = "call", person = "Beta", reply = "Calling Beta.")),
            result,
        )
        val recorded = server.takeRequest()
        val sentBody = recorded.body.readUtf8()
        assertTrue(sentBody.contains("systemInstruction"))
        assertTrue(sentBody.contains("\"responseMimeType\":\"application/json\""))
    }

    @Test
    fun `understand surfaces a Failure on a non-2xx response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("server error"))

        val result = provider.understand(UnderstandRequest("hello", emptyList(), "en"))

        assertTrue(result is Outcome.Failure)
    }

    @Test
    fun `verifyKey succeeds without spending a generateContent call`() = runTest {
        server.enqueue(MockResponse().setBody("""{"models":[]}"""))

        val result = provider.verifyKey()

        assertEquals(Outcome.Success(Unit), result)
        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertTrue(recorded.path?.startsWith("/models") == true)
        assertTrue(recorded.path?.contains("key=test-key") == true)
    }

    @Test
    fun `verifyKey surfaces a Failure on a non-2xx response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("invalid key"))

        val result = provider.verifyKey()

        assertTrue(result is Outcome.Failure)
    }

    @Test
    fun `verifyKey fails fast without a network call when no key is set`() = runTest {
        apiKeyStore.clearApiKey()

        val result = provider.verifyKey()

        assertTrue(result is Outcome.Failure && result.error is NoApiKeyException)
        assertEquals(0, server.requestCount)
    }
}
