package com.erkanpulat.tvkumandam.domain.model

/** Encodes a logical remote command into a transmission-ready IR signal. */
fun interface IrCommand {
    fun encode(toggle: Boolean): IrSignal

    /** True only when a logical press consumes a protocol toggle bit, such as RC5. */
    val usesToggleBit: Boolean
        get() = false
}
