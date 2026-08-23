package com.erkanpulat.tvkumandam.domain.model

import java.util.Collections

class SavedRemote(
    val id: String,
    val name: String,
    val profileId: String,
    quickActions: List<RemoteAction> = emptyList(),
    val isConfirmed: Boolean = false,
    macros: List<SavedMacro> = emptyList(),
) {
    val quickActions: List<RemoteAction> =
        Collections.unmodifiableList(quickActions.toList())
    val macros: List<SavedMacro> = Collections.unmodifiableList(macros.toList())

    init {
        require(id.isNotBlank()) { "Saved remote id cannot be blank." }
        require(name.isNotBlank()) { "Saved remote name cannot be blank." }
        require(profileId.isNotBlank()) { "Saved remote profile id cannot be blank." }
        require(this.quickActions.size <= MAX_QUICK_ACTIONS) { "At most four quick actions are allowed." }
        require(this.quickActions.distinct().size == this.quickActions.size) {
            "Quick actions must be unique."
        }
        require(RemoteAction.Command(RemoteCommand.POWER) !in this.quickActions) {
            "Power is never a quick action."
        }
        require(this.macros.size <= MAX_MACROS) { "Too many macros are saved for one remote." }
        require(this.macros.map(SavedMacro::id).distinct().size == this.macros.size) {
            "Macro ids must be unique within a remote."
        }
        val macroIds = this.macros.mapTo(mutableSetOf(), SavedMacro::id)
        require(this.quickActions.filterIsInstance<RemoteAction.Macro>().all { it.macroId in macroIds }) {
            "Pinned macro actions must reference a saved macro."
        }
    }

    override fun equals(other: Any?): Boolean = other is SavedRemote &&
        id == other.id &&
        name == other.name &&
        profileId == other.profileId &&
        quickActions == other.quickActions &&
        isConfirmed == other.isConfirmed &&
        macros == other.macros

    override fun hashCode(): Int =
        listOf(id, name, profileId, quickActions, isConfirmed, macros).hashCode()

    override fun toString(): String =
        "SavedRemote(id=$id, name=$name, profileId=$profileId, quickActions=$quickActions, " +
            "isConfirmed=$isConfirmed, macros=$macros)"

    companion object {
        const val MAX_QUICK_ACTIONS = 4
        const val MAX_MACROS = 12
    }
}
