package com.lifeos.app.feature.voice

import android.content.Context
import androidx.core.content.edit

/** Plain SharedPreferences — not sensitive, doesn't need core:security's encrypted store. */
object VoiceSettingsPrefs {
    private const val PREFS_NAME = "voice_settings_prefs"
    private const val KEY_ALWAYS_LISTENING = "always_listening_enabled"

    fun isAlwaysListeningEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ALWAYS_LISTENING, false)

    fun setAlwaysListeningEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putBoolean(KEY_ALWAYS_LISTENING, enabled) }
    }
}
