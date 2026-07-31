package com.lifeos.app.core.cognitive

import javax.inject.Inject

/** A signal that interrupts an in-progress [TaskTemplate] — the reactive layer next to the HTN templates. */
data class InterruptionSignal(val reason: String, val at: Long)

/**
 * Advances a [TaskTemplate] through its fixed subtask list. Deliberately
 * NOT a general planner — [replan] handles interruption by restarting the
 * current subtask, not by re-deriving a new plan from scratch (that's the
 * reactive behavior-tree layer's job, kept intentionally simple here).
 */
class HtnTaskPlanner @Inject constructor() {
    fun advance(template: TaskTemplate, completedCount: Int): List<Subtask> =
        template.subtasks.mapIndexed { index, subtask -> subtask.copy(done = index < completedCount) }

    fun isComplete(template: TaskTemplate, completedCount: Int): Boolean = completedCount >= template.subtasks.size

    /** Emergency tasks bypass this entirely (compressed path) — this only ever restarts the current step. */
    fun replan(template: TaskTemplate, completedCount: Int, interruption: InterruptionSignal): Int =
        completedCount.coerceAtMost(template.subtasks.size - 1).coerceAtLeast(0)
}
