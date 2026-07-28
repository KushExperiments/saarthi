package com.saarthi.app.feature.voice

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.saarthi.app.core.navigation.FeatureNavigation
import com.saarthi.app.core.navigation.SaarthiRoute
import javax.inject.Inject

/**
 * The app's new start destination — voice-first, per Philosophy/Interaction
 * OS. Replaces Medicines as the literal entry point (Medicines is still
 * reachable, by voice or by direct navigation, just no longer where the
 * app opens to). Only one feature may claim [startDestination].
 */
class VoiceNavigation @Inject constructor() : FeatureNavigation {
    override val startDestination: SaarthiRoute = VoiceRoute

    override fun NavGraphBuilder.register(navController: NavHostController) {
        composable(VoiceRoute.route) {
            VoiceHomeScreen(navController = navController)
        }
    }
}
