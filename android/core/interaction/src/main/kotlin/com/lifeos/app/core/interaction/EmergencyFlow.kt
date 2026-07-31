package com.lifeos.app.core.interaction

/** A single scripted, fixed prompt — deterministic, never LLM-generated (Interaction OS §13). */
data class EmergencyPrompt(val text: String)

/**
 * Interaction OS §13 — hardcoded dialogue trees for specific emergency
 * scenarios. Deliberately NOT LLM-generated: an emergency script must be
 * predictable and pre-validated, not composed on the fly under pressure.
 */
sealed class EmergencyScript(val prompts: List<EmergencyPrompt>) {
    data object StrokeCheck : EmergencyScript(
        listOf(
            EmergencyPrompt("Can you smile for me?"),
            EmergencyPrompt("Can you raise both your arms?"),
            EmergencyPrompt("Try saying a short sentence for me."),
        ),
    )

    data object FallCheck : EmergencyScript(
        listOf(
            EmergencyPrompt("Are you able to get up?"),
            EmergencyPrompt("Are you hurt anywhere?"),
        ),
    )

    data object NoResponseCheck : EmergencyScript(
        listOf(
            EmergencyPrompt("Are you there? Please say something or tap the screen."),
        ),
    )
}

/** Any failed step means: stop asking, escalate immediately — never a third retry loop. */
const val EMERGENCY_ESCALATION_MESSAGE = "I'm calling for help right now. Stay where you are."
