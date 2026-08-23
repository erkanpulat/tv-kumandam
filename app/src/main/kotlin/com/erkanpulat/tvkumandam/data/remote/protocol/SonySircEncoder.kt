package com.erkanpulat.tvkumandam.data.remote.protocol

import com.erkanpulat.tvkumandam.domain.model.IrSignal

enum class SonySircVariant(
    internal val addressBits: Int,
    internal val hasExtension: Boolean,
) {
    SIRC_12(addressBits = 5, hasExtension = false),
    SIRC_15(addressBits = 8, hasExtension = false),
    SIRC_20(addressBits = 5, hasExtension = true),
}

object SonySircEncoder {
    private const val CARRIER_FREQUENCY_HZ = 40_000
    private const val HEADER_MARK_MICROS = 2_400
    private const val HEADER_SPACE_MICROS = 600
    private const val ZERO_MARK_MICROS = 600
    private const val ONE_MARK_MICROS = 1_200
    private const val BIT_SPACE_MICROS = 600
    private const val COMMAND_BITS = 7
    private const val EXTENSION_BITS = 8
    private const val MAX_EXTENSION = 0xFF

    fun encode(
        command: Int,
        address: Int,
        variant: SonySircVariant,
        extended: Int = 0,
    ): IrSignal {
        validate(command, address, variant, extended)
        val bits = buildList {
            appendLsb(command, COMMAND_BITS)
            appendLsb(address, variant.addressBits)
            if (variant.hasExtension) appendLsb(extended, EXTENSION_BITS)
        }
        val pattern = ArrayList<Int>(2 + bits.size * 2)
        pattern += HEADER_MARK_MICROS
        pattern += HEADER_SPACE_MICROS
        bits.forEach { bit ->
            pattern += if (bit == 1) ONE_MARK_MICROS else ZERO_MARK_MICROS
            pattern += BIT_SPACE_MICROS
        }
        return IrSignal(CARRIER_FREQUENCY_HZ, pattern.toIntArray())
    }

    internal fun validate(
        command: Int,
        address: Int,
        variant: SonySircVariant,
        extended: Int,
    ) {
        require(command in 0 until (1 shl COMMAND_BITS)) {
            "Sony SIRC command must fit in 7 bits."
        }
        require(address in 0 until (1 shl variant.addressBits)) {
            "Sony ${variant.name} address is outside its supported width."
        }
        if (variant.hasExtension) {
            require(extended in 0..MAX_EXTENSION) {
                "Sony SIRC20 extension must fit in 8 bits."
            }
        } else {
            require(extended == 0) { "Only Sony SIRC20 carries an extension byte." }
        }
    }

    private fun MutableList<Int>.appendLsb(value: Int, width: Int) {
        repeat(width) { bitIndex -> add((value shr bitIndex) and 1) }
    }
}
