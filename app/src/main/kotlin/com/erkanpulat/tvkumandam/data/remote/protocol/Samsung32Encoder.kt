package com.erkanpulat.tvkumandam.data.remote.protocol

import com.erkanpulat.tvkumandam.domain.model.IrSignal

/** Encodes Samsung32 frames used by sourced Samsung-compatible TV remotes. */
object Samsung32Encoder {
    private const val CARRIER_FREQUENCY_HZ = 38_000
    private const val HEADER_MARK_MICROS = 4_500
    private const val HEADER_SPACE_MICROS = 4_500
    private const val BIT_MARK_MICROS = 560
    private const val ZERO_SPACE_MICROS = 560
    private const val ONE_SPACE_MICROS = 1_690
    private const val BITS_PER_BYTE = 8
    private const val SHORT_ADDRESS_MAX = 0xFF
    private const val ADDRESS_MAX = 0xFFFF
    private const val COMMAND_MAX = 0xFF

    fun encode(address: Int, command: Int): IrSignal {
        validate(address, command)
        val frameBytes = buildFrameBytes(address, command)
        val pattern = ArrayList<Int>(2 + frameBytes.size * BITS_PER_BYTE * 2 + 1)
        pattern += HEADER_MARK_MICROS
        pattern += HEADER_SPACE_MICROS
        frameBytes.forEach { byte ->
            repeat(BITS_PER_BYTE) { bitIndex ->
                pattern += BIT_MARK_MICROS
                pattern += if (((byte shr bitIndex) and 1) == 1) {
                    ONE_SPACE_MICROS
                } else {
                    ZERO_SPACE_MICROS
                }
            }
        }
        pattern += BIT_MARK_MICROS
        return IrSignal(CARRIER_FREQUENCY_HZ, pattern.toIntArray())
    }

    internal fun validate(address: Int, command: Int) {
        require(address in 0..ADDRESS_MAX) { "Samsung32 address must fit in 16 bits." }
        require(command in 0..COMMAND_MAX) { "Samsung32 command must fit in 8 bits." }
    }

    private fun buildFrameBytes(address: Int, command: Int): IntArray {
        val lowAddressByte = address and 0xFF
        val highAddressByte = if (address <= SHORT_ADDRESS_MAX) {
            lowAddressByte
        } else {
            address ushr BITS_PER_BYTE
        }
        return intArrayOf(
            lowAddressByte,
            highAddressByte,
            command,
            command.inv() and 0xFF,
        )
    }
}
