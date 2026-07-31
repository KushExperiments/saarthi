package com.lifeos.app.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryNodeDao {
    @Query("SELECT * FROM memory_nodes WHERE active = 1 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoryNodeEntity>>

    @Query("SELECT * FROM memory_nodes WHERE category = :category AND active = 1 ORDER BY updatedAt DESC")
    fun observeByCategory(category: String): Flow<List<MemoryNodeEntity>>

    @Query("SELECT * FROM memory_nodes WHERE id = :id")
    suspend fun getById(id: String): MemoryNodeEntity?

    @Query("SELECT * FROM memory_nodes WHERE active = 1 AND label LIKE '%' || :text || '%'")
    suspend fun findByLabel(text: String): List<MemoryNodeEntity>

    @Upsert
    suspend fun upsert(node: MemoryNodeEntity)

    @Delete
    suspend fun delete(node: MemoryNodeEntity)
}
