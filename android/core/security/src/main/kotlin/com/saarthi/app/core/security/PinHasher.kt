package com.saarthi.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom

/** A salted hash — what actually gets persisted. Never the raw PIN. */
data class PinHash(val saltHex: String, val hashHex: String)

/**
 * Deliberately dependency-free (no Android framework, no third-party crypto
 * library) — just [MessageDigest] and [SecureRandom] from the JDK, so this
 * is 100% unit-testable on plain JVM with zero Robolectric/Keystore risk.
 * The PIN itself is never stored or logged — only this salted hash is.
 */
object PinHasher {
    private const val SALT_BYTES = 16
    private val random = SecureRandom()

    fun create(pin: String): PinHash {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return PinHash(saltHex = salt.toHex(), hashHex = hash(pin, salt).toHex())
    }

    fun matches(pin: String, stored: PinHash): Boolean {
        val salt = stored.saltHex.fromHex()
        val candidate = hash(pin, salt).toHex()
        // Constant-time-ish comparison to avoid trivial timing leaks.
        return candidate.length == stored.hashHex.length &&
            candidate.zip(stored.hashHex).fold(true) { acc, (a, b) -> acc and (a == b) }
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
