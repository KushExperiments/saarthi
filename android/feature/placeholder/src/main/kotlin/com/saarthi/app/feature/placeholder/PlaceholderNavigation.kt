package com.saarthi.app.feature.placeholder

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.saarthi.app.core.navigation.FeatureNavigation
import com.saarthi.app.core.navigation.SaarthiRoute
import javax.inject.Inject

class PlaceholderNavigation @Inject constructor() : FeatureNavigation {
    override val startDestination: SaarthiRoute = PlaceholderRoute

    override fun NavGraphBuilder.register(navController: NavHostController) {
        composable(PlaceholderRoute.route) {
            PlaceholderScreen()
        }
    }
}
