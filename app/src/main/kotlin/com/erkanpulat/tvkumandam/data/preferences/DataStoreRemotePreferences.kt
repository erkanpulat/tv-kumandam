package com.erkanpulat.tvkumandam.data.preferences

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.remotePreferencesDataStore by preferencesDataStore(
    name = "remote_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class DataStoreRemotePreferences internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val catalog: RemoteProfileCatalog = RemoteProfileCatalog(),
) : RemotePreferences {
    constructor(context: Context, catalog: RemoteProfileCatalog) : this(context.remotePreferencesDataStore, catalog)

    private val normalizer = RemoteSettingsNormalizer(catalog)

    override val settings: Flow<RemoteSettings> = dataStore.data
        .catch { error ->
            when (error) {
                is CorruptionException -> throw error
                is IOException -> emit(emptyPreferences())
                else -> throw error
            }
        }
        .map(::toRemoteSettings)

    override suspend fun save(settings: RemoteSettings) {
        val normalized = normalizer.normalize(settings)
        dataStore.edit { preferences ->
            write(preferences, normalized)
        }
    }

    override suspend fun update(
        transform: (RemoteSettings) -> RemoteSettings?,
    ): RemoteSettings? {
        var committed: RemoteSettings? = null
        dataStore.edit { preferences ->
            val current = toRemoteSettings(preferences)
            val requested = transform(current) ?: return@edit
            val normalized = normalizer.normalize(requested)
            write(preferences, normalized)
            committed = normalized
        }
        return committed
    }

    private fun write(preferences: androidx.datastore.preferences.core.MutablePreferences, settings: RemoteSettings) {
        preferences[Keys.CURRENT_SETTINGS] = RemoteSettingsCodec.encode(settings)
    }

    private fun toRemoteSettings(preferences: Preferences): RemoteSettings {
        preferences[Keys.CURRENT_SETTINGS]?.let { payload ->
            return RemoteSettingsCodec.decode(payload, normalizer)
        }
        return RemoteSettings()
    }

    private object Keys {
        val CURRENT_SETTINGS = stringPreferencesKey("remote_settings")
    }
}
