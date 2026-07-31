package com.lifeos.app.core.interaction

import com.lifeos.app.core.data.MemoryNodeEntity
import com.lifeos.app.core.memory.KnowledgeGraph
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClarificationEngineTest {

    private fun memoryNode(id: String, label: String) = MemoryNodeEntity(
        id = id,
        category = "RELATIONSHIPS",
        label = label,
        valueText = label,
        sensitivityTier = "HIGH",
        confidence = 0.9f,
        source = "USER_STATED",
        createdAt = 0L,
        updatedAt = 0L,
        validFrom = 0L,
    )

    @Test
    fun `an unresolved name with no known entities and no memory match asks to add them`() = runTest {
        val engine = KnowledgeGraphClarificationEngine(KnowledgeGraph(FakeMemoryNodeDao(), FakeMemoryEdgeDao()))

        val result = engine.clarify(spokenName = "Rohan", knownEntities = emptyList())

        assertTrue(result.candidates.isEmpty())
        assertTrue(result.question.contains("add"))
    }

    @Test
    fun `a single matching known entity produces a narrow yes-or-no question, not a generic one`() = runTest {
        val engine = KnowledgeGraphClarificationEngine(KnowledgeGraph(FakeMemoryNodeDao(), FakeMemoryEdgeDao()))
        val beta = NamedEntity(id = "1", name = "Beta", kind = "contact")

        val result = engine.clarify(spokenName = "beta", knownEntities = listOf(beta))

        assertEquals(listOf(beta), result.candidates)
        assertEquals("Did you mean Beta?", result.question)
    }

    @Test
    fun `ambiguous known entities narrow to at most two candidates`() = runTest {
        val engine = KnowledgeGraphClarificationEngine(KnowledgeGraph(FakeMemoryNodeDao(), FakeMemoryEdgeDao()))
        val entities = listOf(
            NamedEntity(id = "1", name = "Vishal Senior", kind = "contact"),
            NamedEntity(id = "2", name = "Vishal Junior", kind = "contact"),
            NamedEntity(id = "3", name = "Vishal Uncle", kind = "contact"),
        )

        val result = engine.clarify(spokenName = "vishal", knownEntities = entities)

        assertEquals(2, result.candidates.size)
    }

    @Test
    fun `a name absent from known entities but present in the Knowledge Graph still surfaces a candidate`() = runTest {
        val nodeDao = FakeMemoryNodeDao(seed = listOf(memoryNode(id = "n1", label = "Priya")))
        val engine = KnowledgeGraphClarificationEngine(KnowledgeGraph(nodeDao, FakeMemoryEdgeDao()))

        val result = engine.clarify(spokenName = "priya", knownEntities = emptyList())

        assertEquals(1, result.candidates.size)
        assertEquals("Priya", result.candidates.first().name)
    }

    @Test
    fun `no spoken name at all still offers up to two known entities rather than asking a generic question`() = runTest {
        val engine = KnowledgeGraphClarificationEngine(KnowledgeGraph(FakeMemoryNodeDao(), FakeMemoryEdgeDao()))
        val entities = listOf(
            NamedEntity(id = "1", name = "Beta", kind = "contact"),
            NamedEntity(id = "2", name = "Doctor", kind = "contact"),
        )

        val result = engine.clarify(spokenName = null, knownEntities = entities)

        assertEquals(2, result.candidates.size)
        assertEquals("Did you mean Beta or Doctor?", result.question)
    }
}
