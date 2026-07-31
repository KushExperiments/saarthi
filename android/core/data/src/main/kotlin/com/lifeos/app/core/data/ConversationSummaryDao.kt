package com.lifeos.app.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationSummaryDao {
    @Query("SELECT * FROM conversation_summaries WHERE periodType = :periodType ORDER BY periodStart DESC")
    fun observeByPeriodType(periodType: String): Flow<List<ConversationSummaryEntity>>

    @Upsert
    suspend fun upsert(summary: ConversationSummaryEntity)
}
