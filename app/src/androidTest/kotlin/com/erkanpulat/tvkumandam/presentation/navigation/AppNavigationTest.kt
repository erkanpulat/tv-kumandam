package com.erkanpulat.tvkumandam.presentation.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.erkanpulat.tvkumandam.presentation.settings.SettingsScreen
import com.erkanpulat.tvkumandam.presentation.settings.SettingsUiState
import com.erkanpulat.tvkumandam.ui.theme.TvKumandamTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bottomBarExposesThreeTopLevelDestinationsAndSelectedState() {
        var selected: AppDestination = AppDestination.Remote
        composeRule.setContent {
            TvKumandamTheme {
                AppBottomBar(selected = selected, onSelect = { selected = it })
            }
        }

        composeRule.onNodeWithTag("bottom_remote").assertIsSelected()
        composeRule.onNodeWithTag("bottom_devices").performClick()
        assertEquals(AppDestination.Devices, selected)
        composeRule.onNodeWithTag("bottom_settings").performClick()
        assertEquals(AppDestination.Settings, selected)
    }

    @Test
    fun settingsContentRemainsScrollableAt320DpAnd200PercentFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                TvKumandamTheme {
                    SettingsScreen(
                        state = SettingsUiState(isIrAvailable = true),
                        onThemeSelected = {},
                        onHapticsChanged = {},
                        onHandednessSelected = {},
                        onRetry = {},
                        modifier = Modifier
                            .width(320.dp)
                            .height(240.dp),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Bluetooth, Wi-Fi, TV hesabı veya bulut hesabı kullanılmaz.")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
