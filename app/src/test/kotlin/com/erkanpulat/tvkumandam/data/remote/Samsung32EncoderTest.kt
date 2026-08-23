package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.data.remote.protocol.Samsung32Encoder
import com.erkanpulat.tvkumandam.data.remote.protocol.Samsung32IrCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Samsung32EncoderTest {

    @Test
    fun `Samsung32 emits duplicated short address and complemented command LSB first`() {
        // Arduino-IRremote's mature Samsung implementation documents the
        // 4.5ms header, LSB order, duplicated 8-bit address and ~command byte.
        // https://github.com/Arduino-IRremote/Arduino-IRremote/blob/master/src/ir_Samsung.hpp
        val signal = Samsung32Encoder.encode(address = 0x07, command = 0x02)

        assertEquals(38_000, signal.carrierFrequencyHz)
        assertEquals(67, signal.patternCopy().size)
        assertEquals(
            listOf(
                4_500, 4_500,
                560, 1_690, 560, 1_690, 560, 1_690, 560, 560,
                560, 560, 560, 560, 560, 560, 560, 560,
                560, 1_690, 560, 1_690, 560, 1_690, 560, 560,
                560, 560, 560, 560, 560, 560, 560, 560,
                560, 560, 560, 1_690, 560, 560, 560, 560,
                560, 560, 560, 560, 560, 560, 560, 560,
                560, 1_690, 560, 560, 560, 1_690, 560, 1_690,
                560, 1_690, 560, 1_690, 560, 1_690, 560, 1_690,
                560,
            ),
            signal.patternCopy().toList(),
        )
    }

    @Test
    fun `Samsung32 accepts a sourced sixteen bit address and validates all widths`() {
        assertEquals(
            67,
            Samsung32IrCommand(address = 0xE0E0, command = 0x40)
                .encode(toggle = false)
                .patternCopy()
                .size,
        )
        assertThrows(IllegalArgumentException::class.java) {
            Samsung32IrCommand(address = -1, command = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Samsung32IrCommand(address = 0x1_0000, command = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Samsung32IrCommand(address = 0, command = 0x100)
        }
    }

    @Test
    fun `Samsung32 preserves distinct low and high bytes of a sixteen bit address`() {
        val signal = Samsung32Encoder.encode(address = 0x1234, command = 0xA2)

        assertEquals(38_000, signal.carrierFrequencyHz)
        assertEquals(
            listOf(
                4_500, 4_500,
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
    fun `Samsung32 command ignores the RC5 controller toggle`() {
        val command = Samsung32IrCommand(address = 0x07, command = 0x02)

        assertEquals(command.encode(false), command.encode(true))
    }
}
