package com.erkanpulat.tvkumandam.data.remote.protocol

import com.erkanpulat.tvkumandam.domain.model.IrCommand
import com.erkanpulat.tvkumandam.domain.model.IrSignal

/**
 * Validated boundary for immutable raw timing records bundled with sourced profiles.
 * It is internal so arbitrary user-provided timing text cannot enter the transmitter.
 */
internal class RawIrCommand(
    carrierFrequencyHz: Int,
    patternMicros: IntArray,
) : IrCommand {
    private val signal = IrSignal(carrierFrequencyHz, patternMicros)

    override fun encode(toggle: Boolean): IrSignal = signal
}
