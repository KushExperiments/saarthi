package com.lifeos.app.core.ai

import com.lifeos.app.core.common.DispatcherProvider
import com.lifeos.app.core.common.Outcome
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AiProvider] backed by Google's Gemini `generateContent` REST API — one
 * endpoint for both transcription (inline audio) and understanding (text),
 * unlike Groq's separate Whisper/Llama endpoints. Failures return
 * [Outcome.Failure] instead of throwing, same contract as the provider this
 * replaced.
 *
 * NOTE: Gemini's documented inline-audio MIME types are wav/mp3/aiff/aac/
 * ogg/flac. Android's recorder produces AAC audio (typically in an .m4a
 * container), which is sent here as "audio/aac" — worth verifying against
 * a real device recording before relying on it, since [transcribe] isn't
 * wired to a live recording call site yet (voice input currently goes
 * through the on-device SpeechRecognizer in feature:voice; this method
 * exists for the [AiProvider] contract and is covered by its own test).
 */
@Singleton
class GeminiAiProvider @Inject constructor(
    private val apiKeyStore: AiApiKeyStore,
    private val client: OkHttpClient,
    private val dispatchers: DispatcherProvider,
    private val endpoints: GeminiEndpoints,
) : AiProvider {

    override suspend fun verifyKey(): Outcome<Unit> = withContext(dispatchers.io) {
        val key = apiKeyStore.getApiKey()
        if (key.isNullOrBlank()) return@withContext Outcome.Failure(NoApiKeyException())

        try {
            val request = Request.Builder().url(withKey(endpoints.modelsUrl, key))
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Outcome.Success(Unit)
                } else {
                    Outcome.Failure(IllegalStateException("Gemini key check failed: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Outcome.Failure(e)
        }
    }

    override suspend fun transcribe(audio: File): Outcome<String> = withContext(dispatchers.io) {
        val key = apiKeyStore.getApiKey()
        if (key.isNullOrBlank()) return@withContext Outcome.Failure(NoApiKeyException())
        if (!audio.exists()) return@withContext Outcome.Failure(IllegalArgumentException("Audio file does not exist"))

        try {
            val base64Audio = Base64.getEncoder().encodeToString(audio.readBytes())
            val payload = JSONObject().put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray()
                            .put(
                                JSONObject().put(
                                    "text",
                                    "Transcribe the spoken words in this audio exactly, in the original " +
                                        "language. Reply with ONLY the transcribed text, nothing else.",
                                ),
                            )
                            .put(
                                JSONObject().put(
                                    "inlineData",
                                    JSONObject().put("mimeType", "audio/aac").put("data", base64Audio),
                                ),
                            ),
                    ),
                ),
            )
            val request = Request.Builder().url(withKey(endpoints.generateContentUrl, key))
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string()
                    ?: return@withContext Outcome.Failure(IllegalStateException("Empty response body"))
                if (!response.isSuccessful) {
                    return@withContext Outcome.Failure(IllegalStateException("Gemini STT failed: HTTP ${response.code}"))
                }
                val text = firstCandidateText(raw).trim()
                if (text.isBlank()) Outcome.Failure(IllegalStateException("Empty transcription")) else Outcome.Success(text)
            }
        } catch (e: Exception) {
            Outcome.Failure(e)
        }
    }

    override suspend fun understand(request: UnderstandRequest): Outcome<AiIntent> = withContext(dispatchers.io) {
        val key = apiKeyStore.getApiKey()
        if (key.isNullOrBlank()) return@withContext Outcome.Failure(NoApiKeyException())
        if (request.transcript.isBlank()) return@withContext Outcome.Failure(IllegalArgumentException("Blank transcript"))

        try {
            val payload = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt(request)))))
                .put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put("role", "user")
                            .put("parts", JSONArray().put(JSONObject().put("text", request.transcript))),
                    ),
                )
                .put(
                    "generationConfig",
                    JSONObject().put("temperature", 0.2).put("responseMimeType", "application/json"),
                )
            val httpRequest = Request.Builder().url(withKey(endpoints.generateContentUrl, key))
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(httpRequest).execute().use { response ->
                val raw = response.body?.string()
                    ?: return@withContext Outcome.Failure(IllegalStateException("Empty response body"))
                if (!response.isSuccessful) {
                    return@withContext Outcome.Failure(IllegalStateException("Gemini chat failed: HTTP ${response.code}"))
                }
                val intent = JSONObject(stripFences(firstCandidateText(raw)))
                Outcome.Success(
                    AiIntent(
                        action = intent.optString("action", "none"),
                        person = intent.optString("person", ""),
                        query = intent.optString("query", ""),
                        message = intent.optString("message", ""),
                        reply = intent.optString("reply", ""),
                    ),
                )
            }
        } catch (e: Exception) {
            Outcome.Failure(e)
        }
    }

    private fun firstCandidateText(rawResponse: String): String =
        JSONObject(rawResponse).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
            .getString("text")

    private fun withKey(url: String, key: String): String =
        url + (if (url.contains("?")) "&" else "?") + "key=" + key

    private fun systemPrompt(request: UnderstandRequest): String = buildString {
        append("You are Juno, a kind voice helper for an elderly person. ")
        append("Read what the user said (it may be in any language) and decide ONE action. ")
        append("Known people you can contact: ")
        append(if (request.knownPeople.isEmpty()) "(none saved yet). " else request.knownPeople.joinToString(", ") + ". ")
        if (request.languageHint.isNotBlank()) {
            append("The user's preferred language is \"${request.languageHint}\" — reply in that language ")
            append("unless the user clearly spoke a different one. ")
        }
        append("Reply ONLY with a JSON object having keys: ")
        append("action, person, query, message, reply. ")
        append("action must be one of: call, whatsapp, sms, torch_on, torch_off, youtube, ")
        append("volume_up, volume_down, volume_max, medicine_taken, open_medicines, help, answer, none. ")
        append("If the user asks ANY question or wants to chat (maths, facts, general knowledge, ")
        append("jokes, advice), use action \"answer\" and put the full, correct, helpful answer in reply. ")
        append("person = the exact name from the known people list, or empty. ")
        append("query = search text for youtube. ")
        append("message = the message to send (only for whatsapp/sms), written politely in the user's language. ")
        append("reply = a warm answer or confirmation, in the SAME language the user spoke. ")
        append("Keep reply simple and slow-friendly for an elder. Do not add anything outside the JSON.")
    }

    /** Strips ```json fences some models add despite responseMimeType: application/json. */
    private fun stripFences(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("```")) {
            trimmed.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        } else {
            trimmed
        }
    }
}

class NoApiKeyException : Exception("No Gemini API key configured")
