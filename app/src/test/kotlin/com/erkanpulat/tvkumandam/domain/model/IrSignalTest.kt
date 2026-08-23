package com.erkanpulat.tvkumandam.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IrSignalTest {

    @Test
    fun `pattern must be strictly shorter than Android two second boundary`() {
        // Android ConsumerIrManager transmits only patterns shorter than two seconds.
        // https://developer.android.com/reference/android/hardware/ConsumerIrManager#transmit(int,int[])
        val valid = IrSignal(38_000, intArrayOf(1_000_000, 999_999))

        assertEquals(1_999_999, valid.patternCopy().sum())
        assertThrows(IllegalArgumentException::class.java) {
            IrSignal(38_000, intArrayOf(1_000_000, 1_000_000))
        }
    }
}
