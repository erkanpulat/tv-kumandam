package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence

object GrundigCompatibleProfile {
    const val ID = "grundig-compatible"
    private val sourceEvidence = ProfileEvidence(
        tier = EvidenceTier.SOURCE_VERIFIED,
        sourceReference = "Flipper IRDB MIT: Grundig 1786 XM / 1 3018",
    )

    val profile = RcYc1ProfileFamily.createProfile(
        id = ID,
        brand = "Grundig",
        displayName = "Grundig 1786 XM / 1 3018 (RC-YC1)",
        modelAliases = listOf("1786 XM", "1 3018"),
        compatibleBrands = emptyList(),
        evidence = sourceEvidence,
    )
}
