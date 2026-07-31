package com.lifeos.app.feature.medicines

import com.lifeos.app.core.data.MedicineEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicineTest {

    @Test
    fun `isConfirmed is false when confirmedDate is not today`() {
        val medicine = Medicine(
            id = "1",
            name = "Aspirin",
            times = listOf("08:00"),
            confirmedDate = "2020-01-01",
            confirmedTimes = listOf("08:00"),
        )

        assertFalse(medicine.isConfirmed("08:00"))
    }

    @Test
    fun `isConfirmed is true only for a time confirmed today`() {
        val medicine = Medicine(
            id = "1",
            name = "Aspirin",
            times = listOf("08:00", "20:00"),
            confirmedDate = Medicine.today(),
            confirmedTimes = listOf("08:00"),
        )

        assertTrue(medicine.isConfirmed("08:00"))
        assertFalse(medicine.isConfirmed("20:00"))
    }

    @Test
    fun `entity round-trips through toDomain and toEntity without losing data`() {
        val entity = MedicineEntity(
            id = "1",
            name = "Aspirin",
            timesCsv = "08:00,20:00",
            confirmedDate = "2026-01-01",
            confirmedTimesCsv = "08:00",
        )

        val roundTripped = entity.toDomain().toEntity()

        assertEquals(entity, roundTripped)
    }
}
