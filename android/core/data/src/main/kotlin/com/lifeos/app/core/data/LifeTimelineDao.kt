package com.lifeos.app.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeTimelineDao {
    @Query("SELECT * FROM life_timeline_events ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<LifeTimelineEventEntity>>

    @Query("SELECT * FROM life_timeline_events WHERE significance = 'HIGH' ORDER BY occurredAt DESC")
    fun observeSignificant(): Flow<List<LifeTimelineEventEntity>>

    @Query("SELECT * FROM life_timeline_events WHERE occurredAt BETWEEN :from AND :to ORDER BY occurredAt DESC")
    suspend fun inRange(from: Long, to: Long): List<LifeTimelineEventEntity>

    @Upsert
    suspend fun upsert(event: LifeTimelineEventEntity)
}
