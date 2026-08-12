package com.lifeos.app.feature.voice

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.data.MemoryNodeEntity
import com.lifeos.app.core.ui.LifeOSEmptyState
import com.lifeos.app.core.ui.LifeOSIconButton
import com.lifeos.app.core.ui.LifeOSListItem
import com.lifeos.app.core.ui.LifeOSSectionHeader

/**
 * "What I Remember" — every active memory, most recently updated first,
 * with real provenance visible (category + confidence, not hidden behind
 * a tap) and Correct/Forget as first-class actions, per M-002. Voice-
 * reachable ("show me my important memories"), matching Settings' own
 * "not a home-screen button" convention.
 */
@Composable
fun MemoryScreen(viewModel: MemoryViewModel = hiltViewModel()) {
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<MemoryNodeEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        LifeOSSectionHeader(title = "What I Remember")
        Spacer(modifier = Modifier.height(16.dp))

        if (memories.isEmpty()) {
            LifeOSEmptyState(
                message = "Nothing remembered yet. Just tell me things like \"remember that my daughter lives in Pune.\"",
                emoji = "🧠",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(memories, key = { it.id }) { memory ->
                    MemoryRow(
                        memory = memory,
                        onCorrect = { editing = memory },
                        onForget = { viewModel.forget(memory) },
                    )
                }
            }
        }
    }

    val current = editing
    if (current != null) {
        CorrectMemoryDialog(
            memory = current,
            onDismiss = { editing = null },
            onSave = { newValue ->
                viewModel.correct(current, newValue)
                editing = null
            },
        )
    }
}

@Composable
private fun MemoryRow(memory: MemoryNodeEntity, onCorrect: () -> Unit, onForget: () -> Unit) {
    val confidencePercent = (memory.confidence * 100).toInt()
    val categoryLabel = memory.category.lowercase().replace('_', ' ')
    LifeOSListItem(
        title = memory.valueText,
        subtitle = "$categoryLabel  •  $confidencePercent% confidence",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LifeOSIconButton(glyph = "✏️", contentDescription = "Correct this memory", onClick = onCorrect)
                LifeOSIconButton(glyph = "🗑️", contentDescription = "Forget this memory", onClick = onForget)
            }
        },
    )
}

@Composable
private fun CorrectMemoryDialog(memory: MemoryNodeEntity, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(memory.valueText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Correct this memory") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onSave(value) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
