package com.lifeos.app.feature.voice

import com.lifeos.app.core.data.MemoryNodeEntity
import com.lifeos.app.core.data.MemoryProvenanceEntity
import com.lifeos.app.core.memory.MemoryCategory
import com.lifeos.app.core.memory.MemoryRepository
import com.lifeos.app.core.memory.MemorySource
import com.lifeos.app.core.memory.ScoredMemory
import com.lifeos.app.feature.contacts.Contact
import com.lifeos.app.feature.contacts.ContactRepository
import com.lifeos.app.feature.medicines.Medicine
import com.lifeos.app.feature.medicines.MedicineRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Local fakes, deliberately not shared from feature:medicines/feature:contacts'
 * own test source sets — Gradle doesn't expose one module's `test` source set
 * to another module's tests (only `main` is a consumable dependency), so a
 * cross-module test import would fail to resolve. Small enough to duplicate
 * safely rather than reach for AGP test-fixtures, which carries its own
 * unverified version-compat risk.
 */
class LocalFakeMedicineRepository : MedicineRepository {
    private val state = MutableStateFlow<List<Medicine>>(emptyList())
    override fun observeAll(): StateFlow<List<Medicine>> = state
    override suspend fun getById(id: String): Medicine? = state.value.firstOrNull { it.id == id }
    override suspend fun save(medicine: Medicine) {
        state.value = state.value.filterNot { it.id == medicine.id } + medicine
    }
    override suspend fun delete(medicine: Medicine) {
        state.value = state.value.filterNot { it.id == medicine.id }
    }
    override suspend fun markTaken(medicineId: String, time: String) {
        val current = state.value.firstOrNull { it.id == medicineId } ?: return
        val today = Medicine.today()
        val alreadyToday = current.confirmedDate == today
        save(
            current.copy(
                confirmedDate = today,
                confirmedTimes = (if (alreadyToday) current.confirmedTimes + time else listOf(time)).distinct(),
            ),
        )
    }
}

class LocalFakeContactRepository : ContactRepository {
    private val state = MutableStateFlow<List<Contact>>(emptyList())
    override fun observeAll(): StateFlow<List<Contact>> = state
    override suspend fun save(contact: Contact) {
        state.value = state.value.filterNot { it.id == contact.id } + contact
    }
    override suspend fun delete(contact: Contact) {
        state.value = state.value.filterNot { it.id == contact.id }
    }
}

/** A simple, realistic-enough fake — substring match for recall, real confidence/provenance bookkeeping. */
class LocalFakeMemoryRepository : MemoryRepository {
    private val nodes = MutableStateFlow<List<MemoryNodeEntity>>(emptyList())
    private val provenanceByNodeId = mutableMapOf<String, MutableList<MemoryProvenanceEntity>>()

    override fun observeAll(): StateFlow<List<MemoryNodeEntity>> = nodes

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
            confidence = 0.95f,
            source = source.name,
            createdAt = now,
            updatedAt = now,
            validFrom = now,
        )
        nodes.value = listOf(node) + nodes.value
        provenanceByNodeId.getOrPut(node.id) { mutableListOf() }.add(
            MemoryProvenanceEntity(id = UUID.randomUUID().toString(), nodeId = node.id, sourceType = source.name, sourceDetail = sourceDetail, recordedAt = now),
        )
        return node
    }

    override suspend fun recall(query: String, categoryHint: MemoryCategory?, now: Long): List<ScoredMemory> {
        val matches = if (query.isBlank()) {
            nodes.value
        } else {
            nodes.value.filter { it.label.contains(query, ignoreCase = true) || it.valueText.contains(query, ignoreCase = true) }
        }
        return matches.sortedByDescending { it.updatedAt }.map { ScoredMemory(node = it, score = 1f, breakdown = emptyMap()) }
    }

    override suspend fun forget(factId: String, reason: String, now: Long) {
        nodes.value = nodes.value.filterNot { it.id == factId }
    }

    override suspend fun correct(factId: String, newValue: String, now: Long): MemoryNodeEntity? {
        val existing = nodes.value.find { it.id == factId } ?: return null
        val corrected = existing.copy(valueText = newValue, confidence = 0.95f, updatedAt = now)
        nodes.value = nodes.value.map { if (it.id == factId) corrected else it }
        return corrected
    }

    override suspend fun whyRemembered(factId: String): List<MemoryProvenanceEntity> = provenanceByNodeId[factId].orEmpty()
}
