package com.lifeos.app.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Answers "why did you remember this" from real stored metadata (Memory
 * §2/§11 — never synthesized after the fact). Deliberately holds only a
 * description/reference, NEVER raw transcript or audio (Master Plan §6's
 * backup-minimization rule).
 */
@Entity(tableName = "memory_provenance")
data class MemoryProvenanceEntity(
    @PrimaryKey val id: String,
    val nodeId: String? = null,
    val edgeId: String? = null,
    val sourceType: String,
    val sourceDetail: String,
    val recordedAt: Long,
)
