package com.erkanpulat.tvkumandam.domain.model

import java.util.Collections

enum class RemoteLayoutTemplate {
    CLASSIC_DPAD,
    FULL_REMOTE,
    SMART_MEDIA,
    DIRECT_INPUT,
}

enum class RemoteSection {
    QUICK_ACTIONS,
    NAVIGATION,
    VOLUME_AND_CHANNEL,
    PRIMARY_CONTROLS,
    NUMERIC_KEYPAD,
    COLOR_AND_TELETEXT,
    MEDIA,
    DIRECT_INPUTS,
    ADVANCED,
}

/** Ordered, protocol-free instructions for composing one remote screen. */
class RemoteLayoutSpec(
    val template: RemoteLayoutTemplate,
    sections: List<RemoteSection>,
    defaultQuickActions: List<RemoteAction>,
) {
    val sections: List<RemoteSection> = immutableList(sections)
    val defaultQuickActions: List<RemoteAction> = immutableList(defaultQuickActions)

    init {
        require(this.sections.isNotEmpty()) { "A remote layout requires at least one section." }
        require(this.sections.distinct().size == this.sections.size) {
            "Remote layout sections cannot contain duplicates."
        }
        require(this.defaultQuickActions.size <= MAX_QUICK_ACTIONS) {
            "A remote layout supports at most $MAX_QUICK_ACTIONS default quick actions."
        }
        require(this.defaultQuickActions.distinct().size == this.defaultQuickActions.size) {
            "Default quick actions cannot contain duplicates."
        }
        require(RemoteAction.Command(RemoteCommand.POWER) !in this.defaultQuickActions) {
            "Power is isolated and cannot be a reorderable quick action."
        }
    }

    companion object {
        const val MAX_QUICK_ACTIONS = 4

        fun defaultFor(
            template: RemoteLayoutTemplate,
            defaultQuickActions: List<RemoteAction> = emptyList(),
        ): RemoteLayoutSpec = RemoteLayoutSpec(
            template = template,
            sections = when (template) {
                RemoteLayoutTemplate.CLASSIC_DPAD -> listOf(
                    RemoteSection.QUICK_ACTIONS,
                    RemoteSection.NAVIGATION,
                    RemoteSection.VOLUME_AND_CHANNEL,
                    RemoteSection.PRIMARY_CONTROLS,
                    RemoteSection.ADVANCED,
                )
                RemoteLayoutTemplate.FULL_REMOTE -> listOf(
                    RemoteSection.QUICK_ACTIONS,
                    RemoteSection.NUMERIC_KEYPAD,
                    RemoteSection.NAVIGATION,
                    RemoteSection.VOLUME_AND_CHANNEL,
                    RemoteSection.PRIMARY_CONTROLS,
                    RemoteSection.COLOR_AND_TELETEXT,
                    RemoteSection.MEDIA,
                )
                RemoteLayoutTemplate.SMART_MEDIA -> listOf(
                    RemoteSection.QUICK_ACTIONS,
                    RemoteSection.NAVIGATION,
                    RemoteSection.PRIMARY_CONTROLS,
                    RemoteSection.MEDIA,
                    RemoteSection.VOLUME_AND_CHANNEL,
                    RemoteSection.NUMERIC_KEYPAD,
                    RemoteSection.ADVANCED,
                )
                RemoteLayoutTemplate.DIRECT_INPUT -> listOf(
                    RemoteSection.QUICK_ACTIONS,
                    RemoteSection.DIRECT_INPUTS,
                    RemoteSection.NAVIGATION,
                    RemoteSection.VOLUME_AND_CHANNEL,
                    RemoteSection.PRIMARY_CONTROLS,
                    RemoteSection.ADVANCED,
                )
            },
            defaultQuickActions = defaultQuickActions,
        )

        private fun <T> immutableList(values: List<T>): List<T> =
            Collections.unmodifiableList(values.toList())
    }
}
