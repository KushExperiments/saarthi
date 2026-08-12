package com.lifeos.app.feature.voice

/**
 * What Juno's Presence should show right now — see docs/adr/0001 and the
 * approved 2026-08-12 redesign spec. Ten states, matching the spec 1:1.
 */
enum class PresenceState {
    IDLE,
    LISTENING,
    WAKE_WORD,
    THINKING,
    SPEAKING,
    PROCESSING_ACTION,
    SUCCESS,
    ERROR,
    OFFLINE,
    EMERGENCY,
}
