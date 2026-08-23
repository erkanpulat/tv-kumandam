package com.erkanpulat.tvkumandam.domain.remote

import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteSequence
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

/** Progress counts completed logical button presses, never reliability frames. */
data class SequenceProgress(
    val completedSteps: Int,
    val totalSteps: Int,
)

/** Resolves profile commands and owns saved-remote-scoped logical toggle state. */
class RemoteController(
    private val transmitter: IrTransmitter,
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    private data class ToggleOwner(val savedRemoteId: String, val profileId: String)

    private val nextToggleByOwner = mutableMapOf<ToggleOwner, Boolean>()

    val isAvailable: Boolean
        get() = transmitter.isAvailable

    @Synchronized
    fun send(
        ownerId: String,
        profile: RemoteProfile,
        command: RemoteCommand,
    ): TransmissionResult {
        if (!transmitter.isAvailable) return TransmissionResult.UnsupportedDevice

        val irCommand = profile.commandFor(command)
            ?: return TransmissionResult.CommandUnavailable
        val owner = ToggleOwner(ownerId, profile.id)
        val signal = runCatching { irCommand.encode(toggleFor(owner, irCommand)) }
            .getOrElse { error ->
                return TransmissionResult.EncodingFailure(
                    error.message ?: "IR komutu sinyale dönüştürülemedi.",
                )
            }

        return transmitter.transmit(signal).also { result ->
            if (result == TransmissionResult.Success) advanceToggle(owner, irCommand)
        }
    }

    suspend fun send(
        ownerId: String,
        profile: RemoteProfile,
        sequence: RemoteSequence,
        onProgress: suspend (SequenceProgress) -> Unit = {},
    ): TransmissionResult {
        val totalSteps = sequence.steps.size
        onProgress(SequenceProgress(completedSteps = 0, totalSteps = totalSteps))

        sequence.steps.forEachIndexed { stepIndex, step ->
            currentCoroutineContext().ensureActive()
            if (!transmitter.isAvailable) return TransmissionResult.UnsupportedDevice

            val irCommand = profile.commandFor(step.command)
                ?: return TransmissionResult.CommandUnavailable
            val owner = ToggleOwner(ownerId, profile.id)
            val signal = runCatching { irCommand.encode(toggleFor(owner, irCommand)) }
                .getOrElse { error ->
                    return TransmissionResult.EncodingFailure(
                        error.message ?: "IR komutu sinyale dönüştürülemedi.",
                    )
                }

            var transmissionSucceeded = false
            try {
                repeat(step.transmissionCount) { transmissionIndex ->
                    currentCoroutineContext().ensureActive()
                    val result = transmitter.transmit(signal)
                    if (result != TransmissionResult.Success) {
                        // The TV may have received an earlier repeat, so the next logical press
                        // must use the opposite RC5 toggle even when a later repeat fails locally.
                        if (transmissionSucceeded) advanceToggle(owner, irCommand)
                        return result
                    }
                    transmissionSucceeded = true
                    if (transmissionIndex < step.transmissionCount - 1 &&
                        step.transmissionGapMillis > 0L
                    ) {
                        pause(step.transmissionGapMillis)
                    }
                }
            } catch (error: CancellationException) {
                if (transmissionSucceeded) advanceToggle(owner, irCommand)
                throw error
            }
            advanceToggle(owner, irCommand)
            onProgress(
                SequenceProgress(
                    completedSteps = stepIndex + 1,
                    totalSteps = totalSteps,
                ),
            )
            currentCoroutineContext().ensureActive()
            if (step.delayAfterMillis > 0L) pause(step.delayAfterMillis)
        }
        return TransmissionResult.Success
    }

    private fun toggleFor(
        owner: ToggleOwner,
        command: com.erkanpulat.tvkumandam.domain.model.IrCommand,
    ): Boolean = if (command.usesToggleBit) nextToggleByOwner[owner] ?: false else false

    private fun advanceToggle(
        owner: ToggleOwner,
        command: com.erkanpulat.tvkumandam.domain.model.IrCommand,
    ) {
        if (command.usesToggleBit) {
            nextToggleByOwner[owner] = !(nextToggleByOwner[owner] ?: false)
        }
    }
}
