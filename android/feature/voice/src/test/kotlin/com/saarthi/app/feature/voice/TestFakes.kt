package com.saarthi.app.feature.voice

import com.saarthi.app.feature.contacts.Contact
import com.saarthi.app.feature.contacts.ContactRepository
import com.saarthi.app.feature.medicines.Medicine
import com.saarthi.app.feature.medicines.MedicineRepository
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
