package com.erkanpulat.tvkumandam.domain.remote

import com.erkanpulat.tvkumandam.domain.model.IrCommand
import com.erkanpulat.tvkumandam.domain.model.IrSignal
import com.erkanpulat.tvkumandam.domain.model.CommandBinding
import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.InputCapability
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutSpec
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteSequence
import com.erkanpulat.tvkumandam.domain.model.RemoteSequenceStep
import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.data.remote.GrundigCompatibleProfile
import com.erkanpulat.tvkumandam.data.remote.protocol.NecAddressMode
import com.erkanpulat.tvkumandam.data.remote.protocol.NecIrCommand
import com.erkanpulat.tvkumandam.data.remote.protocol.Rc5IrCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteShortcut
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteControllerTest {
    private val TEST_OWNER_ID = "test-saved-remote"

    @Test
    fun `RC-YC1 HDMI1 sequence keeps Source seven Down OK and proven RC5 repeat policy`() {
        val expectedCommands = listOf(RemoteCommand.SOURCE) +
            List(7) { RemoteCommand.DOWN } +
            RemoteCommand.OK

        listOf(
            ArcelikOldLcdProfile.profile,
            GrundigCompatibleProfile.profile,
        ).forEach { profile ->
            val sequence = requireNotNull(profile.shortcutFor(RemoteShortcut.HDMI1))

            assertEquals(expectedCommands, sequence.steps.map { it.command })
            assertEquals(1_000L, sequence.steps.first().delayAfterMillis)
            assertEquals(
                listOf(300L, 300L, 300L, 300L, 300L, 300L, 500L),
                sequence.steps.drop(1).dropLast(1).map { it.delayAfterMillis },
            )
            assertEquals(0L, sequence.steps.last().delayAfterMillis)
            assertTrue(sequence.steps.all { it.transmissionCount == 2 })
            assertTrue(sequence.steps.all { it.transmissionGapMillis == 89L })
        }
    }

    @Test
    fun `signal rejects invalid carrier frequency`() {
        assertThrows(IllegalArgumentException::class.java) {
            IrSignal(carrierFrequencyHz = 0, patternMicros = intArrayOf(1))
        }
    }

    @Test
    fun `signal rejects an empty pattern`() {
        assertThrows(IllegalArgumentException::class.java) {
            IrSignal(carrierFrequencyHz = 36_000, patternMicros = intArrayOf())
        }
    }

    @Test
    fun `signal rejects a single timing value`() {
        assertThrows(IllegalArgumentException::class.java) {
            IrSignal(carrierFrequencyHz = 36_000, patternMicros = intArrayOf(889))
        }
    }

    @Test
    fun `signal rejects non-positive timing values`() {
        assertThrows(IllegalArgumentException::class.java) {
            IrSignal(carrierFrequencyHz = 36_000, patternMicros = intArrayOf(889, 0, 889))
        }
    }

    @Test
    fun `send reports unsupported device without encoding or transmitting`() {
        val command = RecordingCommand()
        val transmitter = RecordingTransmitter(isAvailable = false)
        val controller = RemoteController(transmitter)

        val result = controller.send(TEST_OWNER_ID, profileWith(RemoteCommand.POWER to command), RemoteCommand.POWER)

        assertEquals(TransmissionResult.UnsupportedDevice, result)
        assertTrue(command.requestedToggles.isEmpty())
        assertTrue(transmitter.signals.isEmpty())
    }

    @Test
    fun `send reports a command missing from the active profile`() {
        val controller = RemoteController(RecordingTransmitter(isAvailable = true))

        val result = controller.send(TEST_OWNER_ID, profileWith(), RemoteCommand.MUTE)

        assertEquals(TransmissionResult.CommandUnavailable, result)
    }

    @Test
    fun `successful sends forward exact signals and alternate the RC5 toggle`() {
        val signal = IrSignal(36_000, intArrayOf(889, 889, 1_778))
        val command = RecordingCommand(signal)
        val transmitter = RecordingTransmitter(isAvailable = true)
        val controller = RemoteController(transmitter)
        val profile = profileWith(RemoteCommand.POWER to command)

        assertEquals(TransmissionResult.Success, controller.send(TEST_OWNER_ID, profile, RemoteCommand.POWER))
        assertEquals(TransmissionResult.Success, controller.send(TEST_OWNER_ID, profile, RemoteCommand.POWER))

        assertEquals(listOf(false, true), command.requestedToggles)
        assertEquals(2, transmitter.signals.size)
        assertTrue(transmitter.signals.all { it == signal })
    }

    @Test
    fun `failed transmission does not advance the toggle`() {
        val command = RecordingCommand()
        val transmitter = RecordingTransmitter(
            isAvailable = true,
            result = TransmissionResult.PlatformFailure("test failure"),
        )
        val controller = RemoteController(transmitter)
        val profile = profileWith(RemoteCommand.POWER to command)

        controller.send(TEST_OWNER_ID, profile, RemoteCommand.POWER)
        controller.send(TEST_OWNER_ID, profile, RemoteCommand.POWER)

        assertEquals(listOf(false, false), command.requestedToggles)
        assertFalse(transmitter.signals.isEmpty())
    }

    @Test
    fun `non-toggle command between RC5 presses does not consume the profile toggle`() {
        val rc5Command = RecordingCommand(usesToggleBit = true)
        val nonToggleCommand = RecordingCommand(usesToggleBit = false)
        val controller = RemoteController(RecordingTransmitter(isAvailable = true))
        val profile = profileWith(
            RemoteCommand.POWER to rc5Command,
            RemoteCommand.MUTE to nonToggleCommand,
        )

        assertEquals(TransmissionResult.Success, controller.send(TEST_OWNER_ID, profile, RemoteCommand.POWER))
        assertEquals(TransmissionResult.Success, controller.send(TEST_OWNER_ID, profile, RemoteCommand.MUTE))
        assertEquals(TransmissionResult.Success, controller.send(TEST_OWNER_ID, profile, RemoteCommand.POWER))

        assertEquals(listOf(false, true), rc5Command.requestedToggles)
        assertEquals(listOf(false), nonToggleCommand.requestedToggles)
    }

    @Test
    fun `RC5 NEC RC5 consumes only RC5 toggle state`() {
        val rc5 = Rc5IrCommand(address = 0, command = 0x0C)
        val nec = NecIrCommand(
            address = 0x34,
            command = 0xA2,
            addressMode = NecAddressMode.STANDARD,
        )
        val transmitter = RecordingTransmitter(isAvailable = true)
        val controller = RemoteController(transmitter)
        val profile = profileWith(
            RemoteCommand.POWER to rc5,
            RemoteCommand.MUTE to nec,
        )

        controller.send(TEST_OWNER_ID, profile, RemoteCommand.POWER)
        controller.send(TEST_OWNER_ID, profile, RemoteCommand.MUTE)
        controller.send(TEST_OWNER_ID, profile, RemoteCommand.POWER)

        assertEquals(
            listOf(rc5.encode(false), nec.encode(false), rc5.encode(true)),
            transmitter.signals,
        )
    }

    @Test
    fun `RC5 toggle state is independent for each profile id`() {
        val firstProfileCommand = RecordingCommand(usesToggleBit = true)
        val secondProfileCommand = RecordingCommand(usesToggleBit = true)
        val controller = RemoteController(RecordingTransmitter(isAvailable = true))
        val firstProfile = profileWithId(
            "first-profile",
            RemoteCommand.POWER to firstProfileCommand,
        )
        val secondProfile = profileWithId(
            "second-profile",
            RemoteCommand.POWER to secondProfileCommand,
        )

        controller.send(TEST_OWNER_ID, firstProfile, RemoteCommand.POWER)
        controller.send(TEST_OWNER_ID, secondProfile, RemoteCommand.POWER)
        controller.send(TEST_OWNER_ID, firstProfile, RemoteCommand.POWER)
        controller.send(TEST_OWNER_ID, secondProfile, RemoteCommand.POWER)

        assertEquals(listOf(false, true), firstProfileCommand.requestedToggles)
        assertEquals(listOf(false, true), secondProfileCommand.requestedToggles)
    }

    @Test
    fun `two saved remotes sharing one profile keep independent logical toggle state`() {
        val command = RecordingCommand(usesToggleBit = true)
        val controller = RemoteController(RecordingTransmitter(isAvailable = true))
        val sharedProfile = profileWith(RemoteCommand.POWER to command)

        controller.send(ownerId = "salon-tv", profile = sharedProfile, command = RemoteCommand.POWER)
        controller.send(ownerId = "yatak-tv", profile = sharedProfile, command = RemoteCommand.POWER)
        controller.send(ownerId = "salon-tv", profile = sharedProfile, command = RemoteCommand.POWER)
        controller.send(ownerId = "yatak-tv", profile = sharedProfile, command = RemoteCommand.POWER)

        assertEquals(listOf(false, false, true, true), command.requestedToggles)
    }

    @Test
    fun `two RC5 profiles retain independent toggle states`() {
        val firstCommand = Rc5IrCommand(address = 0, command = 0x0C)
        val secondCommand = Rc5IrCommand(address = 1, command = 0x0D)
        val transmitter = RecordingTransmitter(isAvailable = true)
        val controller = RemoteController(transmitter)
        val firstProfile = profileWithId("first-profile", RemoteCommand.POWER to firstCommand)
        val secondProfile = profileWithId("second-profile", RemoteCommand.POWER to secondCommand)

        controller.send(TEST_OWNER_ID, firstProfile, RemoteCommand.POWER)
        controller.send(TEST_OWNER_ID, secondProfile, RemoteCommand.POWER)
        controller.send(TEST_OWNER_ID, firstProfile, RemoteCommand.POWER)
        controller.send(TEST_OWNER_ID, secondProfile, RemoteCommand.POWER)

        assertEquals(
            listOf(
                firstCommand.encode(false),
                secondCommand.encode(false),
                firstCommand.encode(true),
                secondCommand.encode(true),
            ),
            transmitter.signals,
        )
    }

    @Test
    fun `non-toggle sequence step does not advance the profile RC5 state`() = runTest {
        val rc5Command = RecordingCommand(usesToggleBit = true)
        val nonToggleCommand = RecordingCommand(usesToggleBit = false)
        val controller = RemoteController(
            transmitter = RecordingTransmitter(isAvailable = true),
            pause = {},
        )
        val profile = profileWith(
            RemoteCommand.POWER to rc5Command,
            RemoteCommand.MUTE to nonToggleCommand,
        )
        val sequence = RemoteSequence(
            listOf(RemoteSequenceStep(RemoteCommand.MUTE)),
        )

        controller.send(TEST_OWNER_ID, profile, RemoteCommand.POWER)
        controller.send(TEST_OWNER_ID, profile, sequence)
        controller.send(TEST_OWNER_ID, profile, RemoteCommand.POWER)

        assertEquals(listOf(false, true), rc5Command.requestedToggles)
        assertEquals(listOf(false), nonToggleCommand.requestedToggles)
    }

    @Test
    fun `mixed RC5 NEC RC5 sequence retains the profile RC5 state`() = runTest {
        val rc5 = Rc5IrCommand(address = 0, command = 0x0C)
        val nec = NecIrCommand(
            address = 0x34,
            command = 0xA2,
            addressMode = NecAddressMode.STANDARD,
        )
        val transmitter = RecordingTransmitter(isAvailable = true)
        val controller = RemoteController(transmitter = transmitter, pause = {})
        val profile = profileWith(
            RemoteCommand.POWER to rc5,
            RemoteCommand.MUTE to nec,
        )
        val sequence = RemoteSequence(
            listOf(
                RemoteSequenceStep(RemoteCommand.POWER),
                RemoteSequenceStep(RemoteCommand.MUTE),
                RemoteSequenceStep(RemoteCommand.POWER),
            ),
        )

        assertEquals(TransmissionResult.Success, controller.send(TEST_OWNER_ID, profile, sequence))

        assertEquals(
            listOf(rc5.encode(false), nec.encode(false), rc5.encode(true)),
            transmitter.signals,
        )
    }

    @Test
    fun `sequence sends commands in order and observes inter-command delays`() = runTest {
        val sourceSignal = IrSignal(36_000, intArrayOf(100, 100))
        val downSignal = IrSignal(36_000, intArrayOf(200, 200))
        val okSignal = IrSignal(36_000, intArrayOf(300, 300))
        val transmitter = RecordingTransmitter(isAvailable = true)
        val observedDelays = mutableListOf<Long>()
        val controller = RemoteController(
            transmitter = transmitter,
            pause = { observedDelays += it },
        )
        val profile = profileWith(
            RemoteCommand.SOURCE to RecordingCommand(sourceSignal),
            RemoteCommand.DOWN to RecordingCommand(downSignal),
            RemoteCommand.OK to RecordingCommand(okSignal),
        )
        val sequence = RemoteSequence(
            listOf(
                RemoteSequenceStep(RemoteCommand.SOURCE, delayAfterMillis = 800L),
                RemoteSequenceStep(RemoteCommand.DOWN, delayAfterMillis = 180L),
                RemoteSequenceStep(RemoteCommand.OK),
            ),
        )

        val result = controller.send(TEST_OWNER_ID, profile, sequence)

        assertEquals(TransmissionResult.Success, result)
        assertEquals(listOf(sourceSignal, downSignal, okSignal), transmitter.signals)
        assertEquals(listOf(800L, 180L), observedDelays)
    }

    @Test
    fun `sequence progress starts at zero and advances per completed logical step`() = runTest {
        val transmitter = RecordingTransmitter(isAvailable = true)
        val controller = RemoteController(transmitter, pause = {})
        val profile = profileWith(
            RemoteCommand.SOURCE to RecordingCommand(),
            RemoteCommand.OK to RecordingCommand(),
        )
        val sequence = RemoteSequence(
            listOf(
                RemoteSequenceStep(RemoteCommand.SOURCE, transmissionCount = 2),
                RemoteSequenceStep(RemoteCommand.OK, transmissionCount = 2),
            ),
        )
        val progress = mutableListOf<SequenceProgress>()

        val result = controller.send(TEST_OWNER_ID, profile, sequence, progress::add)

        assertEquals(TransmissionResult.Success, result)
        assertEquals(
            listOf(
                SequenceProgress(completedSteps = 0, totalSteps = 2),
                SequenceProgress(completedSteps = 1, totalSteps = 2),
                SequenceProgress(completedSteps = 2, totalSteps = 2),
            ),
            progress,
        )
    }

    @Test
    fun `cancelling during a sequence delay prevents the next logical step`() = runTest {
        val transmitter = RecordingTransmitter(isAvailable = true)
        val controller = RemoteController(transmitter)
        val profile = profileWith(
            RemoteCommand.SOURCE to RecordingCommand(),
            RemoteCommand.OK to RecordingCommand(),
        )
        val sequence = RemoteSequence(
            listOf(
                RemoteSequenceStep(RemoteCommand.SOURCE, delayAfterMillis = 1_000L),
                RemoteSequenceStep(RemoteCommand.OK),
            ),
        )
        val progress = mutableListOf<SequenceProgress>()

        val job = launch { controller.send(TEST_OWNER_ID, profile, sequence, progress::add) }
        runCurrent()

        assertEquals(1, transmitter.signals.size)
        assertEquals(SequenceProgress(1, 2), progress.last())

        job.cancel()
        runCurrent()
        advanceTimeBy(1_000L)
        runCurrent()

        assertTrue(job.isCancelled)
        assertEquals(1, transmitter.signals.size)
        assertEquals(SequenceProgress(1, 2), progress.last())
    }

    @Test
    fun `sequence repeats one logical press with the same RC5 toggle`() = runTest {
        val signal = IrSignal(36_000, intArrayOf(889, 889))
        val command = RecordingCommand(signal)
        val transmitter = RecordingTransmitter(isAvailable = true)
        val observedDelays = mutableListOf<Long>()
        val controller = RemoteController(
            transmitter = transmitter,
            pause = { observedDelays += it },
        )
        val profile = profileWith(RemoteCommand.DOWN to command)
        val sequence = RemoteSequence(
            listOf(
                RemoteSequenceStep(
                    command = RemoteCommand.DOWN,
                    transmissionCount = 2,
                    transmissionGapMillis = 89L,
                ),
            ),
        )

        val result = controller.send(TEST_OWNER_ID, profile, sequence)

        assertEquals(TransmissionResult.Success, result)
        assertEquals(listOf(false), command.requestedToggles)
        assertEquals(listOf(signal, signal), transmitter.signals)
        assertEquals(listOf(89L), observedDelays)
    }

    @Test
    fun `reliability frames share one toggle and the next logical step uses the next toggle`() =
        runTest {
            val signal = IrSignal(36_000, intArrayOf(889, 889))
            val command = RecordingCommand(signal)
            val transmitter = RecordingTransmitter(isAvailable = true)
            val controller = RemoteController(transmitter, pause = {})
            val profile = profileWith(RemoteCommand.DOWN to command)
            val sequence = RemoteSequence(
                listOf(
                    RemoteSequenceStep(RemoteCommand.DOWN, transmissionCount = 2),
                    RemoteSequenceStep(RemoteCommand.DOWN, transmissionCount = 2),
                ),
            )

            assertEquals(TransmissionResult.Success, controller.send(TEST_OWNER_ID, profile, sequence))

            assertEquals(listOf(false, true), command.requestedToggles)
            assertEquals(listOf(signal, signal, signal, signal), transmitter.signals)
        }

    @Test
    fun `cancelling between reliability frames stops the repeat and advances toggle conservatively`() =
        runTest {
            val command = RecordingCommand()
            val transmitter = RecordingTransmitter(isAvailable = true)
            val controller = RemoteController(transmitter)
            val profile = profileWith(RemoteCommand.DOWN to command)
            val repeatedPress = RemoteSequence(
                listOf(
                    RemoteSequenceStep(
                        command = RemoteCommand.DOWN,
                        transmissionCount = 2,
                        transmissionGapMillis = 1_000L,
                    ),
                ),
            )

            val job = launch { controller.send(TEST_OWNER_ID, profile, repeatedPress) }
            runCurrent()
            assertEquals(1, transmitter.signals.size)

            job.cancel()
            runCurrent()
            advanceTimeBy(1_000L)
            runCurrent()

            assertEquals(1, transmitter.signals.size)
            assertEquals(TransmissionResult.Success, controller.send(TEST_OWNER_ID, profile, RemoteCommand.DOWN))
            assertEquals(listOf(false, true), command.requestedToggles)
        }

    @Test
    fun `partial repeated press advances toggle after its first successful frame`() = runTest {
        val failure = TransmissionResult.PlatformFailure("second frame rejected")
        val command = RecordingCommand()
        val transmitter = RecordingTransmitter(
            isAvailable = true,
            results = ArrayDeque(
                listOf(
                    TransmissionResult.Success,
                    failure,
                    TransmissionResult.Success,
                ),
            ),
        )
        val controller = RemoteController(transmitter, pause = {})
        val profile = profileWith(RemoteCommand.DOWN to command)
        val repeatedPress = RemoteSequence(
            listOf(
                RemoteSequenceStep(
                    command = RemoteCommand.DOWN,
                    transmissionCount = 2,
                    transmissionGapMillis = 89L,
                ),
            ),
        )

        assertEquals(failure, controller.send(TEST_OWNER_ID, profile, repeatedPress))
        assertEquals(TransmissionResult.Success, controller.send(TEST_OWNER_ID, profile, RemoteCommand.DOWN))

        assertEquals(listOf(false, true), command.requestedToggles)
    }

    @Test
    fun `sequence stops after the first failed transmission`() = runTest {
        val failure = TransmissionResult.PlatformFailure("Emitter is busy")
        val transmitter = RecordingTransmitter(
            isAvailable = true,
            results = ArrayDeque(
                listOf(
                    TransmissionResult.Success,
                    failure,
                    TransmissionResult.Success,
                ),
            ),
        )
        val observedDelays = mutableListOf<Long>()
        val controller = RemoteController(
            transmitter = transmitter,
            pause = { observedDelays += it },
        )
        val profile = profileWith(
            RemoteCommand.SOURCE to RecordingCommand(),
            RemoteCommand.DOWN to RecordingCommand(),
            RemoteCommand.OK to RecordingCommand(),
        )
        val sequence = RemoteSequence(
            listOf(
                RemoteSequenceStep(RemoteCommand.SOURCE, delayAfterMillis = 800L),
                RemoteSequenceStep(RemoteCommand.DOWN, delayAfterMillis = 180L),
                RemoteSequenceStep(RemoteCommand.OK),
            ),
        )

        val result = controller.send(TEST_OWNER_ID, profile, sequence)

        assertEquals(failure, result)
        assertEquals(2, transmitter.signals.size)
        assertEquals(listOf(800L), observedDelays)
    }

    private fun profileWith(vararg commands: Pair<RemoteCommand, IrCommand>): RemoteProfile {
        return profileWithId("test", *commands)
    }

    private fun profileWithId(
        id: String,
        vararg commands: Pair<RemoteCommand, IrCommand>,
    ): RemoteProfile {
        val bindings = linkedMapOf(
            RemoteCommand.SOURCE to CommandBinding(DEFAULT_SOURCE_COMMAND, TEST_EVIDENCE),
        )
        commands.forEach { (command, irCommand) ->
            bindings[command] = CommandBinding(irCommand, TEST_EVIDENCE)
        }
        return RemoteProfile(
            id = id,
            brand = "Test Brand",
            displayName = "Test profile",
            modelAliases = listOf("Test Model"),
            remoteModel = null,
            defaultEvidence = TEST_EVIDENCE,
            commands = bindings,
            layout = RemoteLayoutSpec.defaultFor(RemoteLayoutTemplate.CLASSIC_DPAD),
            inputCapability = InputCapability.sourceOnly(),
        )
    }

    private class RecordingCommand(
        private val signal: IrSignal = IrSignal(36_000, intArrayOf(889, 889)),
        override val usesToggleBit: Boolean = true,
    ) : IrCommand {
        val requestedToggles = mutableListOf<Boolean>()

        override fun encode(toggle: Boolean): IrSignal {
            requestedToggles += toggle
            return signal
        }
    }

    private class RecordingTransmitter(
        override val isAvailable: Boolean,
        private val result: TransmissionResult = TransmissionResult.Success,
        private val results: ArrayDeque<TransmissionResult> = ArrayDeque(),
    ) : IrTransmitter {
        val signals = mutableListOf<IrSignal>()

        override fun transmit(signal: IrSignal): TransmissionResult {
            signals += signal
            return results.removeFirstOrNull() ?: result
        }
    }

    private companion object {
        val DEFAULT_SOURCE_COMMAND = IrCommand { IrSignal(36_000, intArrayOf(889, 889)) }
        val TEST_EVIDENCE = ProfileEvidence(
            tier = EvidenceTier.DEVICE_VERIFIED,
            sourceReference = "RemoteController test fixture",
        )
    }
}
