package com.lifeos.app.feature.medicines

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.lifeos.app.core.navigation.FeatureNavigation
import javax.inject.Inject

/**
 * No longer the app's start destination — Voice is (see VoiceNavigation),
 * per the voice-first redesign. Medicines is still reachable, by voice
 * ("open my medicines") or direct navigation. Only one feature may claim
 * [FeatureNavigation.startDestination].
 */
class MedicinesNavigation @Inject constructor() : FeatureNavigation {

    override fun NavGraphBuilder.register(navController: NavHostController) {
        composable(MedicinesRoute.route) {
            MedicinesListScreen()
        }
    }
}
