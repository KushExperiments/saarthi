package com.lifeos.app.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single remembered fact (Memory §3/§5's Knowledge Graph node). `category`/
 * `sensitivityTier`/`source` are stored as their enum's `.name` — core:memory
 * owns the actual enums, core:data stays a dumb storage layer per the
 * project's existing entity/domain split (see MedicineEntity vs. Medicine).
 */
@Entity(tableName = "memory_nodes")
data class MemoryNodeEntity(
    @PrimaryKey val id: String,
    val category: String,
    val label: String,
    val valueText: String,
    val sensitivityTier: String,
    val confidence: Float,
    val source: String,
    val createdAt: Long,
    val updatedAt: Long,
    val validFrom: Long,
    val validUntil: Long? = null,
    val active: Boolean = true,
)
