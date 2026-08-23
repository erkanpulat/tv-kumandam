package com.erkanpulat.tvkumandam.data.remote.profiles

import com.erkanpulat.tvkumandam.data.remote.protocol.NecAddressMode
import com.erkanpulat.tvkumandam.data.remote.protocol.NecIrCommand
import com.erkanpulat.tvkumandam.domain.model.CommandBinding
import com.erkanpulat.tvkumandam.domain.model.InputCapability
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutSpec
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile

internal object LgProfileFamily {
    private const val LG_ADDRESS = 0x04

    fun create(
        id: String,
        displayName: String,
        modelAliases: List<String>,
        evidence: ProfileEvidence,
        hdmiCommands: LinkedHashMap<RemoteCommand, Int>,
        extraCommands: Map<RemoteCommand, Int> = emptyMap(),
    ): RemoteProfile {
        val commands = linkedMapOf(
            RemoteCommand.POWER to 0x08,
            RemoteCommand.MUTE to 0x09,
            RemoteCommand.SOURCE to 0x0B,
            RemoteCommand.VOLUME_UP to 0x02,
            RemoteCommand.VOLUME_DOWN to 0x03,
            RemoteCommand.CHANNEL_UP to 0x00,
            RemoteCommand.CHANNEL_DOWN to 0x01,
            RemoteCommand.MENU to 0x43,
            RemoteCommand.OK to 0x44,
            RemoteCommand.UP to 0x40,
            RemoteCommand.DOWN to 0x41,
            RemoteCommand.LEFT to 0x07,
            RemoteCommand.RIGHT to 0x06,
            RemoteCommand.EXIT to 0x5B,
            RemoteCommand.DIGIT_0 to 0x10,
            RemoteCommand.DIGIT_1 to 0x11,
            RemoteCommand.DIGIT_2 to 0x12,
            RemoteCommand.DIGIT_3 to 0x13,
            RemoteCommand.DIGIT_4 to 0x14,
            RemoteCommand.DIGIT_5 to 0x15,
            RemoteCommand.DIGIT_6 to 0x16,
            RemoteCommand.DIGIT_7 to 0x17,
            RemoteCommand.DIGIT_8 to 0x18,
            RemoteCommand.DIGIT_9 to 0x19,
        ).apply {
            putAll(extraCommands)
            putAll(hdmiCommands)
        }
        val discreteInputs = hdmiCommands.keys.toSet()

        return RemoteProfile(
            id = id,
            brand = "LG",
            displayName = displayName,
            modelAliases = modelAliases,
            remoteModel = null,
            defaultEvidence = evidence,
            commands = commands.mapValuesTo(linkedMapOf()) { (_, command) ->
                CommandBinding(
                    irCommand = NecIrCommand(LG_ADDRESS, command, NecAddressMode.STANDARD),
                    evidence = evidence,
                )
            },
            layout = RemoteLayoutSpec.defaultFor(
                template = RemoteLayoutTemplate.DIRECT_INPUT,
                defaultQuickActions = buildList {
                    add(RemoteAction.Command(RemoteCommand.HDMI1))
                    RemoteCommand.HDMI2.takeIf(discreteInputs::contains)?.let {
                        add(RemoteAction.Command(it))
                    }
                    add(RemoteAction.Command(RemoteCommand.SOURCE))
                },
            ),
            inputCapability = InputCapability.discreteCommands(discreteInputs),
        )
    }
}
