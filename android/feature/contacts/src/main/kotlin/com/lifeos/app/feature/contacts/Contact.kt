package com.lifeos.app.feature.contacts

import com.lifeos.app.core.data.ContactEntity

data class Contact(val id: String, val name: String, val phone: String)

internal fun ContactEntity.toDomain(): Contact = Contact(id, name, phone)
internal fun Contact.toEntity(): ContactEntity = ContactEntity(id, name, phone)
