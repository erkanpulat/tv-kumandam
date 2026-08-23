package com.erkanpulat.tvkumandam.presentation.customize

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.SavedMacroStep
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.ui.theme.TvKumandamTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CustomizeRemoteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val hdmiMacro = SavedMacro(
        "hdmi1",
        "HDMI 1",
        listOf(SavedMacroStep(RemoteCommand.SOURCE)),
    )
    private val hdmi = RemoteAction.Macro(hdmiMacro.id)
    private val source = RemoteAction.Command(RemoteCommand.SOURCE)
    private val menu = RemoteAction.Command(RemoteCommand.MENU)

    @Test
    fun orderedPositionsExposeNonDragAccessibilityOperations() {
        var movedToTop: RemoteAction? = null
        var hidden: RemoteAction? = null
        setEditorContent(
            state(actions = listOf(hdmi, source, menu)),
            onMoveToTop = { movedToTop = it },
            onRemove = { hidden = it },
        )

        composeRule.onNodeWithTag("quick_deck_position_0")
            .assertContentDescriptionContains("1. sıra, HDMI 1")
            .assertContentDescriptionContains("Makro")
        val secondPosition = composeRule.onNodeWithTag("quick_deck_position_1")
            .assertContentDescriptionContains("2. sıra, Kaynak")
        composeRule.runOnIdle {
            secondPosition.fetchSemanticsNode().config[SemanticsActions.CustomActions]
                .first { it.label == "En başa taşı" }.action()
        }
        assertEquals(source, movedToTop)

        val thirdPosition = composeRule.onNodeWithTag("quick_deck_position_2")
        composeRule.runOnIdle {
            thirdPosition.fetchSemanticsNode().config[SemanticsActions.CustomActions]
                .first { it.label == "Gizle" }.action()
            }
        assertEquals(menu, hidden)
    }

    @Test
    fun rtlKeepsDeckIndexZeroPhysicallyLeftAndLeftActionTruthful() {
        var movedLeft: RemoteAction? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TvKumandamTheme(darkTheme = false) {
                    EditorUnderTest(
                        state = state(actions = listOf(hdmi, source, menu)),
                        onMoveLeft = { movedLeft = it },
                    )
                }
            }
        }

        val first = composeRule.onNodeWithTag("quick_deck_position_0")
            .fetchSemanticsNode().boundsInRoot.center.x
        val secondPosition = composeRule.onNodeWithTag("quick_deck_position_1")
        val second = secondPosition.fetchSemanticsNode().boundsInRoot.center.x
        assertTrue("Index zero must remain physically left in RTL", first < second)
        composeRule.runOnIdle {
            secondPosition.fetchSemanticsNode().config[SemanticsActions.CustomActions]
                .first { it.label == "Sola taşı" }.action()
        }
        assertEquals(source, movedLeft)
    }

    @Test
    fun oneLongPressDragCanMoveAcrossTwoDeckSlots() {
        composeRule.setContent {
            var actions by remember { mutableStateOf(listOf(hdmi, source, menu)) }
            TvKumandamTheme(darkTheme = false) {
                EditorUnderTest(
                    state = state(actions = actions),
                    onMove = { from, to ->
                        actions = QuickActionEditor.move(
                            ArcelikOldLcdProfile.profile,
                            actions,
                            from,
                            to,
                            listOf(hdmiMacro),
                        ).actions
                    },
                )
            }
        }

        val cardWidth = composeRule.onNodeWithTag("quick_deck_position_2")
            .fetchSemanticsNode().boundsInRoot.width
        composeRule.onNodeWithContentDescription("Menü tuşunu sürükle")
            .performTouchInput {
                down(center)
                advanceEventTime(700)
                moveBy(Offset(-cardWidth / 2f, 0f))
                advanceEventTime(16)
                moveBy(Offset(-cardWidth / 2f, 0f))
                up()
            }

        composeRule.onNodeWithTag("quick_deck_position_0")
            .assertContentDescriptionContains("1. sıra, Menü")
    }

    @Test
    fun pickerAddsWhenSpaceExistsAndUsesTypedStableTag() {
        var added: RemoteAction? = null
        setEditorContent(
            state(actions = listOf(hdmi, source)),
            onAdd = { added = it },
        )

        composeRule.onNodeWithTag("quick_picker_command_menu")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals(menu, added)
        composeRule.onNodeWithTag("quick_picker_macro_hdmi1")
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun newMacroOpensAFullScreenBoundedEditor() {
        setEditorContent(state(actions = listOf(source)))

        composeRule.onNodeWithText("Yeni makro").performClick()

        composeRule.onNodeWithText("Makro adı").assertIsDisplayed()
        composeRule.onNodeWithText("Komut ekle", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Kaydet").assertIsNotEnabled()
    }

    @Test
    fun fullDeckPickerAsksForExplicitReplacementTarget() {
        val volumeUp = RemoteAction.Command(RemoteCommand.VOLUME_UP)
        val volumeDown = RemoteAction.Command(RemoteCommand.VOLUME_DOWN)
        var replacedIndex: Int? = null
        var replacement: RemoteAction? = null
        setEditorContent(
            state(actions = listOf(hdmi, source, menu, volumeUp)),
            onReplace = { index, action ->
                replacedIndex = index
                replacement = action
            },
        )

        composeRule.onNodeWithTag("quick_picker_command_volume_down")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("quick_replace_target_0")
            .assertIsDisplayed()
            .performClick()

        assertEquals(0, replacedIndex)
        assertEquals(volumeDown, replacement)
    }

    @Test
    fun failedStateKeepsRetryReachableAndDoesNotInvokeDoneItself() {
        var retries = 0
        var done = false
        setEditorContent(
            state(actions = listOf(hdmi), saveError = "Kaydedilemedi", isDirty = true),
            onRetry = { retries += 1 },
            onDone = { done = true },
        )

        composeRule.onNodeWithTag("quick_save_error").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").performClick()

        assertEquals(1, retries)
        assertEquals(false, done)
    }

    @Test
    fun pickerRemainsReachableAt320DpAnd200PercentFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                TvKumandamTheme(darkTheme = false) {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        EditorUnderTest(state(actions = listOf(hdmi)))
                    }
                }
            }
        }

        composeRule.onNodeWithTag("quick_picker_command_menu")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun setEditorContent(
        state: CustomizeRemoteUiState,
        onAdd: (RemoteAction) -> Unit = {},
        onReplace: (Int, RemoteAction) -> Unit = { _, _ -> },
        onRemove: (RemoteAction) -> Unit = {},
        onMoveLeft: (RemoteAction) -> Unit = {},
        onMoveToTop: (RemoteAction) -> Unit = {},
        onMove: (Int, Int) -> Unit = { _, _ -> },
        onRetry: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {
        composeRule.setContent {
            TvKumandamTheme(darkTheme = false) {
                EditorUnderTest(
                    state = state,
                    onAdd = onAdd,
                    onReplace = onReplace,
                    onRemove = onRemove,
                    onMoveLeft = onMoveLeft,
                    onMoveToTop = onMoveToTop,
                    onMove = onMove,
                    onRetry = onRetry,
                    onDone = onDone,
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun EditorUnderTest(
        state: CustomizeRemoteUiState,
        onAdd: (RemoteAction) -> Unit = {},
        onReplace: (Int, RemoteAction) -> Unit = { _, _ -> },
        onRemove: (RemoteAction) -> Unit = {},
        onMoveLeft: (RemoteAction) -> Unit = {},
        onMoveToTop: (RemoteAction) -> Unit = {},
        onMove: (Int, Int) -> Unit = { _, _ -> },
        onRetry: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {
        CustomizeRemoteScreen(
            state = state,
            onBack = {},
            onDone = onDone,
            onAdd = onAdd,
            onReplace = onReplace,
            onRemove = onRemove,
            onMoveLeft = onMoveLeft,
            onMoveRight = {},
            onMoveToTop = onMoveToTop,
            onMove = onMove,
            onReset = {},
            onSaveMacro = { _, _ -> },
            onDeleteMacro = {},
            onRetry = onRetry,
        )
    }

    private fun state(
        actions: List<RemoteAction>,
        saveError: String? = null,
        isDirty: Boolean = false,
    ): CustomizeRemoteUiState {
        val remote = SavedRemote(
            id = "salon",
            name = "Salon TV",
            profileId = ArcelikOldLcdProfile.ID,
            quickActions = actions,
            macros = listOf(hdmiMacro),
        )
        val settings = RemoteSettings(listOf(remote), remote.id)
        return CustomizeRemoteUiState(
            isLoading = false,
            remote = remote,
            profile = ArcelikOldLcdProfile.profile,
            settings = settings,
            actions = actions,
            availableActions = QuickActionEditor.availableActions(
                ArcelikOldLcdProfile.profile,
                listOf(hdmiMacro),
            ),
            isDirty = isDirty,
            saveError = saveError,
        )
    }
}
