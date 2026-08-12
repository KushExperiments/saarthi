package com.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.lifeos.app.core.designsystem.LifeOSTheme
import com.lifeos.app.core.navigation.FeatureNavigation
import com.lifeos.app.feature.voice.HandsFreePromptPrefs
import com.lifeos.app.feature.voice.HandsFreePromptScreen
import com.lifeos.app.feature.voice.restartWakeWordServiceIfEnabled
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
            LifeOSTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current

                    // Resumes hands-free listening across app/process
                    // restarts (it only otherwise starts reactively when
                    // the onboarding prompt is answered). This used to
                    // live in LifeOSApplication.onCreate() — moved here and
                    // wrapped in try/catch after discovering that starting
                    // a foreground service unconditionally in
                    // Application.onCreate(), before anything else in the
                    // app exists, is a real crash risk: Android 12+ can
                    // reject a foreground-service start depending on
                    // exactly how/why the process was launched, and an
                    // uncaught exception there would crash on every single
                    // launch with no way back in short of clearing app
                    // data. A LaunchedEffect inside a live Activity's
                    // composition is a more reliably-foreground context,
                    // and the try/catch means even a rejection here just
                    // silently leaves hands-free off instead of crashing.
                    LaunchedEffect(Unit) {
                        restartWakeWordServiceIfEnabled(context)
                    }

                    var onboardingSeen by remember { mutableStateOf(OnboardingPrefs.hasSeenOnboarding(context)) }
                    var handsFreePromptSeen by remember { mutableStateOf(HandsFreePromptPrefs.hasSeenPrompt(context)) }

                    if (!onboardingSeen) {
                        OnboardingScreen(
                            onDone = {
                                OnboardingPrefs.markSeen(context)
                                onboardingSeen = true
                            },
                        )
                    } else if (!handsFreePromptSeen) {
                        // Requests mic/notification access and turns on
                        // hands-free listening automatically — no in-app
                        // yes/no choice, per direct feedback that this
                        // should be real voice recognition, not
                        // tap-to-talk with an opt-in step in the way.
                        HandsFreePromptScreen(onDone = { handsFreePromptSeen = true })
                    } else {
                        // PIN lock removed: it was gating the entire app
                        // behind a first-run PIN-setup screen backed by a
                        // legacy androidx.security Keystore API
                        // (EncryptedPrefsAuthRepository) that this codebase
                        // never had a way to verify actually works on a real
                        // device (no emulator/device available while
                        // building it — see its own doc comment). That's
                        // the most likely reason the app stopped opening at
                        // all: a crash during Hilt's dependency
                        // construction, before any UI renders, would look
                        // exactly like "doesn't open." An elder-facing app
                        // also shouldn't need a PIN to talk to Juno in the
                        // first place — removed rather than patched blind.
                        LifeOSNavHost(featureNavigations)
                    }
                }
            }
        }
    }
}

@Composable
private fun LifeOSNavHost(featureNavigations: Set<FeatureNavigation>) {
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
