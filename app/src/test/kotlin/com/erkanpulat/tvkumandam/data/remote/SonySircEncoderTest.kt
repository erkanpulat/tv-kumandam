package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.data.remote.protocol.SonySircEncoder
import com.erkanpulat.tvkumandam.data.remote.protocol.SonySircIrCommand
import com.erkanpulat.tvkumandam.data.remote.protocol.SonySircVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SonySircEncoderTest {

    @Test
    fun `SIRC 12 15 and 20 emit literal LSB-first pulse-width frames`() {
        // Linux's mature Sony decoder uses 600us units for SIRC12/15/20;
        // the protocol sends 7 command bits then address/extension LSB first.
        // https://github.com/torvalds/linux/blob/master/drivers/media/rc/img-ir/img-ir-sony.c
        val vectors = listOf(
            SircVector(
                command = SonySircIrCommand(0x15, 0x1A, SonySircVariant.SIRC_12),
                expectedPattern = intArrayOf(
                    2_400, 600, 1_200, 600, 600, 600, 1_200, 600,
                    600, 600, 1_200, 600, 600, 600, 600, 600,
                    600, 600, 1_200, 600, 600, 600, 1_200, 600,
                    1_200, 600,
                ),
            ),
            SircVector(
                command = SonySircIrCommand(0x15, 0xA5, SonySircVariant.SIRC_15),
                expectedPattern = intArrayOf(
                    2_400, 600, 1_200, 600, 600, 600, 1_200, 600,
                    600, 600, 1_200, 600, 600, 600, 600, 600,
                    1_200, 600, 600, 600, 1_200, 600, 600, 600,
                    600, 600, 1_200, 600, 600, 600, 1_200, 600,
                ),
            ),
            SircVector(
                command = SonySircIrCommand(0x15, 0x1A, SonySircVariant.SIRC_20, 0xA5),
                expectedPattern = intArrayOf(
                    2_400, 600, 1_200, 600, 600, 600, 1_200, 600,
                    600, 600, 1_200, 600, 600, 600, 600, 600,
                    600, 600, 1_200, 600, 600, 600, 1_200, 600,
                    1_200, 600, 1_200, 600, 600, 600, 1_200, 600,
                    600, 600, 600, 600, 1_200, 600, 600, 600,
                    1_200, 600,
                ),
            ),
        )

        vectors.forEach { vector ->
            val signal = vector.command.encode(toggle = false)

            assertEquals(40_000, signal.carrierFrequencyHz)
            assertEquals(vector.expectedPattern.toList(), signal.patternCopy().toList())
            assertEquals(signal, vector.command.encode(toggle = true))
        }
    }

    @Test
    fun `SIRC validates command address extension and variant widths`() {
        assertThrows(IllegalArgumentException::class.java) {
            SonySircIrCommand(0x80, 0, SonySircVariant.SIRC_12)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SonySircIrCommand(0, 0x20, SonySircVariant.SIRC_12)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SonySircIrCommand(0, 0x100, SonySircVariant.SIRC_15)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SonySircIrCommand(0, 0, SonySircVariant.SIRC_12, extended = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SonySircEncoder.encode(0, 0, SonySircVariant.SIRC_20, extended = 0x100)
        }
    }

    private data class SircVector(
        val command: SonySircIrCommand,
        val expectedPattern: IntArray,
    )
}
