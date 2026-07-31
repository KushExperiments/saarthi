package com.lifeos.app

import android.content.Context
import androidx.core.content.edit

/**
 * Whether the one-time welcome screen has already run. Plain
 * SharedPreferences, not the encrypted store core:security uses for the
 * PIN — this flag isn't sensitive, and onboarding must be readable before
 * any lock/auth flow exists.
 */
object OnboardingPrefs {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_SEEN = "has_seen_onboarding"

    fun hasSeenOnboarding(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false)

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putBoolean(KEY_SEEN, true) }
    }
}
