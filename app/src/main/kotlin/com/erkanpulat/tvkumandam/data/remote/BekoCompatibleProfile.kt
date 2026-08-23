package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence

object BekoCompatibleProfile {
    const val ID = "beko-compatible"
    private val sourceEvidence = ProfileEvidence(
        tier = EvidenceTier.SOURCE_VERIFIED,
        sourceReference = "Beko F 82-507 B 3HD manual and RC-YC1 family",
    )

    val profile = RcYc1ProfileFamily.createProfile(
        id = ID,
        brand = "Beko",
        displayName = "Beko F 82-507 B 3HD (RC-YC1)",
        modelAliases = listOf("F 82-507 B 3HD"),
        compatibleBrands = emptyList(),
        evidence = sourceEvidence,
    )
}
