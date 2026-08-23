package com.erkanpulat.tvkumandam.domain.model

enum class EvidenceTier {
    DEVICE_VERIFIED,
    SOURCE_VERIFIED,
    EXPERIMENTAL,
}

/** The confidence tier and traceable source behind a profile or one binding. */
data class ProfileEvidence(
    val tier: EvidenceTier,
    val sourceReference: String,
) {
    init {
        require(sourceReference.isNotBlank()) { "Evidence source reference cannot be blank." }
    }
}

data class CommandBinding(
    val irCommand: IrCommand,
    val evidence: ProfileEvidence,
)

data class ShortcutBinding(
    val sequence: RemoteSequence,
    val evidence: ProfileEvidence,
)
