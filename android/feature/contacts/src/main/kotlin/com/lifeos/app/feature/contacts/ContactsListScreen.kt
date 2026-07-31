package com.lifeos.app.feature.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.ui.LifeOSButton
import com.lifeos.app.core.ui.LifeOSEmptyState
import com.lifeos.app.core.ui.LifeOSIconButton
import com.lifeos.app.core.ui.LifeOSListItem
import com.lifeos.app.core.ui.LifeOSSectionHeader

@Composable
fun ContactsListScreen(viewModel: ContactsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        LifeOSSectionHeader(title = "Call Someone")
        Spacer(modifier = Modifier.height(16.dp))

        if (contacts.isEmpty()) {
            LifeOSEmptyState(
                message = "No people yet. Add family so you can say \"call beta\".",
                emoji = "👨‍👩‍👧",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(contacts, key = { it.id }) { contact ->
                    ContactRow(
                        contact = contact,
                        onCall = { ContactActions.dial(context, contact) },
                        onWhatsApp = { ContactActions.openWhatsApp(context, contact) },
                        onDelete = { viewModel.delete(contact) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LifeOSButton(text = "➕ Add a person", onClick = { showAdd = true })
    }

    if (showAdd) {
        AddContactDialog(
            onDismiss = { showAdd = false },
            onSave = { name, phone ->
                viewModel.addContact(name, phone)
                showAdd = false
            },
        )
    }
}

@Composable
private fun ContactRow(contact: Contact, onCall: () -> Unit, onWhatsApp: () -> Unit, onDelete: () -> Unit) {
    LifeOSListItem(
        title = contact.name,
        subtitle = contact.phone,
        leading = { Text(text = "👤", style = MaterialTheme.typography.titleLarge) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LifeOSIconButton(glyph = "📞", contentDescription = "Call ${contact.name}", onClick = onCall)
                LifeOSIconButton(glyph = "🟢", contentDescription = "WhatsApp ${contact.name}", onClick = onWhatsApp)
                LifeOSIconButton(glyph = "🗑️", contentDescription = "Delete ${contact.name}", onClick = onDelete)
            }
        },
    )
}

@Composable
private fun AddContactDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a person") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Beta, Vishal, Doctor)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (with country code)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && phone.isNotBlank(),
                onClick = { onSave(name, phone) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
