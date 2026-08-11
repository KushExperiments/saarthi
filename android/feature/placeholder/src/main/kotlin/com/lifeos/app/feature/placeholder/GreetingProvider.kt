package com.lifeos.app.feature.placeholder

import javax.inject.Inject

/**
 * Deliberately trivial — its only job is to prove constructor injection
 * resolves correctly end-to-end (app → Hilt → ViewModel → screen) before
 * any real business logic exists. Later modules replace this with real
 * injected repositories/use-cases.
 */
class GreetingProvider @Inject constructor() {
    fun greet(): String = "Juno's foundation is running."
}
