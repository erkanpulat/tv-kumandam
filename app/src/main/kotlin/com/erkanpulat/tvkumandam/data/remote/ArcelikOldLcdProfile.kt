package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence

/**
 * RC-YC1 code-family profile physically verified by the project owner on an
 * Arçelik 32HDR. Command data is also cross-checked against the open Flipper
 * IRDB Grundig RC5 set (address 0).
 */
object ArcelikOldLcdProfile {
    const val ID = "arcelik-old-lcd"
    private val verifiedEvidence = ProfileEvidence(
        tier = EvidenceTier.DEVICE_VERIFIED,
        sourceReference = "Project owner physical test: Arçelik 32HDR with RC-YC1 family",
    )

    val profile = RcYc1ProfileFamily.createProfile(
        id = ID,
        brand = "Arçelik",
        displayName = "Arçelik Eski LCD (RC-YC1)",
        modelAliases = listOf("82-507 B", "32HDR"),
        compatibleBrands = emptyList(),
        evidence = verifiedEvidence,
    )
}
