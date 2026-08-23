package com.erkanpulat.tvkumandam.data.remote.protocol

import com.erkanpulat.tvkumandam.domain.model.IrCommand
import com.erkanpulat.tvkumandam.domain.model.IrSignal

data class NecIrCommand(
    val address: Int,
    val command: Int,
    val addressMode: NecAddressMode,
) : IrCommand {
    init {
        NecEncoder.validate(address, command, addressMode)
    }

    override fun encode(toggle: Boolean): IrSignal = NecEncoder.encode(address, command, addressMode)
}
