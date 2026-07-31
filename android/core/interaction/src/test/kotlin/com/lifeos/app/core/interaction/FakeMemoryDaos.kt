package com.lifeos.app.core.interaction

import com.lifeos.app.core.data.MemoryEdgeDao
import com.lifeos.app.core.data.MemoryEdgeEntity
import com.lifeos.app.core.data.MemoryNodeDao
import com.lifeos.app.core.data.MemoryNodeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory fakes — no Room, fully deterministic, only what [ClarificationEngineTest] needs. */
class FakeMemoryNodeDao(seed: List<MemoryNodeEntity> = emptyList()) : MemoryNodeDao {
    private val state = MutableStateFlow(seed)

    override fun observeAll(): StateFlow<List<MemoryNodeEntity>> = state
    override fun observeByCategory(category: String): StateFlow<List<MemoryNodeEntity>> =
        MutableStateFlow(state.value.filter { it.category == category })

    override suspend fun getById(id: String): MemoryNodeEntity? = state.value.firstOrNull { it.id == id }
    override suspend fun findByLabel(text: String): List<MemoryNodeEntity> =
        state.value.filter { it.active && it.label.contains(text, ignoreCase = true) }

    override suspend fun upsert(node: MemoryNodeEntity) {
        state.value = state.value.filterNot { it.id == node.id } + node
    }

    override suspend fun delete(node: MemoryNodeEntity) {
        state.value = state.value.filterNot { it.id == node.id }
    }
}

class FakeMemoryEdgeDao : MemoryEdgeDao {
    override fun observeByNode(nodeId: String): StateFlow<List<MemoryEdgeEntity>> = MutableStateFlow(emptyList())
    override suspend fun neighborsOf(nodeId: String): List<MemoryEdgeEntity> = emptyList()
    override suspend fun upsert(edge: MemoryEdgeEntity) = Unit
    override suspend fun delete(edge: MemoryEdgeEntity) = Unit
}
