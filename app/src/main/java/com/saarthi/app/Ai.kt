package com.saarthi.app

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** What the AI understood the elder wants. */
data class AiIntent(
    val action: String,   // call, whatsapp, sms, torch_on, torch_off, youtube,
                          // volume_up, volume_down, volume_max, medicine_taken,
                          // open_medicines, help, none
    val person: String = "",
    val query: String = "",
    val message: String = "",
    val reply: String = ""
)

/**
 * Free AI brain, powered by Groq.
 *  - Whisper (whisper-large-v3) turns speech in ANY language into text
 *  - Llama 3.3 70B understands the meaning and replies kindly
 * All calls are blocking — run them on a background thread (Dispatchers.IO).
 */
object Groq {
    private const val STT_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
    private const val CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val STT_MODEL = "whisper-large-v3"
    private const val CHAT_MODEL = "llama-3.3-70b-versatile"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Transcribe an audio file to text. Returns null on failure (caller falls back). */
    fun transcribe(key: String, audio: File): String? {
        if (key.isBlank() || !audio.exists()) return null
        return try {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("model", STT_MODEL)
                .addFormDataPart("response_format", "json")
                .addFormDataPart("file", audio.name, audio.asRequestBody("audio/m4a".toMediaType()))
                .build()
            val req = Request.Builder().url(STT_URL)
                .addHeader("Authorization", "Bearer $key")
                .post(body).build()
            client.newCall(req).execute().use { resp ->
                val s = resp.body?.string() ?: return null
                if (!resp.isSuccessful) return null
                JSONObject(s).optString("text").trim().ifBlank { null }
            }
        } catch (_: Exception) { null }
    }

    /** Understand a sentence (any language) and decide what to do. */
    fun understand(key: String, text: String, contactNames: List<String>, lang: String): AiIntent? {
        if (key.isBlank() || text.isBlank()) return null
        val sys = buildString {
            append("You are Saarthi, a kind voice helper for an elderly person. ")
            append("Read what the user said (it may be in any language) and decide ONE action. ")
            append("Known people you can contact: ")
            append(if (contactNames.isEmpty()) "(none saved yet). " else contactNames.joinToString(", ") + ". ")
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
        val payload = JSONObject()
            .put("model", CHAT_MODEL)
            .put("temperature", 0.2)
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("messages", org.json.JSONArray()
                .put(JSONObject().put("role", "system").put("content", sys))
                .put(JSONObject().put("role", "user").put("content", text)))
        return try {
            val req = Request.Builder().url(CHAT_URL)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val s = resp.body?.string() ?: return null
                if (!resp.isSuccessful) return null
                val content = JSONObject(s).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content")
                val j = JSONObject(stripFences(content))
                AiIntent(
                    action = j.optString("action", "none"),
                    person = j.optString("person", ""),
                    query = j.optString("query", ""),
                    message = j.optString("message", ""),
                    reply = j.optString("reply", "")
                )
            }
        } catch (_: Exception) { null }
    }

    private fun stripFences(s: String): String {
        val t = s.trim()
        if (t.startsWith("```")) return t.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return t
    }
}
