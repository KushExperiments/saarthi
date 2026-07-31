package com.lifeos.app.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryEdgeDao {
    @Query("SELECT * FROM memory_edges WHERE sourceNodeId = :nodeId OR targetNodeId = :nodeId")
    fun observeByNode(nodeId: String): Flow<List<MemoryEdgeEntity>>

    @Query("SELECT * FROM memory_edges WHERE sourceNodeId = :nodeId OR targetNodeId = :nodeId")
    suspend fun neighborsOf(nodeId: String): List<MemoryEdgeEntity>

    @Upsert
    suspend fun upsert(edge: MemoryEdgeEntity)

    @Delete
    suspend fun delete(edge: MemoryEdgeEntity)
}
