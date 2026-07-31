package com.lifeos.app.core.memory

import com.lifeos.app.core.data.HabitCandidateDao
import com.lifeos.app.core.data.HabitCandidateEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val MILLIS_PER_DAY = 86_400_000L

/** In-memory fake — no Room, no Robolectric, fully deterministic. */
class FakeHabitCandidateDao : HabitCandidateDao {
    private val state = MutableStateFlow<List<HabitCandidateEntity>>(emptyList())

    override fun observeAll(): StateFlow<List<HabitCandidateEntity>> = state

    override suspend fun findByPattern(pattern: String): HabitCandidateEntity? =
        state.value.firstOrNull { it.patternDescription == pattern }

    override suspend fun getById(id: String): HabitCandidateEntity? =
        state.value.firstOrNull { it.id == id }

    override suspend fun eligibleForPromotion(minCount: Int, now: Long): List<HabitCandidateEntity> =
        state.value.filter {
            it.status == "CANDIDATE" &&
                it.occurrenceCount >= minCount &&
                it.firstObservedAt <= now - it.timeWindowDays * MILLIS_PER_DAY
        }

    override suspend fun staleConfirmed(staleBefore: Long): List<HabitCandidateEntity> =
        state.value.filter { it.status == "CONFIRMED" && it.lastObservedAt < staleBefore }

    override suspend fun upsert(candidate: HabitCandidateEntity) {
        state.value = state.value.filterNot { it.id == candidate.id } + candidate
    }
}
