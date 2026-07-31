package com.lifeos.app.feature.medicines

import android.app.TimePickerDialog
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.ui.LifeOSBadge
import com.lifeos.app.core.ui.LifeOSBadgeTone
import com.lifeos.app.core.ui.LifeOSButton
import com.lifeos.app.core.ui.LifeOSEmptyState
import com.lifeos.app.core.ui.LifeOSIconButton
import com.lifeos.app.core.ui.LifeOSListItem
import com.lifeos.app.core.ui.LifeOSSectionHeader
import java.util.Calendar

@Composable
fun MedicinesListScreen(viewModel: MedicinesViewModel = hiltViewModel()) {
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        LifeOSSectionHeader(title = "My Medicines")
        Spacer(modifier = Modifier.height(16.dp))

        if (medicines.isEmpty()) {
            LifeOSEmptyState(
                message = "No medicines yet. Tap the button below to add one.",
                emoji = "💊",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(medicines, key = { it.id }) { medicine ->
                    MedicineRow(
                        medicine = medicine,
                        onTaken = { viewModel.markTaken(medicine) },
                        onDelete = { viewModel.delete(medicine) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LifeOSButton(text = "➕ Add a medicine", onClick = { showAdd = true })
    }

    if (showAdd) {
        AddMedicineDialog(
            onDismiss = { showAdd = false },
            onSave = { name, times ->
                viewModel.addMedicine(name, times)
                showAdd = false
            },
        )
    }
}

@Composable
private fun MedicineRow(medicine: Medicine, onTaken: () -> Unit, onDelete: () -> Unit) {
    val allConfirmed = medicine.times.all { medicine.isConfirmed(it) }
    LifeOSListItem(
        title = medicine.name,
        subtitle = medicine.times.joinToString("  •  ") { fmt12(it) },
        leading = { Text(text = "💊", style = MaterialTheme.typography.titleLarge) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (allConfirmed) {
                    LifeOSBadge(text = "Taken ✓", tone = LifeOSBadgeTone.NEUTRAL)
                } else {
                    TextButton(onClick = onTaken) { Text("✅ Took it") }
                }
                LifeOSIconButton(glyph = "🗑️", contentDescription = "Delete ${medicine.name}", onClick = onDelete)
            }
        },
    )
}

@Composable
private fun AddMedicineDialog(onDismiss: () -> Unit, onSave: (String, List<String>) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    val times = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a medicine") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Times to take:")
                times.forEach { Text("• ${fmt12(it)}") }
                TextButton(onClick = {
                    val now = Calendar.getInstance()
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            val t = "%02d:%02d".format(hour, minute)
                            if (!times.contains(t)) times.add(t)
                        },
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        false,
                    ).show()
                }) { Text("➕ Add a time") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && times.isNotEmpty(),
                onClick = { onSave(name, times.toList()) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun fmt12(hm: String): String {
    val parts = hm.split(":")
    val hour = parts[0].toInt()
    val minute = parts.getOrElse(1) { "00" }
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = (hour % 12).let { if (it == 0) 12 else it }
    return "$hour12:$minute $amPm"
}
