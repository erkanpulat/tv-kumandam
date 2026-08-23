package com.erkanpulat.tvkumandam.presentation.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.erkanpulat.tvkumandam.ui.theme.TvKumandamTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun explainsMacrosAndStartsTvSetup() {
        var continued = false
        composeRule.setContent {
            TvKumandamTheme {
                WelcomeScreen(
                    isIrAvailable = true,
                    onContinue = { continued = true },
                )
            }
        }

        composeRule.onNodeWithText("TV Kumandam").assertIsDisplayed()
        composeRule.onNodeWithText("Tek tuşla makrolar").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_add_tv").performClick()
        assertTrue(continued)
    }
}
