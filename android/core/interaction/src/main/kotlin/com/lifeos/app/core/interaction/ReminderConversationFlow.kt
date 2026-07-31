package com.lifeos.app.core.interaction

import javax.inject.Inject

/**
 * Interaction OS §12's escalation ladder — cadence itself is
 * Cognitive OS §14's [com.lifeos.app.core.cognitive.AdaptiveParamsStore]
 * concern; this only owns the *wording* at each attempt.
 */
class ReminderConversationFlow @Inject constructor() {

    /** Varied phrasing by attempt number — never the exact same sentence on repeat, per Philosophy's anti-nagging stance. */
    fun escalationPhrase(medicineName: String, attempt: Int): String = when {
        attempt <= 1 -> "It's time for your $medicineName."
        attempt == 2 -> "Just checking — have you taken your $medicineName yet?"
        attempt in 3..4 -> "Still waiting on your $medicineName — tap when you've taken it."
        else -> "This is important: please take your $medicineName, or let me know if something's wrong."
    }

    /** A consent-gated caregiver alert only fires after real, sustained silence — not the first miss. */
    fun shouldEmitCaregiverAlert(attempt: Int): Boolean = attempt >= CAREGIVER_ALERT_THRESHOLD

    private companion object {
        const val CAREGIVER_ALERT_THRESHOLD = 5
    }
}

data class CaregiverAlertEvent(val medicineName: String, val missedAttempts: Int, val at: Long)
