package com.erkanpulat.tvkumandam.presentation.customize

import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.domain.model.CommandBinding
import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.InputCapability
import com.erkanpulat.tvkumandam.domain.model.IrCommand
import com.erkanpulat.tvkumandam.domain.model.IrSignal
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutSpec
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.SavedMacroStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickActionEditorTest {
    @Test
    fun `Power and unsupported actions cannot enter a quick deck`() {
        val profile = ArcelikOldLcdProfile.profile
        val initial = listOf(RemoteAction.Command(RemoteCommand.SOURCE))

        val power = QuickActionEditor.add(profile, initial, RemoteAction.Command(RemoteCommand.POWER))
        val unavailable = QuickActionEditor.add(profile, initial, RemoteAction.Command(RemoteCommand.EXIT))

        assertFalse(power.accepted)
        assertFalse(unavailable.accepted)
        assertEquals(initial, power.actions)
        assertEquals(initial, unavailable.actions)
        assertFalse(RemoteAction.Command(RemoteCommand.POWER) in QuickActionEditor.availableActions(profile))
    }

    @Test
    fun `macro identity and order survive add remove and reset`() {
        val profile = ArcelikOldLcdProfile.profile
        val macro = SavedMacro("hdmi1", "HDMI 1", listOf(SavedMacroStep(RemoteCommand.SOURCE)))
        val hdmi = RemoteAction.Macro(macro.id)
        val source = RemoteAction.Command(RemoteCommand.SOURCE)
        val menu = RemoteAction.Command(RemoteCommand.MENU)

        val added = QuickActionEditor.add(profile, listOf(hdmi, source), menu, listOf(macro))
        val removed = QuickActionEditor.remove(profile, added.actions, source, listOf(macro))
        val reset = QuickActionEditor.reset(profile, emptyList())

        assertTrue(added.accepted)
        assertEquals(listOf(hdmi, source, menu), added.actions)
        assertEquals(listOf(hdmi, menu), removed.actions)
        assertEquals(profile.layout.defaultQuickActions, reset.actions)
        assertEquals(listOf(source), reset.actions)
    }

    @Test
    fun `drag and accessible movement produce identical bounded ordering`() {
        val profile = profileWithFiveSafeCommands()
        val actions = listOf(
            RemoteAction.Command(RemoteCommand.SOURCE),
            RemoteAction.Command(RemoteCommand.MENU),
        )
        val exit = RemoteAction.Command(RemoteCommand.EXIT)

        val byDrag = QuickActionEditor.move(profile, actions + exit, fromIndex = 2, toIndex = 0)
        val byAccessibleActions = QuickActionEditor.moveToTop(profile, actions + exit, exit)
        val full = QuickActionEditor.add(
            profile,
            byDrag.actions,
            RemoteAction.Command(RemoteCommand.VOLUME_UP),
        )
        val fifth = QuickActionEditor.add(
            profile,
            full.actions,
            RemoteAction.Command(RemoteCommand.VOLUME_DOWN),
        )

        assertEquals(byDrag.actions, byAccessibleActions.actions)
        assertEquals(exit, byDrag.actions.first())
        assertTrue(full.accepted)
        assertFalse(fifth.accepted)
        assertEquals(4, full.actions.size)
    }

    @Test
    fun `replace and remove keep uniqueness and reject invalid targets`() {
        val profile = profileWithFiveSafeCommands()
        val source = RemoteAction.Command(RemoteCommand.SOURCE)
        val menu = RemoteAction.Command(RemoteCommand.MENU)
        val exit = RemoteAction.Command(RemoteCommand.EXIT)
        val initial = listOf(source, menu)

        val replaced = QuickActionEditor.replace(profile, initial, 1, exit)
        val duplicate = QuickActionEditor.replace(profile, initial, 1, source)
        val invalidIndex = QuickActionEditor.replace(profile, initial, 9, exit)
        val missingRemove = QuickActionEditor.remove(profile, initial, exit)
        val duplicateAdd = QuickActionEditor.add(profile, initial, source)

        assertTrue(replaced.accepted)
        assertEquals(listOf(source, exit), replaced.actions)
        assertFalse(duplicate.accepted)
        assertFalse(invalidIndex.accepted)
        assertFalse(missingRemove.accepted)
        assertFalse(duplicateAdd.accepted)
        assertEquals(initial, duplicate.actions)
    }

    @Test
    fun `left right and top operations have truthful boundaries`() {
        val profile = profileWithFiveSafeCommands()
        val source = RemoteAction.Command(RemoteCommand.SOURCE)
        val menu = RemoteAction.Command(RemoteCommand.MENU)
        val exit = RemoteAction.Command(RemoteCommand.EXIT)
        val initial = listOf(source, menu, exit)

        val left = QuickActionEditor.moveLeft(profile, initial, menu)
        val right = QuickActionEditor.moveRight(profile, initial, menu)

        assertEquals(listOf(menu, source, exit), left.actions)
        assertEquals(listOf(source, exit, menu), right.actions)
        assertFalse(QuickActionEditor.moveLeft(profile, initial, source).accepted)
        assertFalse(QuickActionEditor.moveRight(profile, initial, exit).accepted)
        assertFalse(QuickActionEditor.moveToTop(profile, initial, source).accepted)
    }

    private fun profileWithFiveSafeCommands(): RemoteProfile {
        val evidence = ProfileEvidence(EvidenceTier.SOURCE_VERIFIED, "Quick-action editor fixture")
        val commands = listOf(
            RemoteCommand.SOURCE,
            RemoteCommand.MENU,
            RemoteCommand.BACK,
            RemoteCommand.EXIT,
            RemoteCommand.VOLUME_UP,
            RemoteCommand.VOLUME_DOWN,
        )
        return RemoteProfile(
            id = "quick-action-fixture",
            brand = "Test",
            displayName = "Quick action fixture",
            modelAliases = listOf("Fixture"),
            remoteModel = null,
            defaultEvidence = evidence,
            commands = commands.associateWith { command ->
                CommandBinding(IrCommand { IrSignal(38_000, intArrayOf(560, 560)) }, evidence)
            },
            layout = RemoteLayoutSpec.defaultFor(RemoteLayoutTemplate.CLASSIC_DPAD),
            inputCapability = InputCapability.sourceOnly(),
        )
    }
}
