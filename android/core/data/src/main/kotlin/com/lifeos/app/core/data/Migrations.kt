package com.lifeos.app.core.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: adds the Memory system's seven tables (Knowledge Graph nodes/
 * edges, provenance, audit log, habit candidates, conversation summaries,
 * life timeline). No changes to `medicines`/`contacts` — additive only.
 *
 * `exportSchema = false` (unchanged from v1) means there's no generated
 * schema JSON to diff a MigrationTestHelper test against without first
 * running a real Room/KSP compile, which this environment can't do. Each
 * CREATE TABLE below is hand-verified column-for-column against its
 * entity's Kotlin properties instead (see the *Entity.kt files in this
 * package) — the safer check available without a build.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `memory_nodes` (
                `id` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `valueText` TEXT NOT NULL,
                `sensitivityTier` TEXT NOT NULL,
                `confidence` REAL NOT NULL,
                `source` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `validFrom` INTEGER NOT NULL,
                `validUntil` INTEGER,
                `active` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `memory_edges` (
                `id` TEXT NOT NULL,
                `sourceNodeId` TEXT NOT NULL,
                `targetNodeId` TEXT NOT NULL,
                `relationType` TEXT NOT NULL,
                `contextScope` TEXT,
                `confidence` REAL NOT NULL,
                `temporalValidityStart` INTEGER,
                `temporalValidityEnd` INTEGER,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `memory_provenance` (
                `id` TEXT NOT NULL,
                `nodeId` TEXT,
                `edgeId` TEXT,
                `sourceType` TEXT NOT NULL,
                `sourceDetail` TEXT NOT NULL,
                `recordedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `memory_audit_log` (
                `id` TEXT NOT NULL,
                `nodeId` TEXT,
                `edgeId` TEXT,
                `action` TEXT NOT NULL,
                `actor` TEXT NOT NULL,
                `reason` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habit_candidates` (
                `id` TEXT NOT NULL,
                `patternDescription` TEXT NOT NULL,
                `occurrenceCount` INTEGER NOT NULL,
                `firstObservedAt` INTEGER NOT NULL,
                `lastObservedAt` INTEGER NOT NULL,
                `timeWindowDays` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `confirmedAt` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `conversation_summaries` (
                `id` TEXT NOT NULL,
                `periodType` TEXT NOT NULL,
                `periodStart` INTEGER NOT NULL,
                `periodEnd` INTEGER NOT NULL,
                `summaryText` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `life_timeline_events` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `occurredAt` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `significance` TEXT NOT NULL,
                `sourceSummaryId` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}

/** v2 -> v3: adds the Decision Traceability table for Cognitive OS §18. Additive only. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `decision_traces` (
                `id` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `reasoningTypesFiredCsv` TEXT NOT NULL,
                `contextSummary` TEXT NOT NULL,
                `memoryProvenanceIdsCsv` TEXT NOT NULL,
                `rejectedAlternativesCsv` TEXT NOT NULL,
                `chosenAction` TEXT NOT NULL,
                `chosenConfidence` REAL NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}
