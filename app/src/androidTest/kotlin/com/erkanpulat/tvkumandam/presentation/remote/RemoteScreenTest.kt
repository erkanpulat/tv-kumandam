package com.erkanpulat.tvkumandam.presentation.remote

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.data.remote.protocol.Rc5IrCommand
import com.erkanpulat.tvkumandam.domain.model.CommandBinding
import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.InputCapability
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutSpec
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteSequence
import com.erkanpulat.tvkumandam.domain.model.RemoteSequenceStep
import com.erkanpulat.tvkumandam.domain.model.RemoteShortcut
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.SavedMacroStep
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.model.ShortcutBinding
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.ui.theme.TvKumandamTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RemoteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun arcelikDeckPreservesTypedOrderAndKeepsPowerOutsideDeck() {
        var sentAction: RemoteAction? = null
        setRemoteContent(
            state = stateFor(ArcelikOldLcdProfile.profile),
            onAction = { sentAction = it },
        )

        composeRule.onNodeWithTag("quick_action_0")
            .assertIsDisplayed()
            .assertTextContains("HDMI1")
            .performClick()
        assertEquals(RemoteAction.Shortcut(RemoteShortcut.HDMI1), sentAction)

        composeRule.onNodeWithTag("quick_action_1").assertTextContains("Kaynak")
        composeRule.onAllNodesWithTag("quick_action_POWER").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Televizyonu aç veya kapat")
            .assertIsDisplayed()
            .performClick()
        assertEquals(RemoteAction.Command(RemoteCommand.POWER), sentAction)
    }

    @Test
    fun userMacroIsClearlyIdentifiedInQuickActions() {
        val macro = SavedMacro(
            id = "movie-mode",
            name = "Film modu",
            steps = listOf(
                SavedMacroStep(RemoteCommand.SOURCE),
                SavedMacroStep(RemoteCommand.DOWN),
                SavedMacroStep(RemoteCommand.OK),
            ),
        )
        setRemoteContent(
            state = stateFor(
                profile = ArcelikOldLcdProfile.profile,
                quickActions = listOf(RemoteAction.Macro(macro.id)),
                macros = listOf(macro),
            ),
        )

        composeRule.onNodeWithTag("quick_action_0")
            .assertTextContains("MAKRO")
            .assertTextContains("Film modu")
            .assertContentDescriptionContains("Makro")
    }

    @Test
    fun classicTemplateExposesItsOrderedSections() = assertTemplateSections(RemoteLayoutTemplate.CLASSIC_DPAD)

    @Test
    fun fullRemoteTemplateExposesItsOrderedSections() = assertTemplateSections(RemoteLayoutTemplate.FULL_REMOTE)

    @Test
    fun smartTemplateExposesItsOrderedSections() = assertTemplateSections(RemoteLayoutTemplate.SMART_MEDIA)

    @Test
    fun directInputTemplateExposesItsOrderedSections() = assertTemplateSections(RemoteLayoutTemplate.DIRECT_INPUT)

    @Test
    fun unsupportedActionsAndEmptySectionsAreAbsent() {
        setRemoteContent(stateFor(ArcelikOldLcdProfile.profile))

        composeRule.onAllNodesWithContentDescription("Televizyonun ana ekranını aç")
            .assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Sesi kapat veya aç").assertCountEquals(0)
        composeRule.onAllNodesWithTag("remote_section_MEDIA").assertCountEquals(0)
        composeRule.onAllNodesWithTag("remote_section_DIRECT_INPUTS").assertCountEquals(0)
    }

    @Test
    fun moreRevealsOnlySupportedAdvancedActions() {
        val profile = allCapabilitiesProfile(RemoteLayoutTemplate.CLASSIC_DPAD)
        setRemoteContent(stateFor(profile))

        composeRule.onAllNodesWithContentDescription("Bilgi ekranını aç").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Daha fazla kumanda tuşunu aç")
            .assertHasClickAction()
            .performClick()

        listOf(
            "Televizyon menüsünden çık",
            "Bilgi ekranını aç",
            "Son izlenen kanala dön",
            "0 rakamını gönder",
            "Kırmızı işlev tuşunu gönder",
            "Oynatmayı başlat veya duraklat",
        ).forEach { description ->
            composeRule.onNodeWithContentDescription(description)
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun shortcutProgressLocksEveryTransmissionAndCancelRemainsEnabled() {
        var cancelled = false
        var haptics = 0
        setRemoteContent(
            state = stateFor(ArcelikOldLcdProfile.profile).copy(
                transmissionState = TransmissionState.Shortcut(
                    savedRemoteId = "${ArcelikOldLcdProfile.ID}-tv",
                    profileId = ArcelikOldLcdProfile.ID,
                    shortcut = RemoteShortcut.HDMI1,
                    completedSteps = 3,
                    totalSteps = 9,
                ),
            ),
            onCancel = { cancelled = true },
            onHaptic = { haptics += 1 },
        )

        composeRule.onNodeWithText("3 / 9 adım gönderildi").assertIsDisplayed()
        composeRule.onNodeWithText("TV'ye doğrultmaya devam et").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_action_0").assertIsNotEnabled().performClick()
        composeRule.onNodeWithContentDescription("Televizyonu aç veya kapat").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Sesi artır").assertIsNotEnabled()
        composeRule.onNodeWithText("Kalan adımları durdur").assertIsEnabled().performClick()

        assertTrue(cancelled)
        assertEquals(0, haptics)
    }

    @Test
    fun acceptedVisibleKeyUsesExactlyOneHapticCallback() {
        var haptics = 0
        setRemoteContent(
            state = stateFor(ArcelikOldLcdProfile.profile),
            onHaptic = { haptics += 1 },
        )

        composeRule.onNodeWithTag("quick_action_0").performClick()

        assertEquals(1, haptics)
    }

    @Test
    fun macroHdmiDispatchesShortcutAndExplainsDuration() {
        var macroAction: RemoteAction? = null
        setRemoteContent(
            state = stateFor(ArcelikOldLcdProfile.profile),
            onAction = { macroAction = it },
        )
        composeRule.onNodeWithTag("quick_action_0")
            .assertTextContains("Yaklaşık")
            .performClick()
        assertEquals(RemoteAction.Shortcut(RemoteShortcut.HDMI1), macroAction)
    }

    @Test
    fun discreteHdmiDispatchesCommand() {
        val direct = allCapabilitiesProfile(RemoteLayoutTemplate.DIRECT_INPUT)
        var directAction: RemoteAction? = null
        setRemoteContent(stateFor(direct), onAction = { directAction = it })
        composeRule.onNodeWithTag("direct_input_HDMI1")
            .performScrollTo()
            .assertTextContains("HDMI 1")
            .performClick()
        assertEquals(RemoteAction.Command(RemoteCommand.HDMI1), directAction)
    }

    @Test
    fun irUnavailableStatusIsPassiveAndTroubleshootingRemainsReachable() {
        var troubleshooting = false
        setRemoteContent(
            state = stateFor(ArcelikOldLcdProfile.profile).copy(isIrAvailable = false),
            onTroubleshooting = { troubleshooting = true },
        )

        composeRule.onNodeWithTag("ir_status")
            .assertIsDisplayed()
            .assertHasNoClickAction()
        composeRule.onNodeWithContentDescription("Televizyonu aç veya kapat").assertIsNotEnabled()
        composeRule.onNodeWithText("Sorun giderme adımlarını aç")
            .assertIsEnabled()
            .performClick()
        assertTrue(troubleshooting)
    }

    @Test
    fun rtlWideDirectTemplateKeepsPhysicalDirectionAndRightHandedRockerOrder() {
        val base = stateFor(allCapabilitiesProfile(RemoteLayoutTemplate.DIRECT_INPUT))
        val rightHanded = base.copy(settings = base.settings.copy(handedness = Handedness.RIGHT))
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TvKumandamTheme {
                    Box(Modifier.width(600.dp).height(1_100.dp)) {
                        RemoteScreen(state = rightHanded, onAction = {})
                    }
                }
            }
        }

        assertPhysicalDpadOrder()
        assertRockerOrder(leftLabel = "SES", rightLabel = "KANAL")
    }

    @Test
    fun rtlCompactSmartTemplateKeepsPhysicalDirectionAndLeftHandedRockerOrder() {
        val base = stateFor(allCapabilitiesProfile(RemoteLayoutTemplate.SMART_MEDIA))
        val leftHanded = base.copy(settings = base.settings.copy(handedness = Handedness.LEFT))
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TvKumandamTheme {
                    Box(Modifier.width(390.dp).height(1_100.dp)) {
                        RemoteScreen(state = leftHanded, onAction = {})
                    }
                }
            }
        }

        assertPhysicalDpadOrder()
        assertRockerOrder(leftLabel = "KANAL", rightLabel = "SES")
    }

    @Test
    fun smartMediaOwnedCommandsDoNotCreateAnEmptyAdvancedSection() {
        val ownedOnly = smartMediaProfile(setOf(RemoteCommand.DIGIT_1, RemoteCommand.PLAY))
        setRemoteContent(stateFor(ownedOnly))

        composeRule.onAllNodesWithTag("remote_section_ADVANCED").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Daha fazla kumanda tuşunu aç")
            .assertCountEquals(0)

        val withExit = smartMediaProfile(
            setOf(RemoteCommand.DIGIT_1, RemoteCommand.PLAY, RemoteCommand.EXIT),
        )
        setRemoteContent(stateFor(withExit))
        composeRule.onNodeWithTag("remote_section_ADVANCED").assertExists()
        composeRule.onNodeWithContentDescription("Daha fazla kumanda tuşunu aç").performClick()
        composeRule.onNodeWithContentDescription("Televizyon menüsünden çık")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun width320UsesCompactLayoutWithoutHorizontalOverflow() = assertWidthLayout(320, "compact")

    @Test
    fun width390UsesCompactLayoutWithoutHorizontalOverflow() = assertWidthLayout(390, "compact")

    @Test
    fun width600UsesWideLayoutWithoutHorizontalOverflow() = assertWidthLayout(600, "wide")

    @Test
    fun twoHundredPercentFontScaleRemainsScrollableWithoutLosingControls() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                TvKumandamTheme {
                    Box(Modifier.width(320.dp).height(700.dp)) {
                        RemoteScreen(state = stateFor(ArcelikOldLcdProfile.profile), onAction = {})
                    }
                }
            }
        }

        composeRule.onNodeWithTag("quick_action_0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Seçimi onayla")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun noDeviceStateDoesNotCrashAndOffersAddTvHandoff() {
        var addTv = false
        setRemoteContent(
            state = RemoteUiState.fromSettings(
                profiles = listOf(ArcelikOldLcdProfile.profile),
                settings = RemoteSettings(),
                isIrAvailable = true,
            ),
            onAddTv = { addTv = true },
        )

        composeRule.onNodeWithText("TV ekleyin").assertIsDisplayed()
        composeRule.onNodeWithText("TV ekle").performClick()
        assertTrue(addTv)
    }

    private fun setRemoteContent(
        state: RemoteUiState,
        onAction: (RemoteAction) -> Unit = {},
        onCancel: () -> Unit = {},
        onTroubleshooting: () -> Unit = {},
        onAddTv: () -> Unit = {},
        onHaptic: () -> Unit = {},
    ) {
        composeRule.setContent {
            TvKumandamTheme {
                RemoteScreen(
                    state = state,
                    onAction = onAction,
                    onCancelTransmission = onCancel,
                    onTroubleshooting = onTroubleshooting,
                    onAddTv = onAddTv,
                    onAcceptedPressHaptic = onHaptic,
                )
            }
        }
    }

    private fun assertTemplateSections(template: RemoteLayoutTemplate) {
        val profile = allCapabilitiesProfile(template)
        setRemoteContent(stateFor(profile))

        val bounds = profile.layout.sections.map { section ->
            composeRule.onNodeWithTag("remote_section_${section.name}")
                .assertExists()
                .fetchSemanticsNode().boundsInRoot.top
        }
        assertEquals("$template section order", bounds.sorted(), bounds)
    }

    private fun assertWidthLayout(width: Int, mode: String) {
        composeRule.setContent {
            TvKumandamTheme {
                Box(Modifier.width(width.dp).height(900.dp)) {
                    RemoteScreen(state = stateFor(ArcelikOldLcdProfile.profile), onAction = {})
                }
            }
        }

        val root = composeRule.onNodeWithTag("remote_layout_$mode")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val device = composeRule.onNodeWithTag("selected_device_card")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        assertTrue("$width dp overflows left", device.left >= root.left)
        assertTrue("$width dp overflows right", device.right <= root.right)
    }

    private fun assertPhysicalDpadOrder() {
        val left = composeRule.onNodeWithTag("dpad_left")
            .performScrollTo()
            .fetchSemanticsNode().boundsInRoot.center.x
        val ok = composeRule.onNodeWithTag("dpad_ok")
            .fetchSemanticsNode().boundsInRoot.center.x
        val right = composeRule.onNodeWithTag("dpad_right")
            .fetchSemanticsNode().boundsInRoot.center.x
        assertTrue("Physical left must stay left of OK in RTL", left < ok)
        assertTrue("Physical right must stay right of OK in RTL", ok < right)
    }

    private fun assertRockerOrder(leftLabel: String, rightLabel: String) {
        val leftNode = composeRule.onNodeWithTag("rocker_left").performScrollTo()
        val rightNode = composeRule.onNodeWithTag("rocker_right").performScrollTo()
        leftNode.assertTextContains(leftLabel)
        rightNode.assertTextContains(rightLabel)
        val left = leftNode.fetchSemanticsNode().boundsInRoot.center.x
        val right = rightNode.fetchSemanticsNode().boundsInRoot.center.x
        assertTrue("Handedness rocker tagged left must remain physically left", left < right)
    }

    private fun stateFor(
        profile: RemoteProfile,
        quickActions: List<RemoteAction> = profile.layout.defaultQuickActions,
        macros: List<SavedMacro> = emptyList(),
    ): RemoteUiState {
        val remote = SavedRemote(
            id = "${profile.id}-tv",
            name = "Salon TV",
            profileId = profile.id,
            quickActions = quickActions,
            macros = macros,
            isConfirmed = true,
        )
        return RemoteUiState.fromSettings(
            profiles = listOf(profile),
            settings = RemoteSettings(
                savedRemotes = listOf(remote),
                selectedSavedRemoteId = remote.id,
            ),
            isIrAvailable = true,
        )
    }

    private fun allCapabilitiesProfile(template: RemoteLayoutTemplate): RemoteProfile {
        val evidence = ProfileEvidence(EvidenceTier.SOURCE_VERIFIED, "Task 7 UI fixture")
        val commands = RemoteCommand.entries.associateWith { command ->
            CommandBinding(
                irCommand = Rc5IrCommand(address = 0, command = command.ordinal % 128),
                evidence = evidence,
            )
        }
        val hdmiSequence = RemoteSequence(
            steps = listOf(RemoteSequenceStep(RemoteCommand.SOURCE)),
        )
        return RemoteProfile(
            id = "ui-${template.name.lowercase()}",
            brand = "Test",
            displayName = "${template.name} Test Ailesi",
            modelAliases = listOf("Test TV"),
            remoteModel = "TEST-7",
            defaultEvidence = evidence,
            commands = commands,
            shortcuts = mapOf(RemoteShortcut.HDMI1 to ShortcutBinding(hdmiSequence, evidence)),
            layout = RemoteLayoutSpec.defaultFor(
                template = template,
                defaultQuickActions = listOf(
                    RemoteAction.Command(RemoteCommand.SOURCE),
                    RemoteAction.Command(RemoteCommand.MUTE),
                ),
            ),
            inputCapability = if (template == RemoteLayoutTemplate.DIRECT_INPUT) {
                InputCapability.discreteCommands(
                    setOf(
                        RemoteCommand.HDMI1,
                        RemoteCommand.HDMI2,
                        RemoteCommand.HDMI3,
                        RemoteCommand.HDMI4,
                    ),
                )
            } else {
                InputCapability.sourceMenuMacros(setOf(RemoteShortcut.HDMI1))
            },
        )
    }

    private fun smartMediaProfile(extraCommands: Set<RemoteCommand>): RemoteProfile {
        val evidence = ProfileEvidence(EvidenceTier.SOURCE_VERIFIED, "Task 7 owned-section fixture")
        val supported = setOf(RemoteCommand.SOURCE) + extraCommands
        return RemoteProfile(
            id = "smart-owned-${extraCommands.sortedBy { it.name }.joinToString("-") { it.name }}",
            brand = "Test",
            displayName = "Smart owned actions",
            modelAliases = listOf("Test TV"),
            remoteModel = "TEST-OWNED",
            defaultEvidence = evidence,
            commands = supported.associateWith { command ->
                CommandBinding(Rc5IrCommand(address = 0, command = command.ordinal % 128), evidence)
            },
            layout = RemoteLayoutSpec.defaultFor(RemoteLayoutTemplate.SMART_MEDIA),
            inputCapability = InputCapability.sourceOnly(),
        )
    }
}
