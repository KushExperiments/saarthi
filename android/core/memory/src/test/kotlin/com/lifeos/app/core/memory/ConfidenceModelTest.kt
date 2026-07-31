package com.lifeos.app.core.memory

import com.lifeos.app.core.data.MemoryNodeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfidenceModelTest {

    @Test
    fun `each source maps to its documented confidence value`() {
        assertEquals(0.95f, ConfidenceModel.confidenceFor(MemorySource.USER_STATED))
        assertEquals(0.90f, ConfidenceModel.confidenceFor(MemorySource.CAREGIVER_IMPORTED))
        assertEquals(0.40f, ConfidenceModel.confidenceFor(MemorySource.AI_INFERRED))
        assertEquals(0.0f, ConfidenceModel.confidenceFor(MemorySource.DENIED))
    }

    @Test
    fun `correct always resets confidence to USER_STATED even from a lower-confidence node`() {
        val inferred = MemoryNodeEntity(
            id = "n1",
            category = MemoryCategory.INTERESTS.name,
            label = "favorite drink",
            valueText = "coffee",
            sensitivityTier = MemoryCategory.INTERESTS.sensitivityTier.name,
            confidence = ConfidenceModel.AI_INFERRED,
            source = MemorySource.AI_INFERRED.name,
            createdAt = 0L,
            updatedAt = 0L,
            validFrom = 0L,
        )

        val corrected = ConfidenceModel.correct(inferred, newValue = "tea", now = 1000L)

        assertEquals("tea", corrected.valueText)
        assertEquals(ConfidenceModel.USER_STATED, corrected.confidence)
        assertEquals(MemorySource.USER_STATED.name, corrected.source)
        assertEquals(1000L, corrected.updatedAt)
    }
}
