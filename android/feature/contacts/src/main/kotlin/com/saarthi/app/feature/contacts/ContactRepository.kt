package com.saarthi.app.feature.contacts

import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun observeAll(): Flow<List<Contact>>
    suspend fun save(contact: Contact)
    suspend fun delete(contact: Contact)
}
