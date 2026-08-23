package com.erkanpulat.tvkumandam.domain.model

fun interface IrCommand {
    fun encode(toggle: Boolean): IrSignal

    /** True only when a logical press consumes a protocol toggle bit, such as RC5. */
    val usesToggleBit: Boolean
        get() = false
}
