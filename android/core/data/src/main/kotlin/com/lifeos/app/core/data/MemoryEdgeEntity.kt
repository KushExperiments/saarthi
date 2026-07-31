package com.lifeos.app.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A relationship between two [MemoryNodeEntity] rows (Memory §5's property
 * graph). `contextScope` is what makes "prefers Marathi for health
 * discussions" representable — null means the edge applies everywhere.
 */
@Entity(tableName = "memory_edges")
data class MemoryEdgeEntity(
    @PrimaryKey val id: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationType: String,
    val contextScope: String? = null,
    val confidence: Float,
    val temporalValidityStart: Long? = null,
    val temporalValidityEnd: Long? = null,
    val createdAt: Long,
)
