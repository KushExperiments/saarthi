package com.saarthi.app.feature.medicines

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.saarthi.app.core.navigation.FeatureNavigation
import com.saarthi.app.core.navigation.SaarthiRoute
import javax.inject.Inject

/**
 * The app's designated start destination — the only feature that claims
 * one. With more than one feature in the `Set<FeatureNavigation>`, only
 * one may set [startDestination] non-null, or which one "wins" becomes
 * non-deterministic (Set iteration order isn't guaranteed).
 */
class MedicinesNavigation @Inject constructor() : FeatureNavigation {
    override val startDestination: SaarthiRoute = MedicinesRoute

    override fun NavGraphBuilder.register(navController: NavHostController) {
        composable(MedicinesRoute.route) {
            MedicinesListScreen()
        }
    }
}
