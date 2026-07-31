package com.lifeos.app.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MedicineEntity::class,
        ContactEntity::class,
        MemoryNodeEntity::class,
        MemoryEdgeEntity::class,
        MemoryProvenanceEntity::class,
        MemoryAuditLogEntity::class,
        HabitCandidateEntity::class,
        ConversationSummaryEntity::class,
        LifeTimelineEventEntity::class,
        DecisionTraceEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class LifeOSDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun contactDao(): ContactDao
    abstract fun memoryNodeDao(): MemoryNodeDao
    abstract fun memoryEdgeDao(): MemoryEdgeDao
    abstract fun memoryProvenanceDao(): MemoryProvenanceDao
    abstract fun memoryAuditLogDao(): MemoryAuditLogDao
    abstract fun habitCandidateDao(): HabitCandidateDao
    abstract fun conversationSummaryDao(): ConversationSummaryDao
    abstract fun lifeTimelineDao(): LifeTimelineDao
    abstract fun decisionTraceDao(): DecisionTraceDao
}
