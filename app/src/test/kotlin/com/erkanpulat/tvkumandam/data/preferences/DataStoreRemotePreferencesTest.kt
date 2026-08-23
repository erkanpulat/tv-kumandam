package com.erkanpulat.tvkumandam.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.erkanpulat.tvkumandam.data.remote.BekoCompatibleProfile
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
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreRemotePreferencesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `fresh store is a no device snapshot with deterministic defaults`() = runTest {
        withPreferences("defaults.preferences_pb") { preferences ->
            assertEquals(RemoteSettings(), preferences.settings.first())
        }
    }

    @Test
    fun `full current snapshot round trips ordered remotes actions and preferences`() = runTest {
        val hdmi = SavedMacro("hdmi1", "HDMI 1", listOf(SavedMacroStep(RemoteCommand.SOURCE)))
        val expected = RemoteSettings(
            savedRemotes = listOf(
                SavedRemote(
                    "salon",
                    "Salon TV",
                    ArcelikOldLcdProfile.ID,
                    listOf(
                        RemoteAction.Macro(hdmi.id),
                        RemoteAction.Command(RemoteCommand.SOURCE),
                    ),
                    isConfirmed = true,
                    macros = listOf(hdmi),
                ),
                SavedRemote("mutfak", "Mutfak", ArcelikOldLcdProfile.ID, emptyList()),
            ),
            selectedSavedRemoteId = "mutfak",
            theme = ThemePreference.DARK,
            hapticsEnabled = false,
            handedness = Handedness.LEFT,
            onboardingCompleted = true,
        )
        withPreferences("round-trip.preferences_pb") { preferences ->
            preferences.save(expected)
            assertEquals(expected, preferences.settings.first())
        }
    }

    @Test
    fun `atomic update applies quick actions to latest snapshot without losing concurrent fields`() = runTest {
        val salon = SavedRemote(
            "salon",
            "Salon TV",
            ArcelikOldLcdProfile.ID,
            listOf(RemoteAction.Command(RemoteCommand.SOURCE)),
        )
        val mutfak = SavedRemote("mutfak", "Mutfak", ArcelikOldLcdProfile.ID)
        val initial = RemoteSettings(
            savedRemotes = listOf(salon, mutfak),
            selectedSavedRemoteId = salon.id,
            theme = ThemePreference.DARK,
        )
        val preferences = DataStoreRemotePreferences(InMemoryPreferencesDataStore(), RemoteProfileCatalog())
        run {
            preferences.save(initial)

            val renamedSalon = SavedRemote(
                salon.id,
                "Yeni Salon",
                salon.profileId,
                salon.quickActions,
                salon.isConfirmed,
            )
            preferences.save(
                initial.copy(
                    savedRemotes = listOf(mutfak, renamedSalon),
                    theme = ThemePreference.LIGHT,
                    handedness = Handedness.LEFT,
                ),
            )

            val macro = SavedMacro("hdmi1", "HDMI 1", listOf(SavedMacroStep(RemoteCommand.SOURCE)))
            val hdmi = RemoteAction.Macro(macro.id)
            val committed = preferences.update { current ->
                current.copy(
                    savedRemotes = current.savedRemotes.map { remote ->
                        if (remote.id == salon.id) {
                            SavedRemote(
                                remote.id,
                                remote.name,
                                remote.profileId,
                                listOf(hdmi, RemoteAction.Command(RemoteCommand.SOURCE)),
                                remote.isConfirmed,
                                listOf(macro),
                            )
                        } else {
                            remote
                        }
                    },
                )
            }

            assertEquals(listOf("mutfak", "salon"), committed!!.savedRemotes.map(SavedRemote::id))
            assertEquals("Yeni Salon", committed.savedRemotes.first { it.id == "salon" }.name)
            assertEquals(
                listOf(hdmi, RemoteAction.Command(RemoteCommand.SOURCE)),
                committed.savedRemotes.first { it.id == "salon" }.quickActions,
            )
            assertEquals(ThemePreference.LIGHT, committed.theme)
            assertEquals(Handedness.LEFT, committed.handedness)
            assertEquals(committed, preferences.settings.first())
        }
    }

    @Test
    fun `failed oversized save keeps the last persisted snapshot`() = runTest {
        val persisted = RemoteSettings(
            savedRemotes = listOf(SavedRemote("one", "One", ArcelikOldLcdProfile.ID)),
            theme = ThemePreference.DARK,
        )
        val oversized = RemoteSettings(
            savedRemotes = (0..64).map { index ->
                SavedRemote("id$index", "TV $index", ArcelikOldLcdProfile.ID)
            },
        )
        withPreferences("oversized-save.preferences_pb") { preferences ->
            preferences.save(persisted)

            try {
                preferences.save(oversized)
                fail("Expected the writer to reject an unreadable snapshot.")
            } catch (_: IllegalArgumentException) {
                // expected
            }

            assertEquals(persisted, preferences.settings.first())
        }
    }

    @Test
    fun `missing selection falls back to first valid remote and current malformed payload is safe`() = runTest {
        withDataStore("normalization.preferences_pb") { dataStore ->
            dataStore.edit { values ->
                values[stringPreferencesKey("remote_settings")] =
                    "1|s=${"missing".toToken()}|t=SYSTEM|h=1|r=RIGHT|o=0|d=" +
                        "${"one".toToken()}~${"One".toToken()}~${BekoCompatibleProfile.ID.toToken()}~0~~;" +
                        "${"unknown".toToken()}~${"Bad".toToken()}~${"unknown".toToken()}~0~C:SOURCE~"
            }
            assertEquals("one", DataStoreRemotePreferences(dataStore, RemoteProfileCatalog()).settings.first().selectedSavedRemoteId)
        }
        withDataStore("malformed.preferences_pb") { dataStore ->
            dataStore.edit { values -> values[stringPreferencesKey("remote_settings")] = "broken" }
            assertEquals(RemoteSettings(), DataStoreRemotePreferences(dataStore, RemoteProfileCatalog()).settings.first())
        }
    }

    @Test
    fun `physically corrupted preferences file is replaced with empty preferences`() = runTest {
        val file = File(temporaryFolder.root, "corrupt.preferences_pb")
        file.writeBytes(byteArrayOf(0x08, 0xFF.toByte(), 0x00))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = { file },
        )
        try {
            assertEquals(RemoteSettings(), DataStoreRemotePreferences(dataStore, RemoteProfileCatalog()).settings.first())
        } finally {
            scope.cancel()
        }
    }

    private suspend fun withPreferences(
        fileName: String,
        block: suspend (DataStoreRemotePreferences) -> Unit,
    ) {
        withDataStore(fileName) { dataStore -> block(DataStoreRemotePreferences(dataStore, RemoteProfileCatalog())) }
    }

    private suspend fun withDataStore(
        fileName: String,
        block: suspend (androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) -> Unit,
    ) {
        val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { File(temporaryFolder.root, fileName) },
        )
        try {
            block(dataStore)
        } finally {
            dataStoreScope.cancel()
        }
    }

    private fun String.toToken(): String = buildString {
        this@toToken.forEach { character ->
            if (character.isLetterOrDigit() || character == '.' || character == '_') append(character)
            else append('^').append(character.code.toString(16).uppercase().padStart(4, '0'))
        }
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val mutex = Mutex()
        private val mutableData = MutableStateFlow<Preferences>(emptyPreferences())
        override val data = mutableData

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = mutex.withLock {
            transform(mutableData.value).also { mutableData.value = it }
        }
    }
}
