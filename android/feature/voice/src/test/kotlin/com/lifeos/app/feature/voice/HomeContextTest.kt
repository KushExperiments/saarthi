package com.lifeos.app.feature.voice

import com.lifeos.app.feature.medicines.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeContextTest {

    @Test
    fun `greeting uses morning, afternoon, evening by hour`() {
        assertEquals("Good morning, Kamala.", timeOfDayGreeting(hour = 8, name = "Kamala"))
        assertEquals("Good afternoon, Kamala.", timeOfDayGreeting(hour = 14, name = "Kamala"))
        assertEquals("Good evening, Kamala.", timeOfDayGreeting(hour = 20, name = "Kamala"))
    }

    @Test
    fun `greeting omits the name when none is known, never a placeholder`() {
        assertEquals("Good morning.", timeOfDayGreeting(hour = 8, name = null))
        assertEquals("Good morning.", timeOfDayGreeting(hour = 8, name = "  "))
    }

    @Test
    fun `no next-due description when nothing is unconfirmed`() {
        val medicine = Medicine(id = "m1", name = "Aspirin", times = listOf("08:00"), confirmedDate = Medicine.today(), confirmedTimes = listOf("08:00"))
        assertNull(nextDueDescription(listOf(medicine)))
    }

    @Test
    fun `no next-due description when there are no medicines at all`() {
        assertNull(nextDueDescription(emptyList()))
    }

    @Test
    fun `next-due description picks the earliest unconfirmed time`() {
        val medicine = Medicine(
            id = "m1",
            name = "Blood pressure",
            times = listOf("20:00", "08:00"),
            confirmedDate = Medicine.today(),
            confirmedTimes = listOf("08:00"),
        )
        assertEquals("Your Blood pressure is due at 8:00 PM.", nextDueDescription(listOf(medicine)))
    }

    @Test
    fun `formatTime12 converts 24h to 12h with am pm`() {
        assertEquals("12:00 AM", formatTime12("00:00"))
        assertEquals("8:00 AM", formatTime12("08:00"))
        assertEquals("12:00 PM", formatTime12("12:00"))
        assertEquals("8:00 PM", formatTime12("20:00"))
    }
}
