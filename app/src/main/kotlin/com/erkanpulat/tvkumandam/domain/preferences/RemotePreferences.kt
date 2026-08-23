package com.erkanpulat.tvkumandam.domain.preferences

import kotlinx.coroutines.flow.Flow

/** Atomic saved-remote settings boundary. Tests can provide a small StateFlow-backed fake. */
interface RemotePreferences {
    val settings: Flow<RemoteSettings>

    suspend fun save(settings: RemoteSettings)

    /**
     * Atomically transforms the latest canonical snapshot. Returning null aborts the mutation.
     */
    suspend fun update(transform: (RemoteSettings) -> RemoteSettings?): RemoteSettings?
}
