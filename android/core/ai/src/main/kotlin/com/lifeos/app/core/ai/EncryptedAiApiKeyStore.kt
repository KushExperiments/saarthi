package com.lifeos.app.core.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_FILE = "lifeos_ai_prefs"
private const val KEY_API_KEY = "gemini_api_key"

/**
 * Keystore-backed storage, same technique as core:security's
 * EncryptedPrefsAuthRepository (own prefs file, AES256_GCM master key) but
 * kept independent of core:security — a Gemini key is a different concern
 * from PIN auth, not a reason to add a cross-module dependency.
 *
 * No unit test yet: same deliberate deferral as EncryptedPrefsAuthRepository
 * (see android/README.md's M-002 "Known Gaps" — the thin Android-Keystore
 * integration layer needs an emulator or Robolectric shadow config neither
 * of which this codebase has set up yet). Not a new gap, the same one.
 */
@Singleton
class EncryptedAiApiKeyStore @Inject constructor(
    @ApplicationContext context: Context,
) : AiApiKeyStore {

    private val prefs: SharedPreferences = run {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    override fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    override fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }
}
