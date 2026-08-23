package com.erkanpulat.tvkumandam.domain.model

sealed interface RemoteAction {
    data class Command(val command: RemoteCommand) : RemoteAction

    data class Macro(val macroId: String) : RemoteAction {
        init {
            require(macroId.isNotBlank()) { "Macro action id cannot be blank." }
        }
    }
}
