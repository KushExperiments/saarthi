package com.lifeos.app.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemoryProvenanceDao {
    @Query("SELECT * FROM memory_provenance WHERE nodeId = :nodeId ORDER BY recordedAt ASC")
    suspend fun forNode(nodeId: String): List<MemoryProvenanceEntity>

    @Query("SELECT * FROM memory_provenance WHERE edgeId = :edgeId ORDER BY recordedAt ASC")
    suspend fun forEdge(edgeId: String): List<MemoryProvenanceEntity>

    @Insert
    suspend fun insert(provenance: MemoryProvenanceEntity)
}
