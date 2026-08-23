package com.erkanpulat.tvkumandam.domain.model

import java.util.Collections

enum class RemoteShortcut {
    HDMI1,
}

data class RemoteSequenceStep(
    val command: RemoteCommand,
    val delayAfterMillis: Long = 0L,
    val transmissionCount: Int = 1,
    val transmissionGapMillis: Long = 0L,
) {
    init {
        require(delayAfterMillis >= 0L) { "Sequence delay cannot be negative." }
        require(transmissionCount > 0) { "A sequence step requires at least one transmission." }
        require(transmissionGapMillis >= 0L) { "Transmission gap cannot be negative." }
    }
}

class RemoteSequence(steps: List<RemoteSequenceStep>) {
    val steps: List<RemoteSequenceStep> = Collections.unmodifiableList(steps.toList())

    init {
        require(this.steps.isNotEmpty()) { "A remote sequence requires at least one step." }
    }
}
