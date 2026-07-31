package com.lifeos.app.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A discrete, dated life event (Memory §6) — birthdays, hospital visits,
 * grandchild born. Deliberately NOT for trend series (blood pressure,
 * weight) — those live in their own module's store and only ever
 * contribute periodic trend-summary entries here, per Memory §6.
 */
@Entity(tableName = "life_timeline_events")
data class LifeTimelineEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val occurredAt: Long,
    val category: String,
    val significance: String,
    val sourceSummaryId: String? = null,
)
