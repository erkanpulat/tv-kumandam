package com.erkanpulat.tvkumandam.domain.model

import java.util.Collections

enum class InputStrategy {
    NONE,
    DISCRETE_COMMANDS,
    SOURCE_ONLY,
}

class InputCapability private constructor(
    val strategy: InputStrategy,
    discreteCommands: Set<RemoteCommand>,
) {
    val discreteCommands: Set<RemoteCommand> = immutableSet(discreteCommands)

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
            )
        }

        fun sourceOnly(): InputCapability = InputCapability(
            strategy = InputStrategy.SOURCE_ONLY,
            discreteCommands = emptySet(),
        )

        fun none(): InputCapability = InputCapability(
            strategy = InputStrategy.NONE,
            discreteCommands = emptySet(),
        )

        private fun <T> immutableSet(values: Set<T>): Set<T> =
            Collections.unmodifiableSet(LinkedHashSet(values))
    }
}
