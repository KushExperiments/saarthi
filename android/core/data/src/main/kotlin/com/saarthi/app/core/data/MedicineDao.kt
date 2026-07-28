package com.saarthi.app.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY name ASC")
    fun observeAll(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getById(id: String): MedicineEntity?

    @Upsert
    suspend fun upsert(medicine: MedicineEntity)

    @Delete
    suspend fun delete(medicine: MedicineEntity)
}
