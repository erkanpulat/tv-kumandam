package com.erkanpulat.tvkumandam.presentation.customize

import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import java.util.Collections

/** Result of one bounded, profile-aware quick-access edit. */
class QuickActionEdit internal constructor(
    actions: List<RemoteAction>,
    val accepted: Boolean,
) {
    val actions: List<RemoteAction> = Collections.unmodifiableList(actions.toList())
}

/** Pure rules shared by drag, accessibility actions, persistence, and the picker. */
object QuickActionEditor {
    fun availableActions(
        profile: RemoteProfile,
        macros: List<SavedMacro> = emptyList(),
    ): List<RemoteAction> = buildList {
        addAll(
            profile.supportedCommands
                .filterNot { it == RemoteCommand.POWER }
                .map(RemoteAction::Command),
        )
        addAll(macros.map { RemoteAction.Macro(it.id) })
    }

    fun normalize(
        profile: RemoteProfile,
        actions: List<RemoteAction>,
        macros: List<SavedMacro> = emptyList(),
    ): List<RemoteAction> =
        actions
            .filter { it in availableActions(profile, macros) }
            .distinct()
            .take(SavedRemote.MAX_QUICK_ACTIONS)

    fun add(
        profile: RemoteProfile,
        actions: List<RemoteAction>,
        action: RemoteAction,
        macros: List<SavedMacro> = emptyList(),
    ): QuickActionEdit {
        val normalized = normalize(profile, actions, macros)
        if (action !in availableActions(profile, macros) || action in normalized ||
            normalized.size >= SavedRemote.MAX_QUICK_ACTIONS
        ) {
            return rejected(normalized)
        }
        return accepted(normalized + action)
    }

    fun remove(
        profile: RemoteProfile,
        actions: List<RemoteAction>,
        action: RemoteAction,
        macros: List<SavedMacro> = emptyList(),
    ): QuickActionEdit {
        val normalized = normalize(profile, actions, macros)
        if (action !in normalized) return rejected(normalized)
        return accepted(normalized.filterNot { it == action })
    }

    fun replace(
        profile: RemoteProfile,
        actions: List<RemoteAction>,
        index: Int,
        replacement: RemoteAction,
        macros: List<SavedMacro> = emptyList(),
    ): QuickActionEdit {
        val normalized = normalize(profile, actions, macros)
        if (index !in normalized.indices || replacement !in availableActions(profile, macros)) {
            return rejected(normalized)
        }
        if (replacement in normalized && normalized[index] != replacement) return rejected(normalized)
        return accepted(normalized.toMutableList().apply { this[index] = replacement })
    }

    fun move(
        profile: RemoteProfile,
        actions: List<RemoteAction>,
        fromIndex: Int,
        toIndex: Int,
        macros: List<SavedMacro> = emptyList(),
    ): QuickActionEdit {
        val normalized = normalize(profile, actions, macros)
        if (fromIndex !in normalized.indices || toIndex !in normalized.indices || fromIndex == toIndex) {
            return rejected(normalized)
        }
        return accepted(
            normalized.toMutableList().apply {
                val action = removeAt(fromIndex)
                add(toIndex, action)
            },
        )
    }

    fun moveLeft(
        profile: RemoteProfile,
        actions: List<RemoteAction>,
        action: RemoteAction,
        macros: List<SavedMacro> = emptyList(),
    ): QuickActionEdit = moveBy(profile, actions, action, -1, macros)

    fun moveRight(
        profile: RemoteProfile,
        actions: List<RemoteAction>,
        action: RemoteAction,
        macros: List<SavedMacro> = emptyList(),
    ): QuickActionEdit = moveBy(profile, actions, action, 1, macros)

    fun moveToTop(
        profile: RemoteProfile,
        actions: List<RemoteAction>,
        action: RemoteAction,
        macros: List<SavedMacro> = emptyList(),
    ): QuickActionEdit {
        val normalized = normalize(profile, actions, macros)
        val fromIndex = normalized.indexOf(action)
        return if (fromIndex <= 0) rejected(normalized) else move(profile, normalized, fromIndex, 0, macros)
    }

    fun reset(profile: RemoteProfile, actions: List<RemoteAction>): QuickActionEdit =
        accepted(normalize(profile, profile.layout.defaultQuickActions))

    private fun moveBy(
        profile: RemoteProfile,
        actions: List<RemoteAction>,
        action: RemoteAction,
        delta: Int,
        macros: List<SavedMacro>,
    ): QuickActionEdit {
        val normalized = normalize(profile, actions, macros)
        val fromIndex = normalized.indexOf(action)
        if (fromIndex == -1) return rejected(normalized)
        return move(profile, normalized, fromIndex, fromIndex + delta, macros)
    }

    private fun accepted(actions: List<RemoteAction>): QuickActionEdit = QuickActionEdit(actions, true)

    private fun rejected(actions: List<RemoteAction>): QuickActionEdit = QuickActionEdit(actions, false)
}
