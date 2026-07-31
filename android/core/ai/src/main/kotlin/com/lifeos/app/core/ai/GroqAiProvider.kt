package com.lifeos.app.core.ai

import com.lifeos.app.core.common.DispatcherProvider
import com.lifeos.app.core.common.Outcome
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val STT_MODEL = "whisper-large-v3"
private const val CHAT_MODEL = "llama-3.3-70b-versatile"

/**
 * Direct port of the legacy prototype's `com.lifeos.app.Ai.Groq` object
 * (app/src/main/java/com/lifeos/app/Ai.kt) behind the [AiProvider] seam.
 * Whisper (whisper-large-v3) transcribes; Llama 3.3 70B understands intent.
 * Failures return [Outcome.Failure] instead of the legacy code's silent
 * `null`, so callers can't accidentally swallow "no key" vs. "network down"
 * vs. "bad response" as the same case.
 */
@Singleton
class GroqAiProvider @Inject constructor(
    private val apiKeyStore: AiApiKeyStore,
    private val client: OkHttpClient,
    private val dispatchers: DispatcherProvider,
    private val endpoints: GroqEndpoints,
) : AiProvider {

    override suspend fun verifyKey(): Outcome<Unit> = withContext(dispatchers.io) {
        val key = apiKeyStore.getApiKey()
        if (key.isNullOrBlank()) return@withContext Outcome.Failure(NoApiKeyException())

        try {
            val request = Request.Builder().url(endpoints.modelsUrl)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Outcome.Success(Unit)
                } else {
                    Outcome.Failure(IllegalStateException("Groq key check failed: HTTP ${response.code}"))
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
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("model", STT_MODEL)
                .addFormDataPart("response_format", "json")
                .addFormDataPart("file", audio.name, audio.asRequestBody("audio/m4a".toMediaType()))
                .build()
            val request = Request.Builder().url(endpoints.sttUrl)
                .addHeader("Authorization", "Bearer $key")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string()
                    ?: return@withContext Outcome.Failure(IllegalStateException("Empty response body"))
                if (!response.isSuccessful) {
                    return@withContext Outcome.Failure(IllegalStateException("Groq STT failed: HTTP ${response.code}"))
                }
                val text = JSONObject(raw).optString("text").trim()
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
                .put("model", CHAT_MODEL)
                .put("temperature", 0.2)
                .put("response_format", JSONObject().put("type", "json_object"))
                .put(
                    "messages",
                    JSONArray()
                        .put(JSONObject().put("role", "system").put("content", systemPrompt(request)))
                        .put(JSONObject().put("role", "user").put("content", request.transcript)),
                )
            val httpRequest = Request.Builder().url(endpoints.chatUrl)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(httpRequest).execute().use { response ->
                val raw = response.body?.string()
                    ?: return@withContext Outcome.Failure(IllegalStateException("Empty response body"))
                if (!response.isSuccessful) {
                    return@withContext Outcome.Failure(IllegalStateException("Groq chat failed: HTTP ${response.code}"))
                }
                val content = JSONObject(raw).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content")
                val intent = JSONObject(stripFences(content))
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

    private fun systemPrompt(request: UnderstandRequest): String = buildString {
        append("You are LifeOS, a kind voice helper for an elderly person. ")
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

    /** Strips ```json fences some models add despite response_format: json_object. */
    private fun stripFences(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("```")) {
            trimmed.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        } else {
            trimmed
        }
    }
}

class NoApiKeyException : Exception("No Groq API key configured")
