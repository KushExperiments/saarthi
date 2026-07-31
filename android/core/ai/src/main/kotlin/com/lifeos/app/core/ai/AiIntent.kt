package com.lifeos.app.core.ai

/**
 * What the AI understood the elder wants. Ported from the legacy prototype's
 * `com.lifeos.app.Ai.AiIntent` — same shape, same action vocabulary, now
 * the seed for core:cognitive's PolicyAllowlist.
 */
data class AiIntent(
    val action: String, // call, whatsapp, sms, torch_on, torch_off, youtube,
                         // volume_up, volume_down, volume_max, medicine_taken,
                         // open_medicines, help, answer, none
    val person: String = "",
    val query: String = "",
    val message: String = "",
    val reply: String = "",
)
