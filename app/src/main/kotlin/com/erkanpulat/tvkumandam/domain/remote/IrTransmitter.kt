package com.erkanpulat.tvkumandam.domain.remote

import com.erkanpulat.tvkumandam.domain.model.IrSignal
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult

/** Platform boundary for the Android ConsumerIrManager implementation. */
interface IrTransmitter {
    val isAvailable: Boolean

    fun transmit(signal: IrSignal): TransmissionResult
}
