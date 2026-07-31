package com.lifeos.app.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Memory §13: daily/weekly rollups. Raw transcripts are never stored here or anywhere. */
@Entity(tableName = "conversation_summaries")
data class ConversationSummaryEntity(
    @PrimaryKey val id: String,
    val periodType: String,
    val periodStart: Long,
    val periodEnd: Long,
    val summaryText: String,
    val createdAt: Long,
)
