package com.lifeos.app.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Memory §7's Habit Learning: a pattern starts as a low-confidence
 * hypothesis and only strengthens on independent, repeated observation —
 * never on being merely retrieved or displayed.
 */
@Entity(tableName = "habit_candidates")
data class HabitCandidateEntity(
    @PrimaryKey val id: String,
    val patternDescription: String,
    val occurrenceCount: Int,
    val firstObservedAt: Long,
    val lastObservedAt: Long,
    val timeWindowDays: Int,
    val status: String,
    val confirmedAt: Long? = null,
)
