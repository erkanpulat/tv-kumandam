package com.erkanpulat.tvkumandam.presentation.devices

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.ui.theme.TvKumandamTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DevicesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedDeviceIsVisibleAndDeleteRequiresConfirmation() {
        val remote = SavedRemote("salon", "Salon TV", ArcelikOldLcdProfile.ID, isConfirmed = true)
        val state = DevicesUiState(
            isLoading = false,
            settings = RemoteSettings(listOf(remote), remote.id),
            devices = listOf(DeviceListItem(remote, ArcelikOldLcdProfile.profile, true)),
            isIrAvailable = true,
        )
        var deleted: String? = null
        composeRule.setContent {
            TvKumandamTheme {
                DevicesScreen(
                    state = state,
                    onAddDevice = {},
                    onSelectRemote = {},
                    onDeleteRemote = { deleted = it },
                )
            }
        }

        composeRule.onNodeWithText("Salon TV").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Salon TV kumandasını sil").performClick()
        assertEquals(null, deleted)
        composeRule.onNodeWithText("TV'yi sil").performClick()
        assertEquals("salon", deleted)
    }

    @Test
    fun finderDisablesTransmissionWithoutIrAndExposesExplicitHumanResponse() {
        val awaitingResponse = mutableStateOf(false)
        val irAvailable = mutableStateOf(false)
        val finder = ProfileFinderState(
            step = FinderStep.TEST,
            brands = listOf("Arçelik"),
            selectedBrand = "Arçelik",
            candidateIds = listOf(ArcelikOldLcdProfile.ID),
            currentProfile = ArcelikOldLcdProfile.profile,
            testCommand = RemoteCommand.POWER,
            awaitingResponse = false,
        )
        composeRule.setContent {
            TvKumandamTheme {
                ProfileFinderScreen(
                    state = DevicesUiState(
                        isLoading = false,
                        isIrAvailable = irAvailable.value,
                        finder = finder.copy(awaitingResponse = awaitingResponse.value),
                    ),
                    onBack = {},
                    onBrand = {},
                    onModel = {},
                    onUnknownModel = {},
                    onSendTest = {},
                    onResponse = {},
                    onNameChange = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithTag("finder_send_POWER").assertIsNotEnabled()
        composeRule.onNodeWithText("Bu telefonda IR vericisi bulunamadı.").assertIsDisplayed()

        composeRule.runOnIdle {
            irAvailable.value = true
            awaitingResponse.value = true
        }
        composeRule.onNodeWithText("Evet").assertIsEnabled()
        composeRule.onNodeWithText("Hayır").assertIsEnabled()
    }

    @Test
    fun namingRemainsReachableAt320DpAnd200PercentFontScale() {
        val finder = ProfileFinderState(
            step = FinderStep.NAME,
            brands = listOf("Arçelik"),
            selectedBrand = "Arçelik",
            currentProfile = ArcelikOldLcdProfile.profile,
            candidateIds = listOf(ArcelikOldLcdProfile.ID),
            tvName = "Salon TV",
        )
        composeRule.setContent {
            val density = LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                TvKumandamTheme {
                    ProfileFinderScreen(
                        state = DevicesUiState(isLoading = false, isIrAvailable = true, finder = finder),
                        onBack = {}, onBrand = {}, onModel = {}, onUnknownModel = {},
                        onSendTest = {}, onResponse = {}, onNameChange = {}, onSave = {},
                        modifier = Modifier.width(320.dp),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("finder_save").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun modelSearchFiltersResultsWhileHeaderAndUnknownModelStayFixed() {
        val models = listOf(
            "32HDR", "40FA", "43G", "50A", "55B", "65C", "72D", "80E", "82-507 B",
        )
        val finder = ProfileFinderState(
            step = FinderStep.MODEL,
            brands = listOf("Arçelik"),
            models = models,
            selectedBrand = "Arçelik",
        )
        composeRule.setContent {
            TvKumandamTheme {
                ProfileFinderScreen(
                    state = DevicesUiState(isLoading = false, isIrAvailable = true, finder = finder),
                    onBack = {}, onBrand = {}, onModel = {}, onUnknownModel = {},
                    onSendTest = {}, onResponse = {}, onNameChange = {}, onSave = {},
                    modifier = Modifier.width(360.dp).height(480.dp),
                )
            }
        }

        val headerTop = composeRule.onNodeWithTag("finder_header")
            .fetchSemanticsNode().boundsInRoot.top
        composeRule.onNodeWithTag("finder_model_search").performTextInput("507")
        composeRule.onNodeWithTag("finder_model_82-507 B").assertIsDisplayed()
        composeRule.onAllNodesWithTag("finder_model_32HDR").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Model aramasını temizle").performClick()
        composeRule.onNodeWithTag("finder_model_82-507 B").performScrollTo().assertIsDisplayed()

        assertEquals(
            headerTop,
            composeRule.onNodeWithTag("finder_header").fetchSemanticsNode().boundsInRoot.top,
        )
        composeRule.onNodeWithTag("finder_unknown_model").assertIsDisplayed()
    }
}
