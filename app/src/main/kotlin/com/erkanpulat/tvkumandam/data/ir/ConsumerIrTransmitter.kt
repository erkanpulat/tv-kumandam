package com.erkanpulat.tvkumandam.data.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import com.erkanpulat.tvkumandam.domain.model.IrSignal
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import com.erkanpulat.tvkumandam.domain.remote.IrTransmitter

class ConsumerIrTransmitter internal constructor(
    private val hardware: ConsumerIrHardware,
) : IrTransmitter {
    constructor(context: Context) : this(
        AndroidConsumerIrHardware(
            context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager,
        ),
    )

    override val isAvailable: Boolean
        get() = try {
            hardware.hasEmitter
        } catch (_: RuntimeException) {
            false
        }

    override fun transmit(signal: IrSignal): TransmissionResult {
        if (!isAvailable) return TransmissionResult.UnsupportedDevice

        return try {
            val advertisedRanges = hardware.carrierFrequencyRanges
            if (
                advertisedRanges != null &&
                advertisedRanges.none { signal.carrierFrequencyHz in it }
            ) {
                return TransmissionResult.UnsupportedCarrier(signal.carrierFrequencyHz)
            }
            hardware.transmit(signal.carrierFrequencyHz, signal.patternCopy())
            TransmissionResult.Success
        } catch (error: RuntimeException) {
            TransmissionResult.PlatformFailure(
                error.message ?: "Android IR servisi komutu gönderemedi.",
            )
        }
    }
}
