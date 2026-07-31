package com.lifeos.app.core.memory

import com.lifeos.app.core.data.MemoryNodeEntity

/**
 * Memory §10's confidence propagation rules. [correct] always resets
 * confidence to [USER_STATED] immediately, overriding whatever accumulated
 * before it — hardcoded, not a tunable, because this is a trust property
 * ("a person should never have to argue with their own memory system"),
 * not a UX preference.
 */
object ConfidenceModel {
    const val USER_STATED = 0.95f
    const val CAREGIVER_IMPORTED = 0.90f
    const val AI_INFERRED = 0.40f
    const val DENIED = 0.0f

    fun confidenceFor(source: MemorySource): Float = when (source) {
        MemorySource.USER_STATED -> USER_STATED
        MemorySource.CAREGIVER_IMPORTED -> CAREGIVER_IMPORTED
        MemorySource.AI_INFERRED -> AI_INFERRED
        MemorySource.DENIED -> DENIED
    }

    fun correct(node: MemoryNodeEntity, newValue: String, now: Long): MemoryNodeEntity = node.copy(
        valueText = newValue,
        confidence = USER_STATED,
        source = MemorySource.USER_STATED.name,
        updatedAt = now,
    )
}
