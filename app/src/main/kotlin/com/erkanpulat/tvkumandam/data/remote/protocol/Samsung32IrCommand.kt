package com.erkanpulat.tvkumandam.data.remote.protocol

import com.erkanpulat.tvkumandam.domain.model.IrCommand
import com.erkanpulat.tvkumandam.domain.model.IrSignal

data class Samsung32IrCommand(
    val address: Int,
    val command: Int,
) : IrCommand {
    init {
        Samsung32Encoder.validate(address, command)
    }

    override fun encode(toggle: Boolean): IrSignal = Samsung32Encoder.encode(address, command)
}
