package com.saarthi.app.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saarthi.app.core.common.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repository: ContactRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    val contacts: StateFlow<List<Contact>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addContact(name: String, phone: String) {
        if (name.isBlank() || phone.isBlank()) return
        viewModelScope.launch(dispatchers.io) {
            repository.save(Contact(id = UUID.randomUUID().toString(), name = name.trim(), phone = phone.trim()))
        }
    }

    fun delete(contact: Contact) {
        viewModelScope.launch(dispatchers.io) {
            repository.delete(contact)
        }
    }
}
