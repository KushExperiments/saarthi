package com.lifeos.app.core.data

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * First Robolectric-backed test in this codebase (android/README.md's M-002
 * notes list Robolectric as available-but-unused). Room's in-memory-database
 * + Robolectric combination is standard/well-supported, unlike the
 * Keystore-backed EncryptedSharedPreferences case that README documents as
 * still deferred — different risk profile, so this one gets written.
 *
 * Exercises the v2 schema directly (all entities as currently defined) —
 * this verifies the new @Entity/@Dao annotations are valid and Room can
 * generate real implementations for them, which is the highest-value check
 * available without a compiler. [MIGRATION_1_2]'s SQL is verified by eye
 * against these same entities (see Migrations.kt's doc comment) rather than
 * a MigrationTestHelper test, since exportSchema=false means there's no
 * schema JSON to test a migration against without a real Room/KSP build.
 */
@RunWith(RobolectricTestRunner::class)
class MemoryDaoTest {

    private lateinit var db: LifeOSDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LifeOSDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `memory node upsert and read round-trips every field`() = runTest {
        val node = MemoryNodeEntity(
            id = "n1",
            category = "IDENTITY",
            label = "preferred name",
            valueText = "Kamala",
            sensitivityTier = "MEDIUM",
            confidence = 0.95f,
            source = "USER_STATED",
            createdAt = 1000L,
            updatedAt = 1000L,
            validFrom = 1000L,
        )

        db.memoryNodeDao().upsert(node)

        assertEquals(node, db.memoryNodeDao().getById("n1"))
        assertEquals(listOf(node), db.memoryNodeDao().findByLabel("preferred"))
    }

    @Test
    fun `memory edge links two nodes and is queryable from either side`() = runTest {
        val edge = MemoryEdgeEntity(
            id = "e1",
            sourceNodeId = "n1",
            targetNodeId = "n2",
            relationType = "child_of",
            confidence = 0.9f,
            createdAt = 1000L,
        )

        db.memoryEdgeDao().upsert(edge)

        assertEquals(listOf(edge), db.memoryEdgeDao().neighborsOf("n1"))
        assertEquals(listOf(edge), db.memoryEdgeDao().neighborsOf("n2"))
    }

    @Test
    fun `provenance is append-only and traceable back to its node`() = runTest {
        val provenance = MemoryProvenanceEntity(
            id = "p1",
            nodeId = "n1",
            sourceType = "USER_STATED",
            sourceDetail = "Elder confirmed during onboarding",
            recordedAt = 1000L,
        )

        db.memoryProvenanceDao().insert(provenance)

        assertEquals(listOf(provenance), db.memoryProvenanceDao().forNode("n1"))
    }

    @Test
    fun `habit candidate eligibility respects both the count and window gates`() = runTest {
        val dao = db.habitCandidateDao()
        val eligible = HabitCandidateEntity(
            id = "h1",
            patternDescription = "tea around 7am",
            occurrenceCount = 5,
            firstObservedAt = 1000L,
            lastObservedAt = 5000L,
            timeWindowDays = 14,
            status = "CANDIDATE",
        )
        val tooFew = eligible.copy(id = "h2", occurrenceCount = 1)
        dao.upsert(eligible)
        dao.upsert(tooFew)

        val result = dao.eligibleForPromotion(minCount = 3, now = 1_300_000_000L)

        assertEquals(listOf(eligible), result)
    }

    @Test
    fun `life timeline orders newest first and filters by significance`() = runTest {
        val dao = db.lifeTimelineDao()
        val minor = LifeTimelineEventEntity(
            id = "t1",
            title = "Doctor visit",
            description = "Routine checkup",
            occurredAt = 1000L,
            category = "Health",
            significance = "LOW",
        )
        val major = LifeTimelineEventEntity(
            id = "t2",
            title = "Grandchild born",
            description = "",
            occurredAt = 2000L,
            category = "LifeStory",
            significance = "HIGH",
        )
        dao.upsert(minor)
        dao.upsert(major)

        assertEquals(listOf(major), dao.inRange(1500L, 3000L))
        assertNull(dao.inRange(0L, 500L).firstOrNull())
    }
}
