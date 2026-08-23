package com.erkanpulat.tvkumandam.data.remote.protocol

import com.erkanpulat.tvkumandam.domain.model.IrCommand
import com.erkanpulat.tvkumandam.domain.model.IrSignal

/** A compact RC5 command definition retained as editable profile data. */
data class Rc5IrCommand(
    val address: Int,
    val command: Int,
) : IrCommand {
    override val usesToggleBit: Boolean = true

    override fun encode(toggle: Boolean): IrSignal = Rc5Encoder.encode(address, command, toggle)
}
