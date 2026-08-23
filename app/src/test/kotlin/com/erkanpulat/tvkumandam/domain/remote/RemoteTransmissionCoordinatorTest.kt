package com.erkanpulat.tvkumandam.domain.remote

import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.domain.model.IrSignal
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteSequence
import com.erkanpulat.tvkumandam.domain.model.RemoteSequenceStep
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteTransmissionCoordinatorTest {
    @Test
    fun `a finder command waits behind an active sequence and cancellation releases the emitter`() = runTest {
        val sequencePaused = CompletableDeferred<Unit>()
        val releaseSequence = CompletableDeferred<Unit>()
        val transmitter = CountingTransmitter()
        val coordinator = RemoteTransmissionCoordinator(
            RemoteController(transmitter) {
                sequencePaused.complete(Unit)
                releaseSequence.await()
            },
        )
        val profile = ArcelikOldLcdProfile.profile
        val sequence = RemoteSequence(listOf(RemoteSequenceStep(RemoteCommand.POWER, delayAfterMillis = 1)))

        val remote = launch { coordinator.send("remote", profile, sequence) }
        sequencePaused.await()
        val finder = async { coordinator.send("finder", profile, RemoteCommand.POWER) }
        runCurrent()

        assertEquals(1, transmitter.transmissions)
        assertFalse(finder.isCompleted)

        remote.cancelAndJoin()
        assertEquals(TransmissionResult.Success, finder.await())
        assertEquals(2, transmitter.transmissions)
    }

    @Test
    fun `a cancelled waiter never reaches the physical transmitter`() = runTest {
        val sequencePaused = CompletableDeferred<Unit>()
        val releaseSequence = CompletableDeferred<Unit>()
        val transmitter = CountingTransmitter()
        val coordinator = RemoteTransmissionCoordinator(
            RemoteController(transmitter) {
                sequencePaused.complete(Unit)
                releaseSequence.await()
            },
        )
        val profile = ArcelikOldLcdProfile.profile
        val sequence = RemoteSequence(listOf(RemoteSequenceStep(RemoteCommand.POWER, delayAfterMillis = 1)))

        val remote = launch { coordinator.send("remote", profile, sequence) }
        sequencePaused.await()
        val finder = launch { coordinator.send("finder", profile, RemoteCommand.POWER) }
        runCurrent()
        finder.cancelAndJoin()
        releaseSequence.complete(Unit)
        remote.join()

        assertEquals(1, transmitter.transmissions)
    }

    private class CountingTransmitter : IrTransmitter {
        override val isAvailable: Boolean = true
        var transmissions = 0

        override fun transmit(signal: IrSignal): TransmissionResult {
            transmissions += 1
            return TransmissionResult.Success
        }
    }
}
