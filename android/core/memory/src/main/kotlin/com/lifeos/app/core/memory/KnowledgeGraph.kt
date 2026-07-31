package com.lifeos.app.core.memory

import com.lifeos.app.core.data.MemoryEdgeDao
import com.lifeos.app.core.data.MemoryEdgeEntity
import com.lifeos.app.core.data.MemoryNodeDao
import com.lifeos.app.core.data.MemoryNodeEntity
import javax.inject.Inject

/** Memory §5's property-graph traversal — the lookup the Clarification Engine (a later phase) uses. */
class KnowledgeGraph @Inject constructor(
    private val nodeDao: MemoryNodeDao,
    private val edgeDao: MemoryEdgeDao,
) {
    suspend fun neighborsOf(nodeId: String): List<MemoryEdgeEntity> = edgeDao.neighborsOf(nodeId)

    suspend fun findByLabel(text: String): List<MemoryNodeEntity> = nodeDao.findByLabel(text)

    suspend fun link(edge: MemoryEdgeEntity) = edgeDao.upsert(edge)
}
