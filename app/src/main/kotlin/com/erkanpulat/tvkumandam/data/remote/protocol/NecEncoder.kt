package com.erkanpulat.tvkumandam.data.remote.protocol

import com.erkanpulat.tvkumandam.domain.model.IrSignal

enum class NecAddressMode {
    STANDARD,
    EXTENDED,
}

object NecEncoder {
    private const val CARRIER_FREQUENCY_HZ = 38_000
    private const val HEADER_MARK_MICROS = 9_000
    private const val HEADER_SPACE_MICROS = 4_500
    private const val BIT_MARK_MICROS = 560
    private const val ZERO_SPACE_MICROS = 560
    private const val ONE_SPACE_MICROS = 1_690
    private const val BITS_PER_BYTE = 8
    private const val STANDARD_MAX_ADDRESS = 0xFF
    private const val EXTENDED_MAX_ADDRESS = 0xFFFF
    private const val MAX_COMMAND = 0xFF

    fun encode(address: Int, command: Int, addressMode: NecAddressMode): IrSignal {
        validate(address, command, addressMode)
        val frameBytes = buildFrameBytes(address, command, addressMode)
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

    internal fun validate(address: Int, command: Int, addressMode: NecAddressMode) {
        val addressRange = when (addressMode) {
            NecAddressMode.STANDARD -> 0..STANDARD_MAX_ADDRESS
            NecAddressMode.EXTENDED -> 0..EXTENDED_MAX_ADDRESS
        }
        require(address in addressRange) {
            "NEC ${addressMode.name.lowercase()} address is outside its supported width."
        }
        require(command in 0..MAX_COMMAND) { "NEC command must fit in 8 bits." }
    }

    private fun buildFrameBytes(
        address: Int,
        command: Int,
        addressMode: NecAddressMode,
    ): IntArray {
        val secondAddressByte = when (addressMode) {
            NecAddressMode.STANDARD -> address.inv() and 0xFF
            NecAddressMode.EXTENDED -> address ushr BITS_PER_BYTE
        }
        return intArrayOf(
            address and 0xFF,
            secondAddressByte,
            command,
            command.inv() and 0xFF,
        )
    }
}
