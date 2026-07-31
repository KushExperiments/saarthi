package com.lifeos.app.core.memory

import com.lifeos.app.core.data.MemoryNodeEntity
import com.lifeos.app.core.data.MemoryProvenanceEntity

interface MemoryRepository {
    suspend fun remember(
        category: MemoryCategory,
        label: String,
        valueText: String,
        source: MemorySource,
        sourceDetail: String,
        now: Long,
    ): MemoryNodeEntity

    suspend fun recall(query: String, categoryHint: MemoryCategory? = null, now: Long): List<ScoredMemory>

    suspend fun forget(factId: String, reason: String, now: Long)

    suspend fun correct(factId: String, newValue: String, now: Long): MemoryNodeEntity?

    /** Reads real stored provenance rows — never synthesizes an explanation (Memory §2/§11). */
    suspend fun whyRemembered(factId: String): List<MemoryProvenanceEntity>
}
