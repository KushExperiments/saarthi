package com.lifeos.app.feature.voice

import com.lifeos.app.core.data.MemoryProvenanceEntity
import com.lifeos.app.core.memory.MemoryCategory
import com.lifeos.app.core.memory.MemorySource
import com.lifeos.app.core.memory.SensitivityTier

/** Health/relationships/life-story/safety facts get a spoken privacy note when remembered; low-stakes ones don't. */
fun isSensitiveCategory(category: MemoryCategory): Boolean =
    category.sensitivityTier == SensitivityTier.HIGH || category.sensitivityTier == SensitivityTier.CRITICAL

/** Turns a real stored provenance row into a spoken sentence — never a synthesized guess. */
fun provenanceExplanation(provenance: MemoryProvenanceEntity): String {
    val source = runCatching { MemorySource.valueOf(provenance.sourceType) }.getOrNull()
    return when (source) {
        MemorySource.USER_STATED -> "You told me that yourself."
        MemorySource.CAREGIVER_IMPORTED -> "A family member added that."
        MemorySource.AI_INFERRED -> "I noticed that — ${provenance.sourceDetail}"
        MemorySource.DENIED -> "That was marked as not true."
        null -> provenance.sourceDetail
    }
}
