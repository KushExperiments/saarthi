package com.lifeos.app.feature.voice

import com.lifeos.app.core.cognitive.DecisionContext
import com.lifeos.app.core.cognitive.DecisionEngine
import com.lifeos.app.core.cognitive.DecisionInput
import com.lifeos.app.core.interaction.ClarificationEngine
import com.lifeos.app.core.interaction.DialogueContext
import com.lifeos.app.core.interaction.DialogueManager
import com.lifeos.app.core.interaction.DialogueResult
import javax.inject.Inject

/**
 * The concrete [DialogueManager] for voice input. [CommandRouter] (unmoved,
 * still living in this module) stays the fast, deterministic floor tier —
 * this only reaches into Cognitive OS's [DecisionEngine] for whatever
 * CommandRouter doesn't recognize, matching the same "rule engine as
 * always-present safety substrate, LLM as fallback" shape used throughout
 * this project.
 */
class VoiceDialogueManager @Inject constructor(
    private val decisionEngine: DecisionEngine,
    private val clarificationEngine: ClarificationEngine,
) : DialogueManager {

    override suspend fun handle(transcript: String, context: DialogueContext): DialogueResult {
        val plan = decisionEngine.decide(
            DecisionInput(
                transcript = transcript,
                knownPeople = context.knownEntities.map { it.name },
                context = DecisionContext(now = System.currentTimeMillis()),
            ),
        )

        return when {
            plan.action == "needs_contact" -> clarificationEngine.clarify(spokenName = null, context.knownEntities)
            plan.action == "answer" -> DialogueResult.Respond(plan.params["reply"] ?: "I'm not sure.")
            plan.action == "none" -> DialogueResult.Unhandled
            else -> DialogueResult.Act(plan)
        }
    }
}
