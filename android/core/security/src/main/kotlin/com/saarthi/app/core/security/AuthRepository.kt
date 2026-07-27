package com.saarthi.app.core.security

/**
 * The only thing later modules should ever depend on — never the raw PIN,
 * never the storage mechanism. Swappable per Architecture §2's Dependency
 * Inversion principle (a future hardware-backed implementation could
 * replace [EncryptedPrefsAuthRepository] without touching a single caller).
 */
interface AuthRepository {
    fun isPinSet(): Boolean
    fun setPin(pin: String)
    fun verifyPin(pin: String): Boolean

    /** Reset the lock entirely. Not exposed in any UI yet — a future
     * caregiver-recovery module gates this properly first. */
    fun clearPin()
}
