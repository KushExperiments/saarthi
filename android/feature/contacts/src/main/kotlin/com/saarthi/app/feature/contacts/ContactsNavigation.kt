package com.saarthi.app.feature.contacts

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.saarthi.app.core.navigation.FeatureNavigation
import javax.inject.Inject

class ContactsNavigation @Inject constructor() : FeatureNavigation {
    override fun NavGraphBuilder.register(navController: NavHostController) {
        composable(ContactsRoute.route) {
            ContactsListScreen()
        }
    }
}
