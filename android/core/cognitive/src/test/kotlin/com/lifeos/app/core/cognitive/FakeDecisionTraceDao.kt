package com.lifeos.app.core.cognitive

import com.lifeos.app.core.data.DecisionTraceDao
import com.lifeos.app.core.data.DecisionTraceEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeDecisionTraceDao : DecisionTraceDao {
    private val state = MutableStateFlow<List<DecisionTraceEntity>>(emptyList())

    override fun observeRecent(): StateFlow<List<DecisionTraceEntity>> = state
    override suspend fun mostRecent(): DecisionTraceEntity? = state.value.maxByOrNull { it.timestamp }
    override suspend fun getById(id: String): DecisionTraceEntity? = state.value.firstOrNull { it.id == id }
    override suspend fun insert(trace: DecisionTraceEntity) {
        state.value = state.value + trace
    }
}
