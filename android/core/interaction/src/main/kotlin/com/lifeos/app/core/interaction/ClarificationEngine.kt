package com.lifeos.app.core.interaction

import com.lifeos.app.core.memory.KnowledgeGraph
import javax.inject.Inject

private const val MAX_CANDIDATES = 2

interface ClarificationEngine {
    /** Never a generic "I didn't understand" — always a narrow, answerable question. */
    suspend fun clarify(spokenName: String?, knownEntities: List<NamedEntity>): DialogueResult.Clarify
}

/**
 * Interaction OS §7 — narrows toward at most [MAX_CANDIDATES] candidates,
 * drawn first from what the caller already knows about this conversation
 * ([knownEntities], e.g. saved contacts), and — when that alone doesn't
 * narrow it down — cross-referenced against Memory's Knowledge Graph
 * (Memory §5) for a name the elder has mentioned before but hasn't been
 * formally added yet.
 */
class KnowledgeGraphClarificationEngine @Inject constructor(
    private val knowledgeGraph: KnowledgeGraph,
) : ClarificationEngine {

    override suspend fun clarify(spokenName: String?, knownEntities: List<NamedEntity>): DialogueResult.Clarify {
        val candidates = when {
            spokenName.isNullOrBlank() -> knownEntities.take(MAX_CANDIDATES)
            else -> {
                val directMatches = knownEntities.filter { it.name.contains(spokenName, ignoreCase = true) }
                directMatches.ifEmpty {
                    knowledgeGraph.findByLabel(spokenName)
                        .map { NamedEntity(id = it.id, name = it.label, kind = "memory") }
                }
            }
        }.take(MAX_CANDIDATES)

        val question = when (candidates.size) {
            0 -> "I'm not sure who you mean — could you add them first?"
            1 -> "Did you mean ${candidates.first().name}?"
            else -> "Did you mean ${candidates.joinToString(" or ") { it.name }}?"
        }
        return DialogueResult.Clarify(question, candidates)
    }
}
