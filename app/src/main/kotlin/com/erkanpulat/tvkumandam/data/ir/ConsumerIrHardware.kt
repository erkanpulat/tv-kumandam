package com.erkanpulat.tvkumandam.data.ir

import android.hardware.ConsumerIrManager

internal interface ConsumerIrHardware {
    val hasEmitter: Boolean
    val carrierFrequencyRanges: List<IrCarrierFrequencyRange>?
        get() = null

    fun transmit(carrierFrequencyHz: Int, patternMicros: IntArray)
}

internal data class IrCarrierFrequencyRange(
    val minimumHz: Int,
    val maximumHz: Int,
) {
    init {
        require(minimumHz > 0) { "Minimum IR carrier frequency must be positive." }
        require(maximumHz >= minimumHz) {
            "Maximum IR carrier frequency cannot be below the minimum."
        }
    }

    operator fun contains(frequencyHz: Int): Boolean = frequencyHz in minimumHz..maximumHz
}

internal class AndroidConsumerIrHardware(
    private val manager: ConsumerIrManager?,
) : ConsumerIrHardware {
    override val hasEmitter: Boolean
        get() = manager?.hasIrEmitter() == true

    override val carrierFrequencyRanges: List<IrCarrierFrequencyRange>?
        get() = manager?.carrierFrequencies?.map { range ->
            IrCarrierFrequencyRange(
                minimumHz = range.minFrequency,
                maximumHz = range.maxFrequency,
            )
        }

    override fun transmit(carrierFrequencyHz: Int, patternMicros: IntArray) {
        checkNotNull(manager) { "Consumer IR service is unavailable." }
            .transmit(carrierFrequencyHz, patternMicros)
    }
}
