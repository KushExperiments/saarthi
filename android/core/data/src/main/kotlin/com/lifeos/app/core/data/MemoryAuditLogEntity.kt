package com.lifeos.app.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Append-only log backing "show what you know about me" / version history (Memory §11). */
@Entity(tableName = "memory_audit_log")
data class MemoryAuditLogEntity(
    @PrimaryKey val id: String,
    val nodeId: String? = null,
    val edgeId: String? = null,
    val action: String,
    val actor: String,
    val reason: String,
    val timestamp: Long,
)
