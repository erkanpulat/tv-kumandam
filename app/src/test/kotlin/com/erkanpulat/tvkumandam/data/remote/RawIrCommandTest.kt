package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.data.remote.protocol.RawIrCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RawIrCommandTest {

    @Test
    fun `sourced raw command validates and owns its timing record`() {
        // Raw timings are bundled profile records, never parsed from user input.
        // Flipper IR file format: first duration is a mark and timings alternate.
        // https://github.com/flipperdevices/flipperzero-firmware/blob/dev/documentation/file_formats/InfraredFileFormats.md
        val sourceRecord = intArrayOf(9_000, 4_500, 560, 560, 560)
        val command = RawIrCommand(38_000, sourceRecord)
        sourceRecord[0] = 1

        assertEquals(
            listOf(9_000, 4_500, 560, 560, 560),
            command.encode(toggle = false).patternCopy().toList(),
        )
        assertEquals(
            command.encode(toggle = false),
            command.encode(toggle = true),
        )
        assertThrows(IllegalArgumentException::class.java) {
            RawIrCommand(38_000, intArrayOf(560, 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            RawIrCommand(38_000, intArrayOf(1_000_000, 1_000_000))
        }
    }
}
