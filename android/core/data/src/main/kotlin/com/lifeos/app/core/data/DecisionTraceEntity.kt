package com.lifeos.app.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cognitive OS §18's Decision Traceability / Audit Log — every decision the
 * Decision Engine makes logs enough to answer "why did you do that" from
 * real stored data, never a synthesized-after-the-fact explanation. Lists
 * are stored as simple `|`-delimited text rather than JSON — core:data has
 * no JSON dependency today and this data is flat, so a delimiter avoids
 * adding one just for this.
 */
@Entity(tableName = "decision_traces")
data class DecisionTraceEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val reasoningTypesFiredCsv: String,
    val contextSummary: String,
    val memoryProvenanceIdsCsv: String,
    val rejectedAlternativesCsv: String,
    val chosenAction: String,
    val chosenConfidence: Float,
)
