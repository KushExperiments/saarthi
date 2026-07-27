package com.saarthi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.saarthi.app.core.designsystem.SaarthiTheme
import com.saarthi.app.core.navigation.FeatureNavigation
import com.saarthi.app.core.security.AuthGate
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // @JvmSuppressWildcards is required for Hilt/Dagger Set multibinding
    // injection to resolve correctly from Kotlin — every feature module's
    // FeatureNavigation lands here without MainActivity importing any of
    // them by name.
    @Inject
    lateinit var featureNavigations: Set<@JvmSuppressWildcards FeatureNavigation>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SaarthiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Nothing behind the lock (no feature screen, no data)
                    // is ever composed until AuthGate reaches Unlocked.
                    AuthGate {
                        SaarthiNavHost(featureNavigations)
                    }
                }
            }
        }
    }
}

@Composable
private fun SaarthiNavHost(featureNavigations: Set<FeatureNavigation>) {
    val navController: NavHostController = rememberNavController()
    val start = featureNavigations.firstNotNullOfOrNull { it.startDestination }?.route

    if (start == null) {
        // No feature module has registered a start destination yet — an
        // empty box rather than a crash, matching "fail gracefully."
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    NavHost(navController = navController, startDestination = start) {
        featureNavigations.forEach { feature ->
            with(feature) { register(navController) }
        }
    }
}
