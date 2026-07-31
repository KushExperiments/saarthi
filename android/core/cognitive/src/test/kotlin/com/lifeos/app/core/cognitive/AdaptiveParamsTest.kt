package com.lifeos.app.core.cognitive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AdaptiveParamsTest {

    @Test
    fun `values within the bounded range construct normally`() {
        assertEquals(1, NagInterval(1).minutes)
        assertEquals(5, NagInterval(5).minutes)
        assertEquals(3, NagInterval(3).minutes)
    }

    @Test
    fun `nag interval can never adapt down to zero or negative, the hard floor`() {
        assertThrows(IllegalArgumentException::class.java) { NagInterval(0) }
        assertThrows(IllegalArgumentException::class.java) { NagInterval(-1) }
    }

    @Test
    fun `nag interval has a ceiling too`() {
        assertThrows(IllegalArgumentException::class.java) { NagInterval(6) }
    }

    @Test
    fun `coerce clamps a corrupted stored value instead of throwing`() {
        assertEquals(1, NagInterval.coerce(-50).minutes)
        assertEquals(5, NagInterval.coerce(999).minutes)
        assertEquals(2, NagInterval.coerce(2).minutes)
    }
}
