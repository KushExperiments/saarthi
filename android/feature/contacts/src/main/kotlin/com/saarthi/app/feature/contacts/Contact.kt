package com.saarthi.app.feature.contacts

import com.saarthi.app.core.data.ContactEntity

data class Contact(val id: String, val name: String, val phone: String)

internal fun ContactEntity.toDomain(): Contact = Contact(id, name, phone)
internal fun Contact.toEntity(): ContactEntity = ContactEntity(id, name, phone)
