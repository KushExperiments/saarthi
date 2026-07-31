package com.lifeos.app.core.cognitive

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "lifeos_adaptive_params"
private const val KEY_NAG_INTERVAL_MINUTES = "nag_interval_minutes"

interface AdaptiveParamsStore {
    fun nagInterval(): NagInterval
    fun setNagIntervalMinutes(minutes: Int)
}

/** Plain (non-encrypted) SharedPreferences — matches VoiceSettingsPrefs's precedent for non-sensitive settings. */
@Singleton
class SharedPrefsAdaptiveParamsStore @Inject constructor(
    @ApplicationContext context: Context,
) : AdaptiveParamsStore {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun nagInterval(): NagInterval =
        NagInterval.coerce(prefs.getInt(KEY_NAG_INTERVAL_MINUTES, NagInterval.DEFAULT.minutes))

    override fun setNagIntervalMinutes(minutes: Int) {
        val bounded = NagInterval.coerce(minutes)
        prefs.edit().putInt(KEY_NAG_INTERVAL_MINUTES, bounded.minutes).apply()
    }
}
