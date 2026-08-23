package com.erkanpulat.tvkumandam.data.remote.profiles

import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand

private fun officialManual(reference: String) = ProfileEvidence(
    tier = EvidenceTier.SOURCE_VERIFIED,
    sourceReference = reference,
)

object Lg50Pc1Dr42Lb1DrProfile {
    const val ID = "lg-50pc1dr-42lb1dr"

    val profile = LgProfileFamily.create(
        id = ID,
        displayName = "LG 50PC1DR / 42LB1DR",
        modelAliases = listOf(
            "50PC1DR",
            "50PC1DRA",
            "50PC1DR-UA",
            "50PC1DRA-UA",
            "42LB1DR",
            "42LB1DRA",
        ),
        evidence = officialManual("LG 50PC1DR/42LB1DR owner manual IR codes"),
        hdmiCommands = linkedMapOf(
            RemoteCommand.HDMI1 to 0xC6,
            RemoteCommand.HDMI2 to 0xCC,
        ),
    )
}

object Lg32Lc50CbProfile {
    const val ID = "lg-32lc50cb"

    val profile = LgProfileFamily.create(
        id = ID,
        displayName = "LG 32LC50CB",
        modelAliases = listOf("32LC50CB"),
        evidence = officialManual("LG 32LC50CB owner manual IR codes"),
        extraCommands = mapOf(RemoteCommand.INFO to 0xAA),
        hdmiCommands = linkedMapOf(
            RemoteCommand.HDMI1 to 0xCE,
            RemoteCommand.HDMI2 to 0xCC,
        ),
    )
}

object LgLc2DPc3DProfile {
    const val ID = "lg-lc2d-pc3d"

    val profile = LgProfileFamily.create(
        id = ID,
        displayName = "LG LC2D / PC3D / PC1D",
        modelAliases = listOf(
            "32LC2D",
            "32LC2DU",
            "37LC2D",
            "42LC2D",
            "42PC3D",
            "42PC3DV",
            "50PC3D",
            "60PC1D",
        ),
        evidence = officialManual("LG LC2D/PC3D/PC1D owner manual IR codes"),
        extraCommands = mapOf(RemoteCommand.INFO to 0xAA),
        hdmiCommands = linkedMapOf(
            RemoteCommand.HDMI1 to 0xCE,
            RemoteCommand.HDMI2 to 0xCC,
        ),
    )
}

object Lg70Profile {
    const val ID = "lg-lg70"

    val profile = LgProfileFamily.create(
        id = ID,
        displayName = "LG LG70 Serisi",
        modelAliases = listOf("32LG70", "42LG70", "47LG70", "52LG70"),
        evidence = officialManual("LG LG70 series owner manual IR codes"),
        extraCommands = mapOf(
            RemoteCommand.PLAY to 0xB0,
            RemoteCommand.PAUSE to 0xBA,
            RemoteCommand.STOP to 0xB1,
            RemoteCommand.REWIND to 0x8F,
            RemoteCommand.FAST_FORWARD to 0x8E,
        ),
        hdmiCommands = linkedMapOf(
            RemoteCommand.HDMI1 to 0xCE,
            RemoteCommand.HDMI2 to 0xCC,
            RemoteCommand.HDMI3 to 0xE9,
            RemoteCommand.HDMI4 to 0xDA,
        ),
    )
}
