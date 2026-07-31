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

class GroqAiProviderTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var server: MockWebServer
    private lateinit var apiKeyStore: FakeAiApiKeyStore
    private lateinit var provider: GroqAiProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        apiKeyStore = FakeAiApiKeyStore()
        val endpoints = GroqEndpoints(
            sttUrl = server.url("/audio/transcriptions").toString(),
            chatUrl = server.url("/chat/completions").toString(),
            modelsUrl = server.url("/models").toString(),
        )
        provider = GroqAiProvider(apiKeyStore, OkHttpClient(), TestDispatcherProvider(), endpoints)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `transcribe returns the recognized text on success`() = runTest {
        server.enqueue(MockResponse().setBody("""{"text":"call beta"}"""))
        val audio = File.createTempFile("lifeos_test", ".m4a").apply { writeText("fake audio") }

        val result = provider.transcribe(audio)

        assertEquals(Outcome.Success("call beta"), result)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertTrue(recorded.getHeader("Authorization")?.startsWith("Bearer ") == true)
        assertTrue(recorded.body.readUtf8().contains("whisper-large-v3"))
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
        server.enqueue(
            MockResponse().setBody(
                """{"choices":[{"message":{"content":${org.json.JSONObject.quote(fenced)}}}]}""",
            ),
        )

        val result = provider.understand(UnderstandRequest("call beta", listOf("Beta"), "en"))

        assertEquals(
            Outcome.Success(AiIntent(action = "call", person = "Beta", reply = "Calling Beta.")),
            result,
        )
        val recorded = server.takeRequest()
        val sentBody = recorded.body.readUtf8()
        assertTrue(sentBody.contains("llama-3.3-70b-versatile"))
        assertTrue(sentBody.contains("\"type\":\"json_object\""))
    }

    @Test
    fun `understand surfaces a Failure on a non-2xx response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("server error"))

        val result = provider.understand(UnderstandRequest("hello", emptyList(), "en"))

        assertTrue(result is Outcome.Failure)
    }

    @Test
    fun `verifyKey succeeds without spending a chat completion`() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":[]}"""))

        val result = provider.verifyKey()

        assertEquals(Outcome.Success(Unit), result)
        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/models", recorded.path)
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
