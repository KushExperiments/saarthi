package com.lifeos.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `matches returns true for the correct PIN`() {
        val stored = PinHasher.create("4821")

        assertTrue(PinHasher.matches("4821", stored))
    }

    @Test
    fun `matches returns false for an incorrect PIN`() {
        val stored = PinHasher.create("4821")

        assertFalse(PinHasher.matches("1234", stored))
    }

    @Test
    fun `the same PIN produces a different hash each time due to random salt`() {
        val first = PinHasher.create("4821")
        val second = PinHasher.create("4821")

        assertNotEquals("salts should differ", first.saltHex, second.saltHex)
        assertNotEquals("hashes should differ because the salts differ", first.hashHex, second.hashHex)
        // But both still verify correctly against their own stored hash.
        assertTrue(PinHasher.matches("4821", first))
        assertTrue(PinHasher.matches("4821", second))
    }

    @Test
    fun `the raw PIN never appears in the stored hash or salt`() {
        val stored = PinHasher.create("4821")

        assertFalse(stored.hashHex.contains("4821"))
        assertFalse(stored.saltHex.contains("4821"))
    }
}
