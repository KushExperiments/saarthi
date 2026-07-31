package com.lifeos.app.feature.placeholder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.lifeos.app.core.designsystem.LifeOSTheme
import org.junit.Rule
import org.junit.Test

/**
 * Bypasses Hilt entirely by passing the ViewModel explicitly (the
 * `hiltViewModel()` default is only exercised at runtime, not here) —
 * proves the screen renders and the Design System theme applies without
 * needing full Hilt test infrastructure for this simple a check.
 */
class PlaceholderScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun placeholderScreen_displaysGreetingMessage() {
        composeRule.setContent {
            LifeOSTheme {
                PlaceholderScreen(viewModel = PlaceholderViewModel(GreetingProvider()))
            }
        }

        composeRule.onNodeWithTag("placeholder_message").assertIsDisplayed()
    }
}
