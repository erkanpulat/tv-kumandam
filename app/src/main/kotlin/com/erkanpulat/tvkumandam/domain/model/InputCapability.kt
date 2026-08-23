package com.erkanpulat.tvkumandam.domain.model

import java.util.Collections

enum class InputStrategy {
    NONE,
    DISCRETE_COMMANDS,
    SOURCE_MENU_MACROS,
    SOURCE_ONLY,
}

/** Declares how a profile selects inputs without conflating commands and macros. */
class InputCapability private constructor(
    val strategy: InputStrategy,
    discreteCommands: Set<RemoteCommand>,
    sourceMenuShortcuts: Set<RemoteShortcut>,
) {
    val discreteCommands: Set<RemoteCommand> = immutableSet(discreteCommands)
    val sourceMenuShortcuts: Set<RemoteShortcut> = immutableSet(sourceMenuShortcuts)

    companion object {
        private val HDMI_COMMANDS = setOf(
            RemoteCommand.HDMI1,
            RemoteCommand.HDMI2,
            RemoteCommand.HDMI3,
            RemoteCommand.HDMI4,
        )

        fun discreteCommands(commands: Set<RemoteCommand>): InputCapability {
            require(commands.isNotEmpty()) { "Discrete input capability requires a command." }
            require(commands.all { it in HDMI_COMMANDS }) {
                "Only discrete HDMI commands belong in discrete input metadata."
            }
            return InputCapability(
                strategy = InputStrategy.DISCRETE_COMMANDS,
                discreteCommands = commands,
                sourceMenuShortcuts = emptySet(),
            )
        }

        fun sourceMenuMacros(shortcuts: Set<RemoteShortcut>): InputCapability {
            require(shortcuts.isNotEmpty()) { "Source-menu input capability requires a shortcut." }
            return InputCapability(
                strategy = InputStrategy.SOURCE_MENU_MACROS,
                discreteCommands = emptySet(),
                sourceMenuShortcuts = shortcuts,
            )
        }

        fun sourceOnly(): InputCapability = InputCapability(
            strategy = InputStrategy.SOURCE_ONLY,
            discreteCommands = emptySet(),
            sourceMenuShortcuts = emptySet(),
        )

        fun none(): InputCapability = InputCapability(
            strategy = InputStrategy.NONE,
            discreteCommands = emptySet(),
            sourceMenuShortcuts = emptySet(),
        )

        private fun <T> immutableSet(values: Set<T>): Set<T> =
            Collections.unmodifiableSet(LinkedHashSet(values))
    }
}
