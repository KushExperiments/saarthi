package com.saarthi.app.core.navigation

/**
 * Every feature module's screens implement this so the destination is a
 * typed, discoverable object rather than a raw string scattered across
 * modules.
 */
interface SaarthiRoute {
    val route: String
}
