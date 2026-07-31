package com.lifeos.app.core.cognitive

/**
 * Fixed allowlist of actions the Decision Engine may ever choose — seeded
 * from core:ai's AiIntent vocabulary. Anything not on this list is
 * rejected by [SafetyValidator]'s PolicyAllowlistStage regardless of how
 * confident a generator was.
 */
object PolicyAllowlist {
    val ALLOWED_ACTIONS = setOf(
        "call",
        "whatsapp",
        "sms",
        "torch_on",
        "torch_off",
        "youtube",
        "volume_up",
        "volume_down",
        "volume_max",
        "medicine_taken",
        "open_medicines",
        "open_settings",
        "help",
        "answer",
        "needs_contact",
        "none",
    )

    fun isAllowed(action: String): Boolean = action in ALLOWED_ACTIONS
}
