package com.erkanpulat.tvkumandam.data.preferences

import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.SavedMacroStep
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.preferences.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSettingsCodecTest {
    private val normalizer = RemoteSettingsNormalizer(RemoteProfileCatalog())

    @Test
    fun `settings macros and pinned actions round trip without losing order`() {
        val hdmi = SavedMacro(
            id = "hdmi1",
            name = "HDMI 1",
            steps = listOf(
                SavedMacroStep(RemoteCommand.SOURCE, delayAfterMillis = 1_000),
                SavedMacroStep(RemoteCommand.DOWN, repeatCount = 7),
                SavedMacroStep(RemoteCommand.OK),
            ),
        )
        val expected = RemoteSettings(
            savedRemotes = listOf(
                SavedRemote(
                    id = "salon",
                    name = "Salon | TV",
                    profileId = ArcelikOldLcdProfile.ID,
                    quickActions = listOf(
                        RemoteAction.Macro(hdmi.id),
                        RemoteAction.Command(RemoteCommand.SOURCE),
                    ),
                    isConfirmed = true,
                    macros = listOf(hdmi),
                ),
            ),
            selectedSavedRemoteId = "salon",
            theme = ThemePreference.DARK,
            hapticsEnabled = false,
            handedness = Handedness.LEFT,
            onboardingCompleted = true,
        )

        val encoded = RemoteSettingsCodec.encode(expected)

        assertTrue(encoded.startsWith("1|"))
        assertEquals(expected, RemoteSettingsCodec.decode(encoded, normalizer))
        assertEquals(encoded, RemoteSettingsCodec.encode(expected))
    }

    @Test
    fun `empty snapshot preserves preferences`() {
        val expected = RemoteSettings(
            theme = ThemePreference.DARK,
            hapticsEnabled = false,
            handedness = Handedness.LEFT,
            onboardingCompleted = true,
        )

        assertEquals(expected, RemoteSettingsCodec.decode(RemoteSettingsCodec.encode(expected), normalizer))
    }

    @Test
    fun `malformed unknown and oversized payloads fail closed`() {
        listOf(
            "",
            "2|s=-|t=SYSTEM|h=1|r=RIGHT|o=0|d=",
            "1|s=-|t=SYSTEM|h=1|r=RIGHT|o=0|d=broken",
            "x".repeat(RemoteSettingsCodec.MAX_PAYLOAD_CHARS + 1),
        ).forEach { payload ->
            assertEquals(RemoteSettings(), RemoteSettingsCodec.decode(payload, normalizer))
        }
    }

    @Test
    fun `normalizer drops macros with unsupported commands`() {
        val unsupported = SavedMacro(
            "exit",
            "Çıkış",
            listOf(SavedMacroStep(RemoteCommand.EXIT)),
        )
        val settings = RemoteSettings(
            savedRemotes = listOf(
                SavedRemote(
                    "salon",
                    "Salon",
                    ArcelikOldLcdProfile.ID,
                    macros = listOf(unsupported),
                ),
            ),
        )

        val normalized = normalizer.normalize(settings)

        assertTrue(normalized.selectedRemote?.macros.orEmpty().isEmpty())
    }
}
