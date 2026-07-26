package com.saarthi.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A person the elder can call/message by a simple name ("Beta", "Vishal", "Doctor"). */
data class Contact(val id: String, val name: String, val phone: String)

/** A medicine and the times of day it must be taken. */
data class Medicine(
    val id: String,
    val name: String,
    val times: List<String>,          // "HH:mm" 24h
    val takenDate: String = "",       // yyyy-MM-dd of last taken record
    val doneTimes: List<String> = emptyList()
)

/** App-wide preferences. The assistant's name is changeable here. */
data class Settings(
    var appName: String = "Saarthi",
    var lang: String = "en-IN",       // BCP-47 for speech + TTS
    var rate: Float = 0.8f,           // slow, friendly
    var userName: String = "",        // who to greet
    var groqKey: String = "",         // free Groq API key (Llama + Whisper)
    var useAI: Boolean = true,        // use Llama to understand (when key + online)
    var useWhisper: Boolean = true    // use Whisper to hear (when key + online)
)

/**
 * Tiny persistent store backed by SharedPreferences + JSON.
 * No database, no server — everything stays on the phone.
 */
object Store {
    private const val PREF = "saarthi"

    fun sp(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ---- Settings ----
    fun settings(ctx: Context): Settings {
        val o = JSONObject(sp(ctx).getString("settings", "{}") ?: "{}")
        return Settings(
            appName = o.optString("appName", "Saarthi"),
            lang = o.optString("lang", "en-IN"),
            rate = o.optDouble("rate", 0.8).toFloat(),
            userName = o.optString("userName", ""),
            // fall back to the optional key baked in from local.properties
            groqKey = o.optString("groqKey", "").ifBlank { BuildConfig.GROQ_KEY },
            useAI = o.optBoolean("useAI", true),
            useWhisper = o.optBoolean("useWhisper", true)
        )
    }
    fun saveSettings(ctx: Context, s: Settings) {
        val o = JSONObject()
            .put("appName", s.appName).put("lang", s.lang)
            .put("rate", s.rate.toDouble()).put("userName", s.userName)
            .put("groqKey", s.groqKey).put("useAI", s.useAI).put("useWhisper", s.useWhisper)
        sp(ctx).edit().putString("settings", o.toString()).apply()
    }

    // ---- Contacts ----
    fun contacts(ctx: Context): MutableList<Contact> {
        val arr = JSONArray(sp(ctx).getString("contacts", "[]") ?: "[]")
        val list = mutableListOf<Contact>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Contact(o.getString("id"), o.getString("name"), o.getString("phone")))
        }
        return list
    }
    fun saveContacts(ctx: Context, list: List<Contact>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone)) }
        sp(ctx).edit().putString("contacts", arr.toString()).apply()
    }

    // ---- Medicines ----
    fun medicines(ctx: Context): MutableList<Medicine> {
        val arr = JSONArray(sp(ctx).getString("medicines", "[]") ?: "[]")
        val list = mutableListOf<Medicine>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val times = mutableListOf<String>()
            val ta = o.getJSONArray("times"); for (j in 0 until ta.length()) times.add(ta.getString(j))
            val done = mutableListOf<String>()
            val da = o.optJSONArray("doneTimes") ?: JSONArray()
            for (j in 0 until da.length()) done.add(da.getString(j))
            list.add(Medicine(o.getString("id"), o.getString("name"), times, o.optString("takenDate", ""), done))
        }
        return list
    }
    fun saveMedicines(ctx: Context, list: List<Medicine>) {
        val arr = JSONArray()
        list.forEach { m ->
            val o = JSONObject().put("id", m.id).put("name", m.name).put("takenDate", m.takenDate)
            o.put("times", JSONArray(m.times))
            o.put("doneTimes", JSONArray(m.doneTimes))
            arr.put(o)
        }
        sp(ctx).edit().putString("medicines", arr.toString()).apply()
    }
}

fun genId(): String = java.util.UUID.randomUUID().toString().take(8)
