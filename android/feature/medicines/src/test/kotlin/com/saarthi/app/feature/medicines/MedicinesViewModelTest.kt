package com.saarthi.app.feature.medicines

import com.saarthi.app.core.testing.MainDispatcherRule
import com.saarthi.app.core.testing.TestDispatcherProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MedicinesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMedicineRepository()
    private val scheduler = mockk<ReminderScheduler>(relaxed = true)
    private val viewModel = MedicinesViewModel(repository, scheduler, TestDispatcherProvider())

    @Test
    fun `addMedicine ignores a blank name`() {
        viewModel.addMedicine(name = "  ", times = listOf("08:00"))

        assertTrue(viewModel.medicines.value.isEmpty())
    }

    @Test
    fun `addMedicine ignores an empty time list`() {
        viewModel.addMedicine(name = "Aspirin", times = emptyList())

        assertTrue(viewModel.medicines.value.isEmpty())
    }

    @Test
    fun `addMedicine saves a sorted, trimmed medicine and schedules it`() {
        viewModel.addMedicine(name = "  Aspirin  ", times = listOf("20:00", "08:00"))

        val saved = viewModel.medicines.value.single()
        assertEquals("Aspirin", saved.name)
        assertEquals(listOf("08:00", "20:00"), saved.times)
        verify { scheduler.scheduleAll(saved) }
    }

    @Test
    fun `markTaken confirms every scheduled time for that medicine`() {
        viewModel.addMedicine(name = "Aspirin", times = listOf("08:00", "20:00"))
        val medicine = viewModel.medicines.value.single()

        viewModel.markTaken(medicine)

        val updated = viewModel.medicines.value.single()
        assertTrue(updated.isConfirmed("08:00"))
        assertTrue(updated.isConfirmed("20:00"))
    }

    @Test
    fun `delete removes the medicine and cancels its alarms`() {
        viewModel.addMedicine(name = "Aspirin", times = listOf("08:00"))
        val medicine = viewModel.medicines.value.single()

        viewModel.delete(medicine)

        assertTrue(viewModel.medicines.value.isEmpty())
        verify { scheduler.cancelAll(medicine) }
    }
}
