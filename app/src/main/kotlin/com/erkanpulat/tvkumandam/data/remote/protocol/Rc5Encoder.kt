package com.erkanpulat.tvkumandam.data.remote.protocol

import com.erkanpulat.tvkumandam.domain.model.IrSignal

/** Encodes 14-bit RC5 frames into ConsumerIr mark/space timings. */
object Rc5Encoder {
    private const val CARRIER_FREQUENCY_HZ = 36_000
    private const val HALF_BIT_MICROS = 889
    private const val ADDRESS_BITS = 5
    private const val COMMAND_BITS = 6
    private const val MAX_ADDRESS = (1 shl ADDRESS_BITS) - 1
    private const val MAX_COMMAND = (1 shl (COMMAND_BITS + 1)) - 1

    fun encode(address: Int, command: Int, toggle: Boolean): IrSignal {
        require(address in 0..MAX_ADDRESS) { "RC5 address must fit in 5 bits." }
        require(command in 0..MAX_COMMAND) { "RC5 command must fit in 7 bits." }

        val bits = buildList {
            add(1) // Start bit.
            // NXP AN10210: the inverted seventh command bit is the RC5 field.
            add(if (command < EXTENDED_COMMAND_OFFSET) 1 else 0)
            add(if (toggle) 1 else 0)
            appendBits(address, ADDRESS_BITS)
            appendBits(command, COMMAND_BITS)
        }

        val halfLevels = bits.flatMap { bit ->
            // NXP AN3053: RC5 one is space/mark; zero is mark/space.
            if (bit == 1) listOf(false, true) else listOf(true, false)
        }
        // ConsumerIr patterns must start carrier-on. The leading RC5 space is
        // indistinguishable from the idle line before a frame, so omit it.
        val transmissibleLevels = halfLevels.dropWhile { level -> !level }
        val mergedDurations = mutableListOf<Int>()
        var currentLevel = transmissibleLevels.first()
        var currentDuration = HALF_BIT_MICROS

        transmissibleLevels.drop(1).forEach { level ->
            if (level == currentLevel) {
                currentDuration += HALF_BIT_MICROS
            } else {
                mergedDurations += currentDuration
                currentLevel = level
                currentDuration = HALF_BIT_MICROS
            }
        }
        mergedDurations += currentDuration

        return IrSignal(
            carrierFrequencyHz = CARRIER_FREQUENCY_HZ,
            patternMicros = mergedDurations.toIntArray(),
        )
    }

    private fun MutableList<Int>.appendBits(value: Int, width: Int) {
        for (bitIndex in width - 1 downTo 0) {
            add((value shr bitIndex) and 1)
        }
    }

    private const val EXTENDED_COMMAND_OFFSET = 1 shl COMMAND_BITS
}
