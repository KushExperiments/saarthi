package com.lifeos.app.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCandidateDao {
    @Query("SELECT * FROM habit_candidates ORDER BY lastObservedAt DESC")
    fun observeAll(): Flow<List<HabitCandidateEntity>>

    @Query("SELECT * FROM habit_candidates WHERE patternDescription = :pattern LIMIT 1")
    suspend fun findByPattern(pattern: String): HabitCandidateEntity?

    @Query("SELECT * FROM habit_candidates WHERE id = :id")
    suspend fun getById(id: String): HabitCandidateEntity?

    // firstObservedAt must be at least *that candidate's own* timeWindowDays
    // in the past — a fast-repeating pattern and a slow one can't share one
    // external cutoff, each row's window has to be evaluated on its own terms.
    @Query(
        "SELECT * FROM habit_candidates WHERE status = 'CANDIDATE' " +
            "AND occurrenceCount >= :minCount " +
            "AND firstObservedAt <= (:now - (timeWindowDays * 86400000))",
    )
    suspend fun eligibleForPromotion(minCount: Int, now: Long): List<HabitCandidateEntity>

    @Query("SELECT * FROM habit_candidates WHERE status = 'CONFIRMED' AND lastObservedAt < :staleBefore")
    suspend fun staleConfirmed(staleBefore: Long): List<HabitCandidateEntity>

    @Upsert
    suspend fun upsert(candidate: HabitCandidateEntity)
}
