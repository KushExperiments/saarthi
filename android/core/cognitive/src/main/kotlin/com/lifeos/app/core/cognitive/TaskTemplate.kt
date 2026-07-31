package com.lifeos.app.core.cognitive

data class Subtask(val description: String, val done: Boolean = false)

/**
 * Cognitive OS §6 — HTN for exactly two known templates, no general
 * solver. GOAP is explicitly rejected by the design doc as "computationally
 * open-ended, unpredictable — wrong fit for a safety-critical domain."
 * Expand this sealed hierarchy incrementally as real templates are needed,
 * never build a generic planner speculatively.
 */
sealed interface TaskTemplate {
    val subtasks: List<Subtask>

    data class MedicineRoutine(val medicineName: String, val time: String) : TaskTemplate {
        override val subtasks = listOf(
            Subtask("Remind at scheduled time"),
            Subtask("Wait for confirmation"),
            Subtask("Escalate if unconfirmed"),
            Subtask("Log outcome"),
        )
    }

    data class AppointmentPrep(val description: String, val at: Long) : TaskTemplate {
        override val subtasks = listOf(
            Subtask("Remind the day before"),
            Subtask("Remind the morning of"),
            Subtask("Offer to help arrange transport"),
        )
    }
}
