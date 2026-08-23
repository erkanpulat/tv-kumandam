package com.erkanpulat.tvkumandam.presentation.remote

import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.data.remote.protocol.Rc5IrCommand
import com.erkanpulat.tvkumandam.domain.model.CommandBinding
import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.InputCapability
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteShortcut
import com.erkanpulat.tvkumandam.domain.model.RemoteSection
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutSpec
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.preferences.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteUiStateTest {

    @Test
    fun `selected saved remote projects typed order profile layout evidence and settings`() {
        val catalog = RemoteProfileCatalog()
        val remote = SavedRemote(
            id = "salon",
            name = "Salon TV",
            profileId = ArcelikOldLcdProfile.ID,
            quickActions = listOf(
                RemoteAction.Shortcut(RemoteShortcut.HDMI1),
                RemoteAction.Command(RemoteCommand.SOURCE),
                RemoteAction.Command(RemoteCommand.MENU),
            ),
            isConfirmed = true,
        )
        val settings = RemoteSettings(
            savedRemotes = listOf(remote),
            selectedSavedRemoteId = remote.id,
            theme = ThemePreference.DARK,
            hapticsEnabled = false,
            handedness = Handedness.LEFT,
        )

        val state = RemoteUiState.fromSettings(
            profiles = catalog.profiles,
            settings = settings,
            isIrAvailable = true,
        )

        assertSame(remote, state.selectedRemote)
        assertEquals(remote.quickActions, state.quickActions)
        assertEquals(ArcelikOldLcdProfile.profile.layout.sections, state.layout?.sections)
        assertSame(settings, state.settings)
        assertFalse(state.hapticsEnabled)
        assertEquals(Handedness.LEFT, state.handedness)
    }

    @Test
    fun `no selected TV remains an explicit safe state`() {
        val catalog = RemoteProfileCatalog()

        val state = RemoteUiState.fromSettings(
            profiles = catalog.profiles,
            settings = RemoteSettings(
                theme = ThemePreference.LIGHT,
                hapticsEnabled = false,
                handedness = Handedness.LEFT,
            ),
            isIrAvailable = true,
        )

        assertNull(state.selectedRemote)
        assertNull(state.selectedProfile)
        assertNull(state.layout)
        assertTrue(state.quickActions.isEmpty())
        assertEquals(ThemePreference.LIGHT, state.settings.theme)
    }

    @Test
    fun `every launch command and shortcut has distinct real Turkish copy`() {
        RemoteCommand.entries.forEach { command ->
            val copy = RemoteActionPresentation.forCommand(command)
            assertTrue("Missing label for $command", copy.label.isNotBlank())
            assertTrue("Missing description for $command", copy.description.isNotBlank())
            assertFalse("Fallback label for $command", copy.label.contains("kullanılam", true))
            assertFalse("Fallback description for $command", copy.description.contains("kullanılam", true))
            assertFalse("Description repeats label for $command", copy.label == copy.description)
        }

        RemoteShortcut.entries.forEach { shortcut ->
            val copy = RemoteActionPresentation.forShortcut(shortcut)
            assertTrue("Missing label for $shortcut", copy.label.isNotBlank())
            assertTrue("Missing description for $shortcut", copy.description.isNotBlank())
        }
    }

    @Test
    fun `presentation filters unsupported actions and preserves template section order`() {
        val profile = ArcelikOldLcdProfile.profile

        val sections = RemoteActionPresentation.visibleSections(profile)

        assertEquals(profile.layout.sections.filterNot { it == RemoteSection.ADVANCED }, sections)
        assertFalse(RemoteCommand.MUTE in RemoteActionPresentation.supportedAdvancedCommands(profile))
        assertTrue(
            RemoteAction.Command(RemoteCommand.POWER) !in
                RemoteActionPresentation.quickActions(profile, profile.layout.defaultQuickActions),
        )
    }

    @Test
    fun `advanced section is based on commands left after owned smart sections`() {
        val ownedOnly = smartProfile(setOf(RemoteCommand.DIGIT_1, RemoteCommand.PLAY))

        assertFalse(RemoteSection.ADVANCED in RemoteActionPresentation.visibleSections(ownedOnly))
        assertTrue(RemoteActionPresentation.effectiveAdvancedCommands(ownedOnly).isEmpty())

        val withExit = smartProfile(
            setOf(RemoteCommand.DIGIT_1, RemoteCommand.PLAY, RemoteCommand.EXIT),
        )
        assertTrue(RemoteSection.ADVANCED in RemoteActionPresentation.visibleSections(withExit))
        assertEquals(
            listOf(RemoteCommand.EXIT),
            RemoteActionPresentation.effectiveAdvancedCommands(withExit),
        )
    }

    private fun smartProfile(extraCommands: Set<RemoteCommand>): RemoteProfile {
        val evidence = ProfileEvidence(EvidenceTier.SOURCE_VERIFIED, "Advanced state fixture")
        val commands = setOf(RemoteCommand.SOURCE) + extraCommands
        return RemoteProfile(
            id = "advanced-${extraCommands.size}",
            brand = "Test",
            displayName = "Advanced fixture",
            modelAliases = listOf("Fixture"),
            remoteModel = null,
            defaultEvidence = evidence,
            commands = commands.associateWith { command ->
                CommandBinding(Rc5IrCommand(address = 0, command = command.ordinal % 128), evidence)
            },
            layout = RemoteLayoutSpec.defaultFor(RemoteLayoutTemplate.SMART_MEDIA),
            inputCapability = InputCapability.sourceOnly(),
        )
    }
}
