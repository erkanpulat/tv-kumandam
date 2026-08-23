package com.erkanpulat.tvkumandam.data.remote.protocol

import com.erkanpulat.tvkumandam.domain.model.IrCommand
import com.erkanpulat.tvkumandam.domain.model.IrSignal

data class SonySircIrCommand(
    val command: Int,
    val address: Int,
    val variant: SonySircVariant,
    val extended: Int = 0,
) : IrCommand {
    init {
        SonySircEncoder.validate(command, address, variant, extended)
    }

    override fun encode(toggle: Boolean): IrSignal =
        SonySircEncoder.encode(command, address, variant, extended)
}
