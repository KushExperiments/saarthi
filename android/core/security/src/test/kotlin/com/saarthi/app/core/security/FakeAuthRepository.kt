package com.saarthi.app.core.security

/** In-memory fake — no Android framework, no Keystore, fully deterministic. */
class FakeAuthRepository : AuthRepository {
    private var stored: PinHash? = null

    override fun isPinSet(): Boolean = stored != null

    override fun setPin(pin: String) {
        stored = PinHasher.create(pin)
    }

    override fun verifyPin(pin: String): Boolean =
        stored?.let { PinHasher.matches(pin, it) } ?: false

    override fun clearPin() {
        stored = null
    }
}
