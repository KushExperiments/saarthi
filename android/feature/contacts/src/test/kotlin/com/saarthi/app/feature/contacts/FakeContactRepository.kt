package com.saarthi.app.feature.contacts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeContactRepository : ContactRepository {
    private val state = MutableStateFlow<List<Contact>>(emptyList())

    override fun observeAll(): StateFlow<List<Contact>> = state

    override suspend fun save(contact: Contact) {
        state.value = state.value.filterNot { it.id == contact.id } + contact
    }

    override suspend fun delete(contact: Contact) {
        state.value = state.value.filterNot { it.id == contact.id }
    }
}
