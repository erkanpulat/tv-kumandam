package com.erkanpulat.tvkumandam.domain.model

import java.util.Collections

/** One user-defined logical button press inside a macro. */
data class SavedMacroStep(
    val command: RemoteCommand,
    val repeatCount: Int = 1,
    val delayAfterMillis: Long = DEFAULT_DELAY_MILLIS,
) {
    init {
        require(command != RemoteCommand.POWER) { "Power cannot be used in a macro." }
        require(repeatCount in 1..MAX_REPEAT_COUNT) { "Macro repeat count is out of range." }
        require(delayAfterMillis in 0L..MAX_DELAY_MILLIS) { "Macro delay is out of range." }
    }

    companion object {
        const val DEFAULT_DELAY_MILLIS = 300L
        const val MAX_REPEAT_COUNT = 10
        const val MAX_DELAY_MILLIS = 3_000L
    }
}

/** A bounded, locally stored sequence that can be pinned like a normal remote action. */
class SavedMacro(
    val id: String,
    val name: String,
    steps: List<SavedMacroStep>,
) {
    val steps: List<SavedMacroStep> = Collections.unmodifiableList(steps.toList())
    val logicalPressCount: Int = this.steps.sumOf(SavedMacroStep::repeatCount)

    init {
        require(id.isNotBlank()) { "Macro id cannot be blank." }
        require(name.isNotBlank() && name.length <= MAX_NAME_LENGTH) { "Macro name is invalid." }
        require(this.steps.isNotEmpty()) { "A macro requires at least one step." }
        require(this.steps.size <= MAX_STEPS) { "A macro has too many steps." }
        require(logicalPressCount <= MAX_LOGICAL_PRESSES) { "A macro has too many logical presses." }
    }

    override fun equals(other: Any?): Boolean = other is SavedMacro &&
        id == other.id && name == other.name && steps == other.steps

    override fun hashCode(): Int = listOf(id, name, steps).hashCode()

    override fun toString(): String = "SavedMacro(id=$id, name=$name, steps=$steps)"

    companion object {
        const val MAX_NAME_LENGTH = 32
        const val MAX_STEPS = 16
        const val MAX_LOGICAL_PRESSES = 32
    }
}
