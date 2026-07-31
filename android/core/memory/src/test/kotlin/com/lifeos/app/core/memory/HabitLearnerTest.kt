package com.lifeos.app.core.memory

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY = 86_400_000L

class HabitLearnerTest {

    private fun learner(dao: FakeHabitCandidateDao = FakeHabitCandidateDao()) = HabitLearner(dao) to dao

    @Test
    fun `a pattern seen twice is not yet a habit`() = runTest {
        val (learner, dao) = learner()

        learner.observeOccurrence("tea at 7am", now = 0L)
        learner.observeOccurrence("tea at 7am", now = DAY)

        val eligible = learner.eligibleCandidates(minCount = 3, now = 20 * DAY)
        assertTrue(eligible.isEmpty())
        assertEquals(2, dao.findByPattern("tea at 7am")?.occurrenceCount)
    }

    @Test
    fun `crossing the count gate without crossing the window gate is still not eligible`() = runTest {
        val (learner, _) = learner()

        // Five occurrences, but all within the first day — window (default 14d) not satisfied.
        repeat(5) { learner.observeOccurrence("tea at 7am", now = it * 1000L) }

        val eligible = learner.eligibleCandidates(minCount = 3, now = 2 * DAY)
        assertTrue(eligible.isEmpty())
    }

    @Test
    fun `crossing both the count and window gates surfaces the candidate`() = runTest {
        val (learner, _) = learner()

        learner.observeOccurrence("tea at 7am", now = 0L)
        learner.observeOccurrence("tea at 7am", now = 5 * DAY)
        learner.observeOccurrence("tea at 7am", now = 10 * DAY)

        val eligible = learner.eligibleCandidates(minCount = 3, now = 20 * DAY)
        assertEquals(1, eligible.size)
        assertEquals("tea at 7am", eligible.first().patternDescription)
    }

    @Test
    fun `confirm is the only path from CANDIDATE to CONFIRMED, and requires an explicit call`() = runTest {
        val (learner, dao) = learner()
        learner.observeOccurrence("tea at 7am", now = 0L)
        val candidate = dao.findByPattern("tea at 7am")!!

        // Repeated observation and even eligibility never flips status on their own.
        learner.observeOccurrence("tea at 7am", now = 20 * DAY)
        assertEquals("CANDIDATE", dao.findByPattern("tea at 7am")?.status)

        learner.confirm(candidate.id, now = 21 * DAY)

        assertEquals("CONFIRMED", dao.findByPattern("tea at 7am")?.status)
    }

    @Test
    fun `decayUnused archives stale confirmed habits without deleting them`() = runTest {
        val (learner, dao) = learner()
        learner.observeOccurrence("tea at 7am", now = 0L)
        val candidate = dao.findByPattern("tea at 7am")!!
        learner.confirm(candidate.id, now = 1 * DAY)

        learner.decayUnused(now = 400 * DAY, staleDays = 180)

        val decayed = dao.findByPattern("tea at 7am")
        assertEquals("DECAYED", decayed?.status)
        assertEquals(candidate.id, decayed?.id) // still present, not deleted
    }
}
