package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.data.remote.protocol.NecAddressMode
import com.erkanpulat.tvkumandam.data.remote.protocol.NecEncoder
import com.erkanpulat.tvkumandam.data.remote.protocol.NecIrCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NecEncoderTest {

    @Test
    fun `standard NEC emits address complements command complements and trailer LSB first`() {
        // Renesas AN-CM-418 section 3: 9ms/4.5ms header, 562.5us pulse-distance
        // data, address/~address/command/~command, final mark.
        // https://www.renesas.com/en/document/apn/cm-418-nec-protocol-implementation-using-slg47011
        val signal = NecEncoder.encode(
            address = 0x34,
            command = 0xA2,
            addressMode = NecAddressMode.STANDARD,
        )

        assertEquals(38_000, signal.carrierFrequencyHz)
        assertEquals(67, signal.patternCopy().size)
        assertEquals(
            listOf(
                9_000, 4_500,
                560, 560, 560, 560, 560, 1_690, 560, 560,
                560, 1_690, 560, 1_690, 560, 560, 560, 560,
                560, 1_690, 560, 1_690, 560, 560, 560, 1_690,
                560, 560, 560, 560, 560, 1_690, 560, 1_690,
                560, 560, 560, 1_690, 560, 560, 560, 560,
                560, 560, 560, 1_690, 560, 560, 560, 1_690,
                560, 1_690, 560, 560, 560, 1_690, 560, 1_690,
                560, 1_690, 560, 560, 560, 1_690, 560, 560,
                560,
            ),
            signal.patternCopy().toList(),
        )
    }

    @Test
    fun `extended NEC emits the literal sixteen bit address instead of its complement`() {
        val signal = NecEncoder.encode(
            address = 0x1234,
            command = 0xA2,
            addressMode = NecAddressMode.EXTENDED,
        )

        assertEquals(67, signal.patternCopy().size)
        assertEquals(
            listOf(
                9_000, 4_500,
                560, 560, 560, 560, 560, 1_690, 560, 560,
                560, 1_690, 560, 1_690, 560, 560, 560, 560,
                560, 560, 560, 1_690, 560, 560, 560, 560,
                560, 1_690, 560, 560, 560, 560, 560, 560,
                560, 560, 560, 1_690, 560, 560, 560, 560,
                560, 560, 560, 1_690, 560, 560, 560, 1_690,
                560, 1_690, 560, 560, 560, 1_690, 560, 1_690,
                560, 1_690, 560, 560, 560, 1_690, 560, 560,
                560,
            ),
            signal.patternCopy().toList(),
        )
    }

    @Test
    fun `NEC validates address mode and command widths at its command boundary`() {
        assertThrows(IllegalArgumentException::class.java) {
            NecIrCommand(0x100, 0, NecAddressMode.STANDARD)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NecIrCommand(0x1_0000, 0, NecAddressMode.EXTENDED)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NecIrCommand(0, 0x100, NecAddressMode.STANDARD)
        }
    }

    @Test
    fun `NEC command does not treat controller toggle as protocol data`() {
        val command = NecIrCommand(0x34, 0xA2, NecAddressMode.STANDARD)

        assertEquals(command.encode(false), command.encode(true))
    }
}
