package com.erkanpulat.tvkumandam.data.remote.profiles

import com.erkanpulat.tvkumandam.data.remote.protocol.NecAddressMode
import com.erkanpulat.tvkumandam.data.remote.protocol.NecIrCommand
import com.erkanpulat.tvkumandam.data.remote.protocol.Samsung32IrCommand
import com.erkanpulat.tvkumandam.domain.model.CommandBinding
import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.InputCapability
import com.erkanpulat.tvkumandam.domain.model.IrCommand
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutSpec
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile

private fun communityEvidence(remoteModel: String) = ProfileEvidence(
    tier = EvidenceTier.SOURCE_VERIFIED,
    sourceReference = "Flipper-IRDB CC0: $remoteModel",
)

private fun communityProfile(
    id: String,
    brand: String,
    displayName: String,
    modelAliases: List<String>,
    remoteModel: String,
    commands: LinkedHashMap<RemoteCommand, Int>,
    encoder: (Int) -> IrCommand,
    template: RemoteLayoutTemplate,
    defaultQuickActions: List<RemoteAction>,
): RemoteProfile {
    val evidence = communityEvidence(remoteModel)
    return RemoteProfile(
        id = id,
        brand = brand,
        displayName = displayName,
        modelAliases = modelAliases,
        remoteModel = remoteModel,
        remoteAliases = listOf(remoteModel),
        defaultEvidence = evidence,
        commands = commands.mapValuesTo(linkedMapOf()) { (_, command) ->
            CommandBinding(encoder(command), evidence)
        },
        layout = RemoteLayoutSpec.defaultFor(template, defaultQuickActions),
        inputCapability = InputCapability.sourceOnly(),
    )
}

object SamsungAa5900484AProfile {
    const val ID = "samsung-aa59-00484a"

    val profile = communityProfile(
        id = ID,
        brand = "Samsung",
        displayName = "Samsung AA59-00484A",
        modelAliases = listOf(
            "5WXXH",
            "LE19D450G",
            "LE19D450G1W",
            "LE19D451G3W",
            "LE22D450G1W",
            "LE22D451G3W",
            "LE26D450G",
            "LE26D450G1W",
            "LE32D450G",
            "LE32D450G1W",
            "LE32D550K",
            "LE32D550K1W",
            "LE32D551K2W",
            "LE32D570K2S",
            "LE32D579K",
            "LE32D580K",
            "LE37B550A",
            "LE37D550",
            "LE37D550K",
            "LE37D550K1W",
            "LE37D551K2W",
            "LE37D570K2",
            "LE37D570K2S",
            "LE37D579K",
            "LE37D580K",
            "LE40D550K",
            "LE40D550K1W",
            "LE40D550K2W",
            "LE40D551K2W",
            "LE40D570K2S",
            "LE40D579K",
            "LE40D580K",
            "LE46D550K",
            "LE46D550K1W",
            "LE46D551K2W",
            "LE46D570K2S",
            "LE46D580K",
            "PS43D450A",
            "PS43D450A2W",
            "PS43D451A3W",
            "PS43D452A5W",
            "PS51D450A",
            "PS51D450A2W",
            "PS51D451A3W",
            "PS51D452A5W",
        ),
        remoteModel = "AA59-00484A",
        commands = linkedMapOf(
            RemoteCommand.POWER to 0x02,
            RemoteCommand.SOURCE to 0x01,
            RemoteCommand.MUTE to 0x0F,
            RemoteCommand.VOLUME_UP to 0x07,
            RemoteCommand.VOLUME_DOWN to 0x0B,
            RemoteCommand.CHANNEL_UP to 0x12,
            RemoteCommand.CHANNEL_DOWN to 0x10,
            RemoteCommand.MENU to 0x1A,
            RemoteCommand.GUIDE to 0x4F,
            RemoteCommand.INFO to 0x1F,
            RemoteCommand.OK to 0x68,
            RemoteCommand.LEFT to 0x65,
            RemoteCommand.UP to 0x60,
            RemoteCommand.RIGHT to 0x62,
            RemoteCommand.DOWN to 0x61,
            RemoteCommand.BACK to 0x58,
            RemoteCommand.EXIT to 0x2D,
            RemoteCommand.PLAY to 0x47,
            RemoteCommand.PAUSE to 0x4A,
            RemoteCommand.REWIND to 0x45,
            RemoteCommand.FAST_FORWARD to 0x48,
            RemoteCommand.STOP to 0x46,
            RemoteCommand.RED to 0x6C,
            RemoteCommand.GREEN to 0x14,
            RemoteCommand.YELLOW to 0x15,
            RemoteCommand.BLUE to 0x16,
            RemoteCommand.DIGIT_0 to 0x11,
            RemoteCommand.DIGIT_1 to 0x04,
            RemoteCommand.DIGIT_2 to 0x05,
            RemoteCommand.DIGIT_3 to 0x06,
            RemoteCommand.DIGIT_4 to 0x08,
            RemoteCommand.DIGIT_5 to 0x09,
            RemoteCommand.DIGIT_6 to 0x0A,
            RemoteCommand.DIGIT_7 to 0x0C,
            RemoteCommand.DIGIT_8 to 0x0D,
            RemoteCommand.DIGIT_9 to 0x0E,
        ),
        encoder = { Samsung32IrCommand(address = 0x07, command = it) },
        template = RemoteLayoutTemplate.SMART_MEDIA,
        defaultQuickActions = listOf(
            RemoteAction.Command(RemoteCommand.SOURCE),
            RemoteAction.Command(RemoteCommand.MENU),
        ),
    )
}

object ToshibaCt8560Profile {
    const val ID = "toshiba-ct-8560"

    val profile = communityProfile(
        id = ID,
        brand = "Toshiba",
        displayName = "Toshiba CT-8560",
        modelAliases = emptyList(),
        remoteModel = "CT-8560",
        commands = linkedMapOf(
            RemoteCommand.POWER to 0x12,
            RemoteCommand.SOURCE to 0x14,
            RemoteCommand.MUTE to 0x10,
            RemoteCommand.VOLUME_UP to 0x1A,
            RemoteCommand.VOLUME_DOWN to 0x1E,
            RemoteCommand.CHANNEL_UP to 0x1B,
            RemoteCommand.CHANNEL_DOWN to 0x1F,
            RemoteCommand.OK to 0x21,
            RemoteCommand.UP to 0x19,
            RemoteCommand.DOWN to 0x1D,
            RemoteCommand.LEFT to 0x42,
            RemoteCommand.RIGHT to 0x40,
            RemoteCommand.BACK to 0x64,
            RemoteCommand.EXIT to 0x43,
            RemoteCommand.PLAY to 0x67,
            RemoteCommand.PAUSE to 0x6A,
            RemoteCommand.STOP to 0x68,
            RemoteCommand.FAST_FORWARD to 0x6B,
            RemoteCommand.REWIND to 0x6C,
        ),
        encoder = { NecIrCommand(0x40, it, NecAddressMode.STANDARD) },
        template = RemoteLayoutTemplate.SMART_MEDIA,
        defaultQuickActions = listOf(
            RemoteAction.Command(RemoteCommand.SOURCE),
            RemoteAction.Command(RemoteCommand.PLAY),
        ),
    )
}

object HitachiCle1031Profile {
    const val ID = "hitachi-cle-1031"

    val profile = communityProfile(
        id = ID,
        brand = "Hitachi",
        displayName = "Hitachi CLE-1031",
        modelAliases = listOf("32FHDSM6", "40FHDSM8", "50UHDSM8", "55UHDSM8"),
        remoteModel = "CLE-1031",
        commands = linkedMapOf(
            RemoteCommand.POWER to 0x12,
            RemoteCommand.SOURCE to 0x14,
            RemoteCommand.MUTE to 0x10,
            RemoteCommand.VOLUME_UP to 0x1A,
            RemoteCommand.VOLUME_DOWN to 0x1E,
            RemoteCommand.CHANNEL_UP to 0x1B,
            RemoteCommand.CHANNEL_DOWN to 0x1F,
            RemoteCommand.OK to 0x0A,
            RemoteCommand.UP to 0x19,
            RemoteCommand.DOWN to 0x1D,
            RemoteCommand.LEFT to 0x46,
            RemoteCommand.RIGHT to 0x47,
            RemoteCommand.MENU to 0x49,
            RemoteCommand.BACK to 0x40,
            RemoteCommand.EXIT to 0x44,
            RemoteCommand.INFO to 0x16,
            RemoteCommand.GUIDE to 0x0B,
        ),
        encoder = { NecIrCommand(0x50, it, NecAddressMode.STANDARD) },
        template = RemoteLayoutTemplate.CLASSIC_DPAD,
        defaultQuickActions = listOf(
            RemoteAction.Command(RemoteCommand.SOURCE),
            RemoteAction.Command(RemoteCommand.MENU),
        ),
    )
}

object JvcLt49Hw97URmC3311Profile {
    const val ID = "jvc-lt-49hw97u-rm-c3311"

    val profile = communityProfile(
        id = ID,
        brand = "JVC",
        displayName = "JVC LT-49HW97U / RM-C3311",
        modelAliases = listOf("LT-49HW97U"),
        remoteModel = "RM-C3311",
        commands = linkedMapOf(
            RemoteCommand.POWER to 0x0C,
            RemoteCommand.SOURCE to 0x0F,
            RemoteCommand.MUTE to 0x0D,
            RemoteCommand.VOLUME_UP to 0x14,
            RemoteCommand.VOLUME_DOWN to 0x15,
            RemoteCommand.CHANNEL_UP to 0x12,
            RemoteCommand.CHANNEL_DOWN to 0x13,
            RemoteCommand.MENU to 0x11,
            RemoteCommand.OK to 0x46,
            RemoteCommand.UP to 0x42,
            RemoteCommand.DOWN to 0x43,
            RemoteCommand.LEFT to 0x44,
            RemoteCommand.RIGHT to 0x45,
            RemoteCommand.HOME to 0x78,
            RemoteCommand.EXIT to 0xB5,
            RemoteCommand.BACK to 0xBB,
            RemoteCommand.DIGIT_0 to 0x00,
            RemoteCommand.DIGIT_1 to 0x01,
            RemoteCommand.DIGIT_2 to 0x02,
            RemoteCommand.DIGIT_3 to 0x03,
            RemoteCommand.DIGIT_4 to 0x04,
            RemoteCommand.DIGIT_5 to 0x05,
            RemoteCommand.DIGIT_6 to 0x06,
            RemoteCommand.DIGIT_7 to 0x07,
            RemoteCommand.DIGIT_8 to 0x08,
            RemoteCommand.DIGIT_9 to 0x09,
        ),
        encoder = { Samsung32IrCommand(address = 0x0E, command = it) },
        template = RemoteLayoutTemplate.FULL_REMOTE,
        defaultQuickActions = listOf(
            RemoteAction.Command(RemoteCommand.SOURCE),
            RemoteAction.Command(RemoteCommand.MENU),
        ),
    )
}
