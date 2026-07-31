package com.lifeos.app.feature.medicines

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeMedicineRepository : MedicineRepository {
    private val state = MutableStateFlow<List<Medicine>>(emptyList())

    override fun observeAll(): StateFlow<List<Medicine>> = state
    override suspend fun getById(id: String): Medicine? = state.value.firstOrNull { it.id == id }

    override suspend fun save(medicine: Medicine) {
        state.value = state.value.filterNot { it.id == medicine.id } + medicine
    }

    override suspend fun delete(medicine: Medicine) {
        state.value = state.value.filterNot { it.id == medicine.id }
    }

    override suspend fun markTaken(medicineId: String, time: String) {
        val current = state.value.firstOrNull { it.id == medicineId } ?: return
        val today = Medicine.today()
        val alreadyToday = current.confirmedDate == today
        val updated = current.copy(
            confirmedDate = today,
            confirmedTimes = (if (alreadyToday) current.confirmedTimes + time else listOf(time)).distinct(),
        )
        save(updated)
    }
}
