package com.lifeos.app.feature.placeholder

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.lifeos.app.core.navigation.FeatureNavigation
import javax.inject.Inject

/**
 * No longer the app's start destination now that a real feature
 * (Medicines) exists — kept registered as a secondary route and as a
 * living template for how a feature module plugs in. Only one feature may
 * claim [FeatureNavigation.startDestination]; see MedicinesNavigation.
 */
class PlaceholderNavigation @Inject constructor() : FeatureNavigation {

    override fun NavGraphBuilder.register(navController: NavHostController) {
        composable(PlaceholderRoute.route) {
            PlaceholderScreen()
        }
    }
}
