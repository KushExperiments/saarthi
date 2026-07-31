package com.lifeos.app.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/**
 * The contract every feature module implements to contribute its own
 * screens to the app's single [androidx.navigation.NavHost] — this is
 * what lets a new feature module (Reminders, Memory, Emergency, ...) be
 * added without any other feature module knowing it exists, per
 * Architecture §3's "arrows only point toward foundation" dependency rule.
 */
interface FeatureNavigation {
    /** The route this feature should be shown at when the app starts fresh. */
    val startDestination: LifeOSRoute?
        get() = null

    fun NavGraphBuilder.register(navController: NavHostController)
}
