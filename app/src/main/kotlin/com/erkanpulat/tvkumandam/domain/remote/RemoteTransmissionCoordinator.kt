package com.erkanpulat.tvkumandam.domain.remote

import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteSequence
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Application-scoped suspension gate for the physical IR emitter. A caller owns
 * the emitter for one logical command or one complete sequence, never via a JVM monitor.
 */
class RemoteTransmissionCoordinator(
    private val controller: RemoteController,
) {
    private val mutex = Mutex()

    val isAvailable: Boolean
        get() = controller.isAvailable

    suspend fun send(
        ownerId: String,
        profile: RemoteProfile,
        command: RemoteCommand,
    ): TransmissionResult = mutex.withLock {
        controller.send(ownerId, profile, command)
    }

    suspend fun send(
        ownerId: String,
        profile: RemoteProfile,
        sequence: RemoteSequence,
        onProgress: suspend (SequenceProgress) -> Unit = {},
    ): TransmissionResult = mutex.withLock {
        controller.send(ownerId, profile, sequence, onProgress)
    }
}
