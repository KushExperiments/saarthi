package com.lifeos.app.core.navigation

/**
 * Every feature module's screens implement this so the destination is a
 * typed, discoverable object rather than a raw string scattered across
 * modules.
 */
interface LifeOSRoute {
    val route: String
}
