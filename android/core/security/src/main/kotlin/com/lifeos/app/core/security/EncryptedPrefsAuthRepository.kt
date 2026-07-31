package com.lifeos.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_FILE = "lifeos_auth_prefs"
private const val KEY_SALT = "pin_salt"
private const val KEY_HASH = "pin_hash"

/**
 * Keystore-backed storage (Engineering Master Plan §9) — the PIN itself
 * never touches disk, only a salted hash (via [PinHasher]) inside a file
 * that is itself AES-256 encrypted with a Keystore-held master key.
 */
@Singleton
class EncryptedPrefsAuthRepository @Inject constructor(
    @ApplicationContext context: Context,
) : AuthRepository {

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

    override fun isPinSet(): Boolean =
        prefs.contains(KEY_SALT) && prefs.contains(KEY_HASH)

    override fun setPin(pin: String) {
        val hash = PinHasher.create(pin)
        prefs.edit()
            .putString(KEY_SALT, hash.saltHex)
            .putString(KEY_HASH, hash.hashHex)
            .apply()
    }

    override fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val hash = prefs.getString(KEY_HASH, null) ?: return false
        return PinHasher.matches(pin, PinHash(salt, hash))
    }

    override fun clearPin() {
        prefs.edit().remove(KEY_SALT).remove(KEY_HASH).apply()
    }
}
