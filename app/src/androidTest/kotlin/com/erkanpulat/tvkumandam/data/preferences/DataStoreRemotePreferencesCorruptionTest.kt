package com.erkanpulat.tvkumandam.data.preferences

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.preferences.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DataStoreRemotePreferencesCorruptionTest {
    @Test
    fun production_context_datastore_repairs_a_corrupt_file_before_writing_a_snapshot() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // This is the production singleton file. No second DataStore is opened for it; cleanup happens
        // before the first DataStoreRemotePreferences(context, ...) access in this instrumentation process.
        val file = context.preferencesDataStoreFile("remote_preferences")
        check(!file.exists() || file.delete()) { "Could not reset the isolated test preferences file." }
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(0x08, 0xFF.toByte(), 0x00))

        val preferences = DataStoreRemotePreferences(context, RemoteProfileCatalog())
        assertEquals(RemoteSettings(), preferences.settings.first())

        val expected = RemoteSettings(
            theme = ThemePreference.DARK,
            hapticsEnabled = false,
            handedness = Handedness.LEFT,
            onboardingCompleted = true,
        )
        preferences.save(expected)

        assertEquals(expected, preferences.settings.first())
    }
}
