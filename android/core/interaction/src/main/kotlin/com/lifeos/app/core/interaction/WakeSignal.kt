package com.lifeos.app.core.interaction

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A tiny cross-module signal so MainActivity (deliberately generic — it
 * iterates [FeatureNavigation] without knowing any feature by name, per
 * Architecture §3's "arrows only point toward foundation" rule) can tell
 * whichever feature owns voice interaction "the wake word fired, start
 * listening now," without either side depending on the other directly.
 * app depends on core:interaction, feature:voice depends on
 * core:interaction — this lives at their shared foundation.
 */
@Singleton
class WakeSignal @Inject constructor() {
    private val _triggered = MutableStateFlow(0L)

    /** Emits a new (non-zero, monotonically distinct) value each time [fire] is called. */
    val triggered: StateFlow<Long> = _triggered

    fun fire() {
        _triggered.value = System.currentTimeMillis()
    }
}
