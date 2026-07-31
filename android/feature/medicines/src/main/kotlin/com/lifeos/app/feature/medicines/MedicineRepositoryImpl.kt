package com.lifeos.app.feature.medicines

import com.lifeos.app.core.data.MedicineDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MedicineRepositoryImpl @Inject constructor(
    private val dao: MedicineDao,
) : MedicineRepository {

    override fun observeAll(): Flow<List<Medicine>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Medicine? = dao.getById(id)?.toDomain()

    override suspend fun save(medicine: Medicine) = dao.upsert(medicine.toEntity())

    override suspend fun delete(medicine: Medicine) = dao.delete(medicine.toEntity())

    override suspend fun markTaken(medicineId: String, time: String) {
        val current = dao.getById(medicineId)?.toDomain() ?: return
        val today = Medicine.today()
        val alreadyConfirmedToday = current.confirmedDate == today
        val updatedTimes = if (alreadyConfirmedToday) current.confirmedTimes + time else listOf(time)
        dao.upsert(current.copy(confirmedDate = today, confirmedTimes = updatedTimes.distinct()).toEntity())
    }
}
