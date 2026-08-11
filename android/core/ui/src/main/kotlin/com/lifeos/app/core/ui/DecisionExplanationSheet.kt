package com.lifeos.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A caller-supplied, plain DTO — deliberately not `core:cognitive`'s
 * `ActionPlan`/`Verdict` or `core:data`'s `DecisionTraceEntity` directly.
 * `core:ui` stays dependency-free of feature logic; the caller (which does
 * depend on those modules) maps into this shape.
 */
data class DecisionExplanation(
    val action: String,
    val confidence: Float,
    val reasoning: List<String>,
    val alternatives: List<String>,
)

/** "Why did Juno do that?" — Cognitive OS's Decision Traceability made legible, not just logged. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionExplanationSheet(
    explanation: DecisionExplanation,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(text = "Why did I do that?", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Action", style = MaterialTheme.typography.labelLarge)
            Text(text = explanation.action, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "How sure I was", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${(explanation.confidence * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyLarge,
            )

            if (explanation.reasoning.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Why", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    explanation.reasoning.forEach { reason ->
                        Text(text = "• $reason", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            if (explanation.alternatives.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Other options I considered", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    explanation.alternatives.forEach { alternative ->
                        Text(text = "• $alternative", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
