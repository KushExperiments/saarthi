package com.lifeos.app.core.cognitive

import com.lifeos.app.core.data.DecisionTraceEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomDecisionTraceReaderTest {

    private fun trace(id: String, timestamp: Long, action: String) = DecisionTraceEntity(
        id = id,
        timestamp = timestamp,
        reasoningTypesFiredCsv = "RULE|LLM",
        contextSummary = "night=false familyPresent=false emergency=false",
        memoryProvenanceIdsCsv = "",
        rejectedAlternativesCsv = "whatsapp:LLM|sms:RULE",
        chosenAction = action,
        chosenConfidence = 0.8f,
    )

    @Test
    fun `no traces at all returns null, not a crash`() = runTest {
        val reader = RoomDecisionTraceReader(FakeDecisionTraceDao())

        assertNull(reader.mostRecent())
    }

    @Test
    fun `returns the most recent trace, mapped and delimited fields split back into lists`() = runTest {
        val dao = FakeDecisionTraceDao()
        dao.insert(trace(id = "t1", timestamp = 1000L, action = "open_medicines"))
        dao.insert(trace(id = "t2", timestamp = 2000L, action = "call"))
        val reader = RoomDecisionTraceReader(dao)

        val result = reader.mostRecent()

        assertEquals("call", result?.action)
        assertEquals(0.8f, result?.confidence)
        assertEquals(listOf("RULE", "LLM"), result?.reasoningTypesFired)
        assertEquals(listOf("whatsapp:LLM", "sms:RULE"), result?.rejectedAlternatives)
    }
}
