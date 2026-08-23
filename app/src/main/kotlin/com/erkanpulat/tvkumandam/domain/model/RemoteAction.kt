package com.erkanpulat.tvkumandam.domain.model

/** A logical user action. Protocol details remain behind profile bindings. */
sealed interface RemoteAction {
    data class Command(val command: RemoteCommand) : RemoteAction

    data class Macro(val macroId: String) : RemoteAction {
        init {
            require(macroId.isNotBlank()) { "Macro action id cannot be blank." }
        }
    }

    /** Built-in verified sequences remain separate from user-created macros. */
    data class Shortcut(val shortcut: RemoteShortcut) : RemoteAction
}
