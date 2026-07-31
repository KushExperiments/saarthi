package com.lifeos.app.core.interaction

/** Interaction OS §5 — owned deterministically by the Conversation module, never agent-inferred. */
enum class ConversationState {
    IDLE,
    LISTENING,
    UNDERSTANDING,
    CLARIFYING,
    THINKING,
    EXECUTING,
    WAITING,
    REMINDING,
    EMERGENCY,
    INTERRUPTED,
    CANCELLED,
    GOODBYE,
}
