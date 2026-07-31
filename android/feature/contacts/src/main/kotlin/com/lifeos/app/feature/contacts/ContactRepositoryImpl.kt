package com.lifeos.app.feature.contacts

import com.lifeos.app.core.data.ContactDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val dao: ContactDao,
) : ContactRepository {
    override fun observeAll(): Flow<List<Contact>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(contact: Contact) = dao.upsert(contact.toEntity())

    override suspend fun delete(contact: Contact) = dao.delete(contact.toEntity())
}
