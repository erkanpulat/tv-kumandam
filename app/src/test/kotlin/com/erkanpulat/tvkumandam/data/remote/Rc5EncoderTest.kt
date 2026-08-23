package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.data.remote.protocol.Rc5Encoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Rc5EncoderTest {

    @Test
    fun `boundary commands preserve standard RC5 and encode the extended field literally`() {
        // NXP AN10210: F=1 selects commands 0..63, F=0 selects 64..127.
        // https://www.nxp.com.cn/docs/en/application-note/AN10210.pdf
        val vectors = listOf(
            Rc5Vector(
                command = 0,
                toggle = false,
                expectedPattern = intArrayOf(
                    889, 889, 1_778, 889, 889, 889, 889, 889, 889, 889, 889,
                    889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889,
                    889, 889, 889,
                ),
            ),
            Rc5Vector(
                command = 63,
                toggle = true,
                expectedPattern = intArrayOf(
                    889, 889, 889, 889, 1_778, 889, 889, 889, 889, 889, 889,
                    889, 889, 1_778, 889, 889, 889, 889, 889, 889, 889, 889,
                    889, 889, 889,
                ),
            ),
            Rc5Vector(
                command = 64,
                toggle = false,
                expectedPattern = intArrayOf(
                    1_778, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889,
                    889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889, 889,
                    889, 889, 889,
                ),
            ),
            Rc5Vector(
                command = 127,
                toggle = true,
                expectedPattern = intArrayOf(
                    1_778, 1_778, 1_778, 889, 889, 889, 889, 889, 889, 889,
                    889, 1_778, 889, 889, 889, 889, 889, 889, 889, 889, 889,
                    889, 889,
                ),
            ),
        )

        vectors.forEach { vector ->
            val signal = Rc5Encoder.encode(
                address = 0,
                command = vector.command,
                toggle = vector.toggle,
            )

            assertEquals(36_000, signal.carrierFrequencyHz)
            assertEquals(
                "command=${vector.command}, toggle=${vector.toggle}",
                vector.expectedPattern.toList(),
                signal.patternCopy().toList(),
            )
        }
    }

    @Test
    fun `power code follows the NXP RC5 Manchester polarity`() {
        val signal = Rc5Encoder.encode(address = 0, command = 0x0C, toggle = false)

        assertEquals(36_000, signal.carrierFrequencyHz)
        assertEquals(
            // S1, S2, toggle, address 0, command 0x0C; MSB first.
            listOf(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0),
            decodeRc5Levels(signal.patternCopy()),
        )
    }

    @Test
    fun `toggle bit changes the encoded signal`() {
        val firstPress = Rc5Encoder.encode(address = 0, command = 0x0C, toggle = false)
        val secondPress = Rc5Encoder.encode(address = 0, command = 0x0C, toggle = true)

        assertNotEquals(firstPress, secondPress)
    }

    @Test
    fun `encoder rejects values outside the RC5 field widths`() {
        assertThrows(IllegalArgumentException::class.java) {
            Rc5Encoder.encode(address = 32, command = 0, toggle = false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Rc5Encoder.encode(address = 0, command = 128, toggle = false)
        }
    }

    /**
     * ConsumerIr starts with carrier-on. RC5's first start bit begins with an
     * idle space, which is intentionally omitted from the Android pattern.
     */
    private fun decodeRc5Levels(pattern: IntArray): List<Int> {
        val levels = buildList {
            add(false) // Restore the omitted leading half-bit space.
            pattern.forEachIndexed { index, durationMicros ->
                assertEquals(0, durationMicros % HALF_BIT_MICROS)
                repeat(durationMicros / HALF_BIT_MICROS) {
                    add(index % 2 == 0) // ConsumerIr alternates mark, space.
                }
            }
        }

        assertEquals(FRAME_HALF_BITS, levels.size)
        return levels.chunked(2).map { (firstHalf, secondHalf) ->
            when (firstHalf to secondHalf) {
                false to true -> 1
                true to false -> 0
                else -> error("Invalid Manchester pair: $firstHalf, $secondHalf")
            }
        }
    }

    private companion object {
        const val HALF_BIT_MICROS = 889
        const val FRAME_HALF_BITS = 28
    }

    private data class Rc5Vector(
        val command: Int,
        val toggle: Boolean,
        val expectedPattern: IntArray,
    )
}
