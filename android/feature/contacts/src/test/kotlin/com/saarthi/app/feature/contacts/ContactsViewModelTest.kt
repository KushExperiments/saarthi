package com.saarthi.app.feature.contacts

import com.saarthi.app.core.testing.MainDispatcherRule
import com.saarthi.app.core.testing.TestDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ContactsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeContactRepository
    private lateinit var viewModel: ContactsViewModel

    @Before
    fun setUp() {
        // Built after the rule installs the test Main dispatcher (not as a field
        // initializer, which JUnit4 runs before @Rule.starting()) so this ViewModel's
        // eager stateIn(..., SharingStarted.Eagerly, ...) dispatches on the right Main.
        repository = FakeContactRepository()
        viewModel = ContactsViewModel(repository, TestDispatcherProvider())
    }

    @Test
    fun `addContact ignores a blank name or phone`() {
        viewModel.addContact(name = "  ", phone = "12345")
        viewModel.addContact(name = "Beta", phone = "  ")

        assertTrue(viewModel.contacts.value.isEmpty())
    }

    @Test
    fun `addContact saves a trimmed contact`() {
        viewModel.addContact(name = "  Beta  ", phone = "  +911234567890  ")

        val saved = viewModel.contacts.value.single()
        assertEquals("Beta", saved.name)
        assertEquals("+911234567890", saved.phone)
    }

    @Test
    fun `delete removes the contact`() {
        viewModel.addContact(name = "Beta", phone = "+911234567890")
        val contact = viewModel.contacts.value.single()

        viewModel.delete(contact)

        assertTrue(viewModel.contacts.value.isEmpty())
    }
}
