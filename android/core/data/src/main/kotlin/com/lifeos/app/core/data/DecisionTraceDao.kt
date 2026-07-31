package com.lifeos.app.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DecisionTraceDao {
    @Query("SELECT * FROM decision_traces ORDER BY timestamp DESC")
    fun observeRecent(): Flow<List<DecisionTraceEntity>>

    @Query("SELECT * FROM decision_traces ORDER BY timestamp DESC LIMIT 1")
    suspend fun mostRecent(): DecisionTraceEntity?

    @Query("SELECT * FROM decision_traces WHERE id = :id")
    suspend fun getById(id: String): DecisionTraceEntity?

    @Insert
    suspend fun insert(trace: DecisionTraceEntity)
}
