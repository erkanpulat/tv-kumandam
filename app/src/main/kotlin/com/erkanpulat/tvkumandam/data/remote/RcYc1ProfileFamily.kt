package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.data.remote.protocol.Rc5IrCommand
import com.erkanpulat.tvkumandam.domain.model.CommandBinding
import com.erkanpulat.tvkumandam.domain.model.InputCapability
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutSpec
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteShortcut
import com.erkanpulat.tvkumandam.domain.model.ShortcutBinding

/** Shared RC5 bindings for the sourced RC-YC1 television code family. */
internal object RcYc1ProfileFamily {
    private const val RC5_TV_ADDRESS = 0

    fun createProfile(
        id: String,
        brand: String,
        displayName: String,
        modelAliases: List<String>,
        compatibleBrands: List<String>,
        evidence: ProfileEvidence,
    ) = RemoteProfile(
        id = id,
        brand = brand,
        displayName = displayName,
        modelAliases = modelAliases,
        remoteModel = "RC-YC1",
        compatibleBrands = compatibleBrands,
        remoteAliases = listOf("RC-YC1"),
        defaultEvidence = evidence,
        commands = linkedMapOf(
            RemoteCommand.POWER to binding(0x0C, evidence),
            RemoteCommand.SOURCE to binding(0x38, evidence),
            RemoteCommand.VOLUME_UP to binding(0x10, evidence),
            RemoteCommand.VOLUME_DOWN to binding(0x11, evidence),
            RemoteCommand.CHANNEL_UP to binding(0x20, evidence),
            RemoteCommand.CHANNEL_DOWN to binding(0x21, evidence),
            RemoteCommand.MENU to binding(0x19, evidence),
            RemoteCommand.OK to binding(0x35, evidence),
            RemoteCommand.UP to binding(0x16, evidence),
            RemoteCommand.DOWN to binding(0x17, evidence),
            RemoteCommand.LEFT to binding(0x13, evidence),
            RemoteCommand.RIGHT to binding(0x12, evidence),
        ),
        shortcuts = linkedMapOf(
            RemoteShortcut.HDMI1 to ShortcutBinding(
                sequence = OldLcdHdmiShortcut.openAndSelectHdmi1,
                evidence = evidence,
            ),
        ),
        layout = RemoteLayoutSpec.defaultFor(
            template = RemoteLayoutTemplate.CLASSIC_DPAD,
            defaultQuickActions = listOf(
                RemoteAction.Command(RemoteCommand.SOURCE),
            ),
        ),
        inputCapability = InputCapability.sourceMenuMacros(setOf(RemoteShortcut.HDMI1)),
    )

    private fun binding(command: Int, evidence: ProfileEvidence) = CommandBinding(
        irCommand = Rc5IrCommand(RC5_TV_ADDRESS, command),
        evidence = evidence,
    )
}
