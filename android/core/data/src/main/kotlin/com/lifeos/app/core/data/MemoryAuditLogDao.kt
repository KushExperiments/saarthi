package com.lifeos.app.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemoryAuditLogDao {
    @Query("SELECT * FROM memory_audit_log WHERE nodeId = :nodeId ORDER BY timestamp ASC")
    suspend fun forNode(nodeId: String): List<MemoryAuditLogEntity>

    @Insert
    suspend fun insert(entry: MemoryAuditLogEntity)
}
