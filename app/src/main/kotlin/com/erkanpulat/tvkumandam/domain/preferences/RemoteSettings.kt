package com.erkanpulat.tvkumandam.domain.preferences

import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import java.util.Collections

enum class ThemePreference { SYSTEM, LIGHT, DARK }

enum class Handedness { RIGHT, LEFT }

/** Immutable local settings snapshot. The selected id always points at a saved remote when one exists. */
class RemoteSettings(
    savedRemotes: List<SavedRemote> = emptyList(),
    selectedSavedRemoteId: String? = savedRemotes.firstOrNull()?.id,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val hapticsEnabled: Boolean = true,
    val handedness: Handedness = Handedness.RIGHT,
    val onboardingCompleted: Boolean = false,
) {
    val savedRemotes: List<SavedRemote> =
        Collections.unmodifiableList(savedRemotes.toList())
    val selectedSavedRemoteId: String? = selectedSavedRemoteId
        ?.takeIf { selectedId -> savedRemotes.any { it.id == selectedId } }
        ?: this.savedRemotes.firstOrNull()?.id

    val selectedRemote: SavedRemote?
        get() = savedRemotes.firstOrNull { it.id == selectedSavedRemoteId }

    fun copy(
        savedRemotes: List<SavedRemote> = this.savedRemotes,
        selectedSavedRemoteId: String? = this.selectedSavedRemoteId,
        theme: ThemePreference = this.theme,
        hapticsEnabled: Boolean = this.hapticsEnabled,
        handedness: Handedness = this.handedness,
        onboardingCompleted: Boolean = this.onboardingCompleted,
    ): RemoteSettings = RemoteSettings(
        savedRemotes = savedRemotes,
        selectedSavedRemoteId = selectedSavedRemoteId,
        theme = theme,
        hapticsEnabled = hapticsEnabled,
        handedness = handedness,
        onboardingCompleted = onboardingCompleted,
    )

    override fun equals(other: Any?): Boolean = other is RemoteSettings &&
        savedRemotes == other.savedRemotes &&
        selectedSavedRemoteId == other.selectedSavedRemoteId &&
        theme == other.theme &&
        hapticsEnabled == other.hapticsEnabled &&
        handedness == other.handedness &&
        onboardingCompleted == other.onboardingCompleted

    override fun hashCode(): Int = listOf(
        savedRemotes,
        selectedSavedRemoteId,
        theme,
        hapticsEnabled,
        handedness,
        onboardingCompleted,
    ).hashCode()

    override fun toString(): String =
        "RemoteSettings(savedRemotes=$savedRemotes, selectedSavedRemoteId=$selectedSavedRemoteId, " +
            "theme=$theme, hapticsEnabled=$hapticsEnabled, handedness=$handedness, " +
            "onboardingCompleted=$onboardingCompleted)"
}
