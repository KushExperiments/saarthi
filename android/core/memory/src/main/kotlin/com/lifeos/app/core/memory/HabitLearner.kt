package com.lifeos.app.core.memory

import com.lifeos.app.core.data.HabitCandidateDao
import com.lifeos.app.core.data.HabitCandidateEntity
import java.util.UUID
import javax.inject.Inject

private const val DEFAULT_WINDOW_DAYS = 14
private const val DEFAULT_STALE_DAYS = 180
private const val MILLIS_PER_DAY = 86_400_000L

private object HabitStatus {
    const val CANDIDATE = "CANDIDATE"
    const val CONFIRMED = "CONFIRMED"
    const val DECAYED = "DECAYED"
}

/**
 * Memory §7 — a habit starts as a low-confidence hypothesis after one or
 * two occurrences and only strengthens on independent, repeated
 * observation. Showing a fact fifty times is not the same as confirming it
 * fifty times: nothing in here raises confidence from mere retrieval,
 * only from [observeOccurrence] (a real, independent observation) or
 * [confirm] (an explicit human confirmation).
 */
class HabitLearner @Inject constructor(
    private val dao: HabitCandidateDao,
) {
    suspend fun observeOccurrence(pattern: String, now: Long, windowDays: Int = DEFAULT_WINDOW_DAYS) {
        val existing = dao.findByPattern(pattern)
        if (existing == null) {
            dao.upsert(
                HabitCandidateEntity(
                    id = UUID.randomUUID().toString(),
                    patternDescription = pattern,
                    occurrenceCount = 1,
                    firstObservedAt = now,
                    lastObservedAt = now,
                    timeWindowDays = windowDays,
                    status = HabitStatus.CANDIDATE,
                ),
            )
        } else if (existing.status == HabitStatus.CANDIDATE) {
            dao.upsert(existing.copy(occurrenceCount = existing.occurrenceCount + 1, lastObservedAt = now))
        } else {
            // CONFIRMED habit re-observed — just refreshes lastObservedAt so
            // decayUnused() doesn't archive a habit that's still happening.
            dao.upsert(existing.copy(lastObservedAt = now))
        }
    }

    suspend fun eligibleCandidates(minCount: Int, now: Long): List<HabitCandidateEntity> =
        dao.eligibleForPromotion(minCount, now)

    /** The only path CANDIDATE -> CONFIRMED. Never automatic. */
    suspend fun confirm(candidateId: String, now: Long) {
        val candidate = dao.getById(candidateId) ?: return
        dao.upsert(candidate.copy(status = HabitStatus.CONFIRMED, confirmedAt = now))
    }

    /** Archives, never deletes — Memory §4's Archival is always reversible. */
    suspend fun decayUnused(now: Long, staleDays: Int = DEFAULT_STALE_DAYS) {
        val staleBefore = now - staleDays * MILLIS_PER_DAY
        dao.staleConfirmed(staleBefore).forEach { candidate ->
            dao.upsert(candidate.copy(status = HabitStatus.DECAYED))
        }
    }
}
