package com.erkanpulat.tvkumandam.data.ir

import com.erkanpulat.tvkumandam.domain.model.IrSignal
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsumerIrTransmitterTest {

    @Test
    fun `device without an emitter returns unsupported without transmitting`() {
        val hardware = FakeConsumerIrHardware(hasEmitter = false)
        val transmitter = ConsumerIrTransmitter(hardware)

        val result = transmitter.transmit(TEST_SIGNAL)

        assertFalse(transmitter.isAvailable)
        assertEquals(TransmissionResult.UnsupportedDevice, result)
        assertEquals(0, hardware.transmissionCount)
    }

    @Test
    fun `available emitter receives the signal`() {
        val hardware = FakeConsumerIrHardware(hasEmitter = true)
        val transmitter = ConsumerIrTransmitter(hardware)

        val result = transmitter.transmit(TEST_SIGNAL)

        assertTrue(transmitter.isAvailable)
        assertEquals(TransmissionResult.Success, result)
        assertEquals(TEST_SIGNAL.carrierFrequencyHz, hardware.lastFrequency)
        assertArrayEquals(TEST_SIGNAL.patternCopy(), hardware.lastPattern)
    }

    @Test
    fun `advertised carrier ranges reject an unsupported frequency without transmitting`() {
        val hardware = FakeConsumerIrHardware(
            hasEmitter = true,
            carrierFrequencyRanges = listOf(
                IrCarrierFrequencyRange(30_000, 35_000),
                IrCarrierFrequencyRange(40_000, 45_000),
            ),
        )

        val result = ConsumerIrTransmitter(hardware).transmit(TEST_SIGNAL)

        assertEquals(TransmissionResult.UnsupportedCarrier(36_000), result)
        assertEquals(0, hardware.transmissionCount)
    }

    @Test
    fun `advertised carrier range endpoints are inclusive`() {
        val hardware = FakeConsumerIrHardware(
            hasEmitter = true,
            carrierFrequencyRanges = listOf(IrCarrierFrequencyRange(36_000, 36_000)),
        )

        assertEquals(
            TransmissionResult.Success,
            ConsumerIrTransmitter(hardware).transmit(TEST_SIGNAL),
        )
        assertEquals(1, hardware.transmissionCount)
    }

    @Test
    fun `missing advertised ranges do not invent unsupported carrier status`() {
        val hardware = FakeConsumerIrHardware(
            hasEmitter = true,
            carrierFrequencyRanges = null,
        )

        assertEquals(
            TransmissionResult.Success,
            ConsumerIrTransmitter(hardware).transmit(TEST_SIGNAL),
        )
        assertEquals(1, hardware.transmissionCount)
    }

    @Test
    fun `carrier range validates positive ordered endpoints`() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            IrCarrierFrequencyRange(0, 36_000)
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            IrCarrierFrequencyRange(40_000, 36_000)
        }
    }

    @Test
    fun `platform exception is converted to a typed failure`() {
        val hardware = FakeConsumerIrHardware(
            hasEmitter = true,
            failure = IllegalStateException("Emitter is busy"),
        )

        val result = ConsumerIrTransmitter(hardware).transmit(TEST_SIGNAL)

        assertEquals(TransmissionResult.PlatformFailure("Emitter is busy"), result)
    }

    @Test
    fun `availability probe exception is treated as unsupported`() {
        val failingHardware = object : ConsumerIrHardware {
            override val hasEmitter: Boolean
                get() = throw IllegalStateException("IR service died")

            override fun transmit(carrierFrequencyHz: Int, patternMicros: IntArray) = Unit
        }
        val transmitter = ConsumerIrTransmitter(failingHardware)

        assertFalse(transmitter.isAvailable)
        assertEquals(
            TransmissionResult.UnsupportedDevice,
            transmitter.transmit(TEST_SIGNAL),
        )
    }

    private class FakeConsumerIrHardware(
        override val hasEmitter: Boolean,
        override val carrierFrequencyRanges: List<IrCarrierFrequencyRange>? = null,
        private val failure: RuntimeException? = null,
    ) : ConsumerIrHardware {
        var transmissionCount = 0
            private set
        var lastFrequency: Int? = null
            private set
        var lastPattern: IntArray? = null
            private set

        override fun transmit(carrierFrequencyHz: Int, patternMicros: IntArray) {
            failure?.let { throw it }
            transmissionCount += 1
            lastFrequency = carrierFrequencyHz
            lastPattern = patternMicros.copyOf()
        }
    }

    private companion object {
        val TEST_SIGNAL = IrSignal(
            carrierFrequencyHz = 36_000,
            patternMicros = intArrayOf(889, 889, 1_778, 889),
        )
    }
}
