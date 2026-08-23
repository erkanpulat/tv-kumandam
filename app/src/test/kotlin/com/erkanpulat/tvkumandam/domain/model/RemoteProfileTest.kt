package com.erkanpulat.tvkumandam.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteProfileTest {

    @Test
    fun `command catalog contains only the approved safe TV operations`() {
        assertEquals(
            setOf(
                "POWER", "MUTE", "SOURCE", "VOLUME_UP", "VOLUME_DOWN",
                "CHANNEL_UP", "CHANNEL_DOWN", "MENU", "HOME", "BACK", "EXIT",
                "INFO", "GUIDE", "LAST_CHANNEL", "PICTURE_FORMAT", "OK", "UP",
                "DOWN", "LEFT", "RIGHT", "DIGIT_0", "DIGIT_1", "DIGIT_2",
                "DIGIT_3", "DIGIT_4", "DIGIT_5", "DIGIT_6", "DIGIT_7",
                "DIGIT_8", "DIGIT_9", "RED", "GREEN", "YELLOW", "BLUE",
                "TELETEXT", "PLAY_PAUSE", "PLAY", "PAUSE", "STOP", "PREVIOUS",
                "NEXT", "REWIND", "FAST_FORWARD", "HDMI1", "HDMI2", "HDMI3",
                "HDMI4",
            ),
            RemoteCommand.entries.map { it.name }.toSet(),
        )
    }

    @Test
    fun `profile rejects a blank stable id`() {
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(id = " ")
        }
    }

    @Test
    fun `profile rejects a blank brand`() {
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(brand = " ")
        }
    }

    @Test
    fun `profile rejects a blank display family`() {
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(displayName = " ")
        }
    }

    @Test
    fun `profile requires model or remote discovery metadata and rejects blank aliases`() {
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(
                modelAliases = emptyList(),
                remoteModel = null,
                remoteAliases = emptyList(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(modelAliases = listOf("Model A", " "))
        }
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(remoteAliases = listOf("RM-1", " "))
        }
    }

    @Test
    fun `profile rejects a present but blank remote model`() {
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(remoteModel = " ")
        }
    }

    @Test
    fun `evidence rejects a blank source reference`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProfileEvidence(EvidenceTier.EXPERIMENTAL, " ")
        }
    }

    @Test
    fun `profile rejects an unsupported command in default quick actions`() {
        val layout = classicLayout(
            listOf(RemoteAction.Command(RemoteCommand.MUTE)),
        )

        assertThrows(IllegalArgumentException::class.java) {
            validProfile(layout = layout)
        }
    }

    @Test
    fun `profile rejects an unsupported shortcut in default quick actions`() {
        val layout = classicLayout(
            listOf(RemoteAction.Shortcut(RemoteShortcut.HDMI1)),
        )

        assertThrows(IllegalArgumentException::class.java) {
            validProfile(layout = layout)
        }
    }

    @Test
    fun `layout rejects Power in the reorderable default quick deck`() {
        assertThrows(IllegalArgumentException::class.java) {
            classicLayout(listOf(RemoteAction.Command(RemoteCommand.POWER)))
        }
    }

    @Test
    fun `layout preserves section and quick-action insertion order without leaking mutation`() {
        val sections = mutableListOf(RemoteSection.QUICK_ACTIONS, RemoteSection.NAVIGATION)
        val actions = mutableListOf(
            RemoteAction.Command(RemoteCommand.SOURCE),
            RemoteAction.Shortcut(RemoteShortcut.HDMI1),
        )
        val layout = RemoteLayoutSpec(
            template = RemoteLayoutTemplate.CLASSIC_DPAD,
            sections = sections,
            defaultQuickActions = actions,
        )

        sections.reverse()
        actions.clear()

        assertEquals(
            listOf(RemoteSection.QUICK_ACTIONS, RemoteSection.NAVIGATION),
            layout.sections,
        )
        assertEquals(
            listOf(
                RemoteAction.Command(RemoteCommand.SOURCE),
                RemoteAction.Shortcut(RemoteShortcut.HDMI1),
            ),
            layout.defaultQuickActions,
        )
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (layout.sections as MutableList<RemoteSection>).add(RemoteSection.ADVANCED)
        }
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (layout.defaultQuickActions as MutableList<RemoteAction>)
                .add(RemoteAction.Command(RemoteCommand.MENU))
        }
    }

    @Test
    fun `layout rejects duplicate sections and duplicate default quick actions`() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteLayoutSpec(
                template = RemoteLayoutTemplate.CLASSIC_DPAD,
                sections = listOf(RemoteSection.NAVIGATION, RemoteSection.NAVIGATION),
                defaultQuickActions = emptyList(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            classicLayout(
                listOf(
                    RemoteAction.Command(RemoteCommand.SOURCE),
                    RemoteAction.Command(RemoteCommand.SOURCE),
                ),
            )
        }
    }

    @Test
    fun `layout rejects no sections and more than four default quick actions`() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteLayoutSpec(
                template = RemoteLayoutTemplate.CLASSIC_DPAD,
                sections = emptyList(),
                defaultQuickActions = emptyList(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            RemoteLayoutSpec(
                template = RemoteLayoutTemplate.CLASSIC_DPAD,
                sections = listOf(RemoteSection.QUICK_ACTIONS),
                defaultQuickActions = listOf(
                    RemoteAction.Command(RemoteCommand.SOURCE),
                    RemoteAction.Command(RemoteCommand.MENU),
                    RemoteAction.Command(RemoteCommand.MUTE),
                    RemoteAction.Command(RemoteCommand.INFO),
                    RemoteAction.Command(RemoteCommand.GUIDE),
                ),
            )
        }
    }

    @Test
    fun `Classic and Legacy templates use materially different section order`() {
        val classic = RemoteLayoutSpec.defaultFor(RemoteLayoutTemplate.CLASSIC_DPAD)
        val fullRemote = RemoteLayoutSpec.defaultFor(RemoteLayoutTemplate.FULL_REMOTE)

        assertEquals(
            listOf(
                RemoteSection.QUICK_ACTIONS,
                RemoteSection.NAVIGATION,
                RemoteSection.VOLUME_AND_CHANNEL,
                RemoteSection.PRIMARY_CONTROLS,
                RemoteSection.ADVANCED,
            ),
            classic.sections,
        )
        assertEquals(
            listOf(
                RemoteSection.QUICK_ACTIONS,
                RemoteSection.NUMERIC_KEYPAD,
                RemoteSection.NAVIGATION,
                RemoteSection.VOLUME_AND_CHANNEL,
                RemoteSection.PRIMARY_CONTROLS,
                RemoteSection.COLOR_AND_TELETEXT,
                RemoteSection.MEDIA,
            ),
            fullRemote.sections,
        )
        assertNotEquals(classic.sections, fullRemote.sections)
    }

    @Test
    fun `command evidence can remain experimental under a device-verified profile`() {
        val experimental = ProfileEvidence(
            tier = EvidenceTier.EXPERIMENTAL,
            sourceReference = "Candidate RC5 table row 42",
        )
        val profile = validProfile(
            commands = linkedMapOf(
                RemoteCommand.POWER to binding(DEVICE_VERIFIED),
                RemoteCommand.SOURCE to binding(DEVICE_VERIFIED),
                RemoteCommand.EXIT to binding(experimental),
            ),
        )

        assertEquals(EvidenceTier.DEVICE_VERIFIED, profile.defaultEvidence.tier)
        assertEquals(experimental, profile.evidenceFor(RemoteAction.Command(RemoteCommand.EXIT)))
        assertEquals(DEVICE_VERIFIED, profile.evidenceFor(RemoteCommand.POWER))
        assertNull(profile.evidenceFor(RemoteCommand.MUTE))
    }

    @Test
    fun `discrete HDMI1 and HDMI1 source-menu macro remain different action strategies`() {
        val discrete = validProfile(
            commands = linkedMapOf(
                RemoteCommand.POWER to binding(DEVICE_VERIFIED),
                RemoteCommand.SOURCE to binding(DEVICE_VERIFIED),
                RemoteCommand.HDMI1 to binding(SOURCE_VERIFIED),
            ),
            inputCapability = InputCapability.discreteCommands(setOf(RemoteCommand.HDMI1)),
        )
        val macroSequence = RemoteSequence(
            listOf(
                RemoteSequenceStep(RemoteCommand.SOURCE, 1_000L),
                RemoteSequenceStep(RemoteCommand.DOWN, 500L),
                RemoteSequenceStep(RemoteCommand.OK),
            ),
        )
        val macro = validProfile(
            commands = linkedMapOf(
                RemoteCommand.POWER to binding(DEVICE_VERIFIED),
                RemoteCommand.SOURCE to binding(DEVICE_VERIFIED),
                RemoteCommand.DOWN to binding(DEVICE_VERIFIED),
                RemoteCommand.OK to binding(DEVICE_VERIFIED),
            ),
            shortcuts = linkedMapOf(
                RemoteShortcut.HDMI1 to ShortcutBinding(macroSequence, DEVICE_VERIFIED),
            ),
            layout = classicLayout(listOf(RemoteAction.Shortcut(RemoteShortcut.HDMI1))),
            inputCapability = InputCapability.sourceMenuMacros(setOf(RemoteShortcut.HDMI1)),
        )

        assertTrue(discrete.commandFor(RemoteCommand.HDMI1) === TEST_IR_COMMAND)
        assertNull(discrete.shortcutFor(RemoteShortcut.HDMI1))
        assertEquals(InputStrategy.DISCRETE_COMMANDS, discrete.inputCapability.strategy)
        assertEquals(setOf(RemoteCommand.HDMI1), discrete.inputCapability.discreteCommands)

        assertNull(macro.commandFor(RemoteCommand.HDMI1))
        assertEquals(
            listOf(RemoteCommand.SOURCE, RemoteCommand.DOWN, RemoteCommand.OK),
            macro.shortcutFor(RemoteShortcut.HDMI1)?.steps?.map { it.command },
        )
        assertEquals(InputStrategy.SOURCE_MENU_MACROS, macro.inputCapability.strategy)
        assertEquals(setOf(RemoteShortcut.HDMI1), macro.inputCapability.sourceMenuShortcuts)
        assertEquals(DEVICE_VERIFIED, macro.evidenceFor(RemoteShortcut.HDMI1))
        assertTrue(RemoteAction.Command(RemoteCommand.SOURCE) in macro.supportedActions)
        assertTrue(RemoteAction.Shortcut(RemoteShortcut.HDMI1) in macro.supportedActions)
        assertNotEquals(
            RemoteAction.Command(RemoteCommand.HDMI1),
            RemoteAction.Shortcut(RemoteShortcut.HDMI1),
        )
    }

    @Test
    fun `source-only input metadata carries no discrete command or macro`() {
        val profile = validProfile()

        assertEquals(InputStrategy.SOURCE_ONLY, profile.inputCapability.strategy)
        assertEquals(emptySet<RemoteCommand>(), profile.inputCapability.discreteCommands)
        assertEquals(emptySet<RemoteShortcut>(), profile.inputCapability.sourceMenuShortcuts)
    }

    @Test
    fun `no-input metadata carries no Source requirement`() {
        val profile = validProfile(
            commands = linkedMapOf(RemoteCommand.POWER to binding(DEVICE_VERIFIED)),
            layout = classicLayout(emptyList()),
            inputCapability = InputCapability.none(),
        )

        assertEquals(InputStrategy.NONE, profile.inputCapability.strategy)
        assertEquals(emptySet<RemoteCommand>(), profile.inputCapability.discreteCommands)
        assertEquals(emptySet<RemoteShortcut>(), profile.inputCapability.sourceMenuShortcuts)
    }

    @Test
    fun `input metadata rejects empty or non-discrete strategy contents`() {
        assertThrows(IllegalArgumentException::class.java) {
            InputCapability.discreteCommands(emptySet())
        }
        assertThrows(IllegalArgumentException::class.java) {
            InputCapability.discreteCommands(setOf(RemoteCommand.SOURCE))
        }
        assertThrows(IllegalArgumentException::class.java) {
            InputCapability.sourceMenuMacros(emptySet())
        }
    }

    @Test
    fun `profile rejects shortcut steps and input metadata without matching bindings`() {
        val unsupportedSequence = RemoteSequence(
            listOf(RemoteSequenceStep(RemoteCommand.MUTE)),
        )
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(
                shortcuts = mapOf(
                    RemoteShortcut.HDMI1 to ShortcutBinding(
                        unsupportedSequence,
                        DEVICE_VERIFIED,
                    ),
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(
                inputCapability = InputCapability.discreteCommands(setOf(RemoteCommand.HDMI1)),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(
                inputCapability = InputCapability.sourceMenuMacros(setOf(RemoteShortcut.HDMI1)),
            )
        }
    }

    @Test
    fun `source-only input strategy requires a Source command binding`() {
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(
                commands = linkedMapOf(
                    RemoteCommand.POWER to binding(DEVICE_VERIFIED),
                ),
                layout = classicLayout(emptyList()),
                inputCapability = InputCapability.sourceOnly(),
            )
        }
    }

    @Test
    fun `source-menu input strategy rejects a macro not starting with Source`() {
        val wrongSequence = RemoteSequence(
            listOf(
                RemoteSequenceStep(RemoteCommand.DOWN, 300L),
                RemoteSequenceStep(RemoteCommand.SOURCE, 1_000L),
                RemoteSequenceStep(RemoteCommand.OK),
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            validProfile(
                commands = linkedMapOf(
                    RemoteCommand.POWER to binding(DEVICE_VERIFIED),
                    RemoteCommand.SOURCE to binding(DEVICE_VERIFIED),
                    RemoteCommand.DOWN to binding(DEVICE_VERIFIED),
                    RemoteCommand.OK to binding(DEVICE_VERIFIED),
                ),
                shortcuts = linkedMapOf(
                    RemoteShortcut.HDMI1 to ShortcutBinding(wrongSequence, DEVICE_VERIFIED),
                ),
                layout = classicLayout(listOf(RemoteAction.Shortcut(RemoteShortcut.HDMI1))),
                inputCapability = InputCapability.sourceMenuMacros(setOf(RemoteShortcut.HDMI1)),
            )
        }
    }

    @Test
    fun `sequence steps preserve insertion order without leaking mutation`() {
        val mutableSteps = mutableListOf(
            RemoteSequenceStep(RemoteCommand.SOURCE, 1_000L),
            RemoteSequenceStep(RemoteCommand.OK),
        )
        val sequence = RemoteSequence(mutableSteps)

        mutableSteps.clear()

        assertEquals(
            listOf(RemoteCommand.SOURCE, RemoteCommand.OK),
            sequence.steps.map { it.command },
        )
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (sequence.steps as MutableList<RemoteSequenceStep>)
                .add(RemoteSequenceStep(RemoteCommand.DOWN))
        }
    }

    @Test
    fun `profile collections preserve insertion order and cannot be mutated by callers`() {
        val aliases = mutableListOf("Model A", "Model A Alt")
        val compatibleBrands = mutableListOf("Compatible Brand")
        val remoteAliases = mutableListOf("RM-1", "RM-1 Alt")
        val commands = linkedMapOf(
            RemoteCommand.POWER to binding(DEVICE_VERIFIED),
            RemoteCommand.SOURCE to binding(DEVICE_VERIFIED),
        )
        val profile = validProfile(
            modelAliases = aliases,
            compatibleBrands = compatibleBrands,
            remoteAliases = remoteAliases,
            commands = commands,
        )

        aliases.clear()
        compatibleBrands.clear()
        remoteAliases.clear()
        commands.clear()

        assertEquals(listOf("Model A", "Model A Alt"), profile.modelAliases)
        assertEquals(listOf("Compatible Brand"), profile.compatibleBrands)
        assertEquals(listOf("RM-1", "RM-1 Alt"), profile.remoteAliases)
        assertEquals(
            listOf(RemoteCommand.POWER, RemoteCommand.SOURCE),
            profile.supportedCommands.toList(),
        )
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (profile.modelAliases as MutableList<String>).add("Injected")
        }
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (profile.remoteAliases as MutableList<String>).add("Injected")
        }
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (profile.supportedCommands as MutableSet<RemoteCommand>).add(RemoteCommand.MUTE)
        }
    }

    private fun validProfile(
        id: String = "test-family",
        brand: String = "Test Brand",
        displayName: String = "Test Family",
        modelAliases: List<String> = listOf("Model A"),
        remoteModel: String? = "RM-1",
        compatibleBrands: List<String> = emptyList(),
        remoteAliases: List<String> = listOfNotNull(remoteModel),
        commands: Map<RemoteCommand, CommandBinding> = linkedMapOf(
            RemoteCommand.POWER to binding(DEVICE_VERIFIED),
            RemoteCommand.SOURCE to binding(DEVICE_VERIFIED),
        ),
        shortcuts: Map<RemoteShortcut, ShortcutBinding> = emptyMap(),
        layout: RemoteLayoutSpec = classicLayout(
            listOf(RemoteAction.Command(RemoteCommand.SOURCE)),
        ),
        inputCapability: InputCapability = InputCapability.sourceOnly(),
    ) = RemoteProfile(
        id = id,
        brand = brand,
        displayName = displayName,
        modelAliases = modelAliases,
        remoteModel = remoteModel,
        compatibleBrands = compatibleBrands,
        remoteAliases = remoteAliases,
        defaultEvidence = DEVICE_VERIFIED,
        commands = commands,
        shortcuts = shortcuts,
        layout = layout,
        inputCapability = inputCapability,
    )

    private fun classicLayout(defaults: List<RemoteAction>) = RemoteLayoutSpec.defaultFor(
        template = RemoteLayoutTemplate.CLASSIC_DPAD,
        defaultQuickActions = defaults,
    )

    private fun binding(evidence: ProfileEvidence) = CommandBinding(TEST_IR_COMMAND, evidence)

    private companion object {
        val TEST_IR_COMMAND = IrCommand { IrSignal(36_000, intArrayOf(889, 889)) }
        val DEVICE_VERIFIED = ProfileEvidence(
            tier = EvidenceTier.DEVICE_VERIFIED,
            sourceReference = "Physical test unit A",
        )
        val SOURCE_VERIFIED = ProfileEvidence(
            tier = EvidenceTier.SOURCE_VERIFIED,
            sourceReference = "Manufacturer manual page 12",
        )
    }
}
