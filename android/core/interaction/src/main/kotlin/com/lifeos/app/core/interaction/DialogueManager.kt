package com.lifeos.app.core.interaction

import com.lifeos.app.core.cognitive.ActionPlan

/** Generic over [NamedEntity] rather than a concrete `Contact` type — core:interaction can't depend on feature:contacts. */
data class NamedEntity(val id: String, val name: String, val kind: String)

data class DialogueContext(
    val knownEntities: List<NamedEntity> = emptyList(),
    val conversationHistory: List<String> = emptyList(),
)

sealed interface DialogueResult {
    data class Clarify(val question: String, val candidates: List<NamedEntity>) : DialogueResult
    data class Respond(val text: String) : DialogueResult
    data class Act(val plan: ActionPlan) : DialogueResult
    data object Unhandled : DialogueResult
}

interface DialogueManager {
    suspend fun handle(transcript: String, context: DialogueContext): DialogueResult
}
