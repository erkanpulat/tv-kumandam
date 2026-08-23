package com.erkanpulat.tvkumandam.domain.model

import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SavedMacroTest {
    @Test
    fun `macro expands repeats and applies RC5 reliability frames`() {
        val macro = SavedMacro(
            "hdmi1",
            "HDMI 1",
            listOf(
                SavedMacroStep(RemoteCommand.SOURCE, delayAfterMillis = 1_000),
                SavedMacroStep(RemoteCommand.DOWN, repeatCount = 7),
                SavedMacroStep(RemoteCommand.OK),
            ),
        )

        val sequence = requireNotNull(ArcelikOldLcdProfile.profile.sequenceFor(macro))

        assertEquals(9, sequence.steps.size)
        assertEquals(2, sequence.steps.first().transmissionCount)
        assertEquals(89L, sequence.steps.first().transmissionGapMillis)
    }

    @Test
    fun `power and unbounded values are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            SavedMacroStep(RemoteCommand.POWER)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SavedMacroStep(RemoteCommand.DOWN, repeatCount = SavedMacroStep.MAX_REPEAT_COUNT + 1)
        }
    }
}
