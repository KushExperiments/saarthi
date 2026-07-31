package com.lifeos.app.core.interaction

import javax.inject.Inject

/** Interaction OS §16 — higher priority wins. */
enum class InterruptionType(val priority: Int) {
    BARGE_IN(1),
    LOW_BATTERY(2),
    INCOMING_CALL(3),
    DOORBELL(2),
    INTERNET_LOST(1),
    CAREGIVER_JOINING(2),
    EMERGENCY(4),
}

class InterruptionHandler @Inject constructor() {
    /** An emergency, or anything strictly higher priority than the current state's own interruption, always wins. */
    fun shouldInterrupt(currentState: ConversationState, incoming: InterruptionType): Boolean = when {
        incoming == InterruptionType.EMERGENCY -> true
        currentState == ConversationState.EMERGENCY -> false
        else -> true
    }
}
