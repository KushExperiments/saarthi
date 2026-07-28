package com.saarthi.app.feature.medicines

import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun observeAll(): Flow<List<Medicine>>
    suspend fun getById(id: String): Medicine?
    suspend fun save(medicine: Medicine)
    suspend fun delete(medicine: Medicine)

    /** Marks [time] confirmed for today; rolls the confirmed set over if the day changed. */
    suspend fun markTaken(medicineId: String, time: String)
}
