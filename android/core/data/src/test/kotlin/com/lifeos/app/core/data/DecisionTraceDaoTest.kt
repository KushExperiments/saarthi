package com.lifeos.app.core.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Exercises the v3 schema addition (decision_traces) — see MemoryDaoTest.kt for why Robolectric here is safe to rely on. */
@RunWith(RobolectricTestRunner::class)
class DecisionTraceDaoTest {

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
    fun `inserted traces round-trip and are ordered newest first`() = runTest {
        val dao = db.decisionTraceDao()
        val older = DecisionTraceEntity(
            id = "t1",
            timestamp = 1000L,
            reasoningTypesFiredCsv = "RULE",
            contextSummary = "night=false",
            memoryProvenanceIdsCsv = "",
            rejectedAlternativesCsv = "",
            chosenAction = "open_medicines",
            chosenConfidence = 0.7f,
        )
        val newer = older.copy(id = "t2", timestamp = 2000L, chosenAction = "call")

        dao.insert(older)
        dao.insert(newer)

        assertEquals(listOf(newer, older), dao.observeRecent().first())
        assertEquals(newer, dao.mostRecent())
        assertEquals(older, dao.getById("t1"))
        assertNull(dao.getById("does-not-exist"))
    }
}
