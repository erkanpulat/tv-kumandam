package com.erkanpulat.tvkumandam.presentation.remote

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.SavedMacroStep
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.ui.theme.TvKumandamTheme

@Preview(name = "Telefon 320 - Karanlık", widthDp = 320, heightDp = 760, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Telefon 390 - Karanlık", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RemoteScreenCompactPreview() {
    TvKumandamTheme(darkTheme = true) {
        RemoteScreen(state = previewState(), onAction = {})
    }
}

@Preview(name = "Tablet 720 - Açık", widthDp = 720, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun RemoteScreenTabletPreview() {
    TvKumandamTheme(darkTheme = false) {
        RemoteScreen(state = previewState(), onAction = {})
    }
}

@Preview(name = "Makro ilerlemesi", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RemoteScreenMacroPreview() {
    TvKumandamTheme(darkTheme = true) {
        RemoteScreen(
            state = previewState().copy(
                transmissionState = TransmissionState.Macro(
                    savedRemoteId = "preview-salon",
                    profileId = ArcelikOldLcdProfile.ID,
                    macroId = "preview-hdmi",
                    macroName = "HDMI 1",
                    completedSteps = 3,
                    totalSteps = 9,
                ),
            ),
            onAction = {},
        )
    }
}

private fun previewState(): RemoteUiState {
    val profile = ArcelikOldLcdProfile.profile
    val remote = SavedRemote(
        id = "preview-salon",
        name = "Salon TV",
        profileId = profile.id,
        quickActions = listOf(
            RemoteAction.Macro("preview-hdmi"),
            RemoteAction.Command(RemoteCommand.SOURCE),
            RemoteAction.Command(RemoteCommand.MENU),
        ),
        isConfirmed = true,
        macros = listOf(
            SavedMacro(
                id = "preview-hdmi",
                name = "HDMI 1",
                steps = listOf(
                    SavedMacroStep(RemoteCommand.SOURCE),
                    SavedMacroStep(RemoteCommand.DOWN, repeatCount = 7),
                    SavedMacroStep(RemoteCommand.OK),
                ),
            ),
        ),
    )
    return RemoteUiState.fromSettings(
        profiles = listOf(profile),
        settings = RemoteSettings(
            savedRemotes = listOf(remote),
            selectedSavedRemoteId = remote.id,
        ),
        isIrAvailable = true,
    )
}
