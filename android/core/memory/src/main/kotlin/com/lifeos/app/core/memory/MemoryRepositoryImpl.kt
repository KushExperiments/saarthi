package com.lifeos.app.core.memory

import com.lifeos.app.core.data.MemoryAuditLogDao
import com.lifeos.app.core.data.MemoryAuditLogEntity
import com.lifeos.app.core.data.MemoryNodeDao
import com.lifeos.app.core.data.MemoryNodeEntity
import com.lifeos.app.core.data.MemoryProvenanceDao
import com.lifeos.app.core.data.MemoryProvenanceEntity
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

private object AuditAction {
    const val CREATED = "CREATED"
    const val CORRECTED = "CORRECTED"
    const val FORGOTTEN = "FORGOTTEN"
}

class MemoryRepositoryImpl @Inject constructor(
    private val nodeDao: MemoryNodeDao,
    private val provenanceDao: MemoryProvenanceDao,
    private val auditLogDao: MemoryAuditLogDao,
    private val retrievalEngine: RetrievalEngine,
) : MemoryRepository {

    override fun observeAll() = nodeDao.observeAll()

    override suspend fun remember(
        category: MemoryCategory,
        label: String,
        valueText: String,
        source: MemorySource,
        sourceDetail: String,
        now: Long,
    ): MemoryNodeEntity {
        val node = MemoryNodeEntity(
            id = UUID.randomUUID().toString(),
            category = category.name,
            label = label,
            valueText = valueText,
            sensitivityTier = category.sensitivityTier.name,
            confidence = ConfidenceModel.confidenceFor(source),
            source = source.name,
            createdAt = now,
            updatedAt = now,
            validFrom = now,
        )
        nodeDao.upsert(node)
        provenanceDao.insert(
            MemoryProvenanceEntity(
                id = UUID.randomUUID().toString(),
                nodeId = node.id,
                sourceType = source.name,
                sourceDetail = sourceDetail,
                recordedAt = now,
            ),
        )
        auditLogDao.insert(
            MemoryAuditLogEntity(
                id = UUID.randomUUID().toString(),
                nodeId = node.id,
                action = AuditAction.CREATED,
                actor = "USER",
                reason = sourceDetail,
                timestamp = now,
            ),
        )
        return node
    }

    override suspend fun recall(query: String, categoryHint: MemoryCategory?, now: Long): List<ScoredMemory> {
        val candidates = nodeDao.observeAll().first()
        return retrievalEngine.rank(query, candidates, categoryHint, now)
    }

    override suspend fun forget(factId: String, reason: String, now: Long) {
        val node = nodeDao.getById(factId) ?: return
        nodeDao.upsert(node.copy(active = false, updatedAt = now))
        auditLogDao.insert(
            MemoryAuditLogEntity(
                id = UUID.randomUUID().toString(),
                nodeId = factId,
                action = AuditAction.FORGOTTEN,
                actor = "USER",
                reason = reason,
                timestamp = now,
            ),
        )
    }

    override suspend fun correct(factId: String, newValue: String, now: Long): MemoryNodeEntity? {
        val node = nodeDao.getById(factId) ?: return null
        val corrected = ConfidenceModel.correct(node, newValue, now)
        nodeDao.upsert(corrected)
        provenanceDao.insert(
            MemoryProvenanceEntity(
                id = UUID.randomUUID().toString(),
                nodeId = factId,
                sourceType = MemorySource.USER_STATED.name,
                sourceDetail = "User corrected a previous value",
                recordedAt = now,
            ),
        )
        auditLogDao.insert(
            MemoryAuditLogEntity(
                id = UUID.randomUUID().toString(),
                nodeId = factId,
                action = AuditAction.CORRECTED,
                actor = "USER",
                reason = "Corrected from \"${node.valueText}\" to \"$newValue\"",
                timestamp = now,
            ),
        )
        return corrected
    }

    override suspend fun whyRemembered(factId: String): List<MemoryProvenanceEntity> = provenanceDao.forNode(factId)
}
