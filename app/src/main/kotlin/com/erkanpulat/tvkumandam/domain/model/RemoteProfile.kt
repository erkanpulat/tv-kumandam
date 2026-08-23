package com.erkanpulat.tvkumandam.domain.model

import java.util.Collections
import java.util.Locale

/** An exact remote family with immutable, evidence-bearing action bindings. */
class RemoteProfile(
    val id: String,
    val brand: String,
    val displayName: String,
    modelAliases: List<String>,
    val remoteModel: String?,
    compatibleBrands: List<String> = emptyList(),
    remoteAliases: List<String> = listOfNotNull(remoteModel),
    val defaultEvidence: ProfileEvidence,
    commands: Map<RemoteCommand, CommandBinding>,
    shortcuts: Map<RemoteShortcut, ShortcutBinding> = emptyMap(),
    val layout: RemoteLayoutSpec,
    val inputCapability: InputCapability,
) {
    val modelAliases: List<String> = Collections.unmodifiableList(modelAliases.toList())
    val compatibleBrands: List<String> = Collections.unmodifiableList(compatibleBrands.toList())
    val remoteAliases: List<String> = Collections.unmodifiableList(remoteAliases.toList())
    private val commandMap: Map<RemoteCommand, CommandBinding> =
        Collections.unmodifiableMap(LinkedHashMap(commands))
    private val shortcutMap: Map<RemoteShortcut, ShortcutBinding> =
        Collections.unmodifiableMap(LinkedHashMap(shortcuts))
    private val commandSet: Set<RemoteCommand> = immutableSet(commandMap.keys)
    private val shortcutSet: Set<RemoteShortcut> = immutableSet(shortcutMap.keys)
    private val actionSet: Set<RemoteAction> = immutableSet(
        commandMap.keys.mapTo(LinkedHashSet<RemoteAction>()) { RemoteAction.Command(it) }.apply {
            shortcutMap.keys.mapTo(this) { RemoteAction.Shortcut(it) }
        },
    )

    init {
        require(id.isNotBlank()) { "Profile id cannot be blank." }
        require(brand.isNotBlank()) { "Profile brand cannot be blank." }
        require(displayName.isNotBlank()) { "Profile display family cannot be blank." }
        require(this.modelAliases.isNotEmpty() || this.remoteAliases.isNotEmpty()) {
            "A profile requires model or remote discovery metadata."
        }
        require(this.modelAliases.all(String::isNotBlank)) { "Model aliases cannot be blank." }
        require(this.modelAliases.distinctNormalized().size == this.modelAliases.size) {
            "Model aliases cannot contain duplicates."
        }
        require(this.compatibleBrands.all(String::isNotBlank)) {
            "Compatible brands cannot be blank."
        }
        require(this.compatibleBrands.distinctNormalized().size == this.compatibleBrands.size) {
            "Compatible brands cannot contain duplicates."
        }
        require(this.compatibleBrands.none { it.equals(brand, ignoreCase = true) }) {
            "The exact brand cannot also be a compatible brand."
        }
        require(this.remoteAliases.all(String::isNotBlank)) { "Remote aliases cannot be blank." }
        require(this.remoteAliases.distinctNormalized().size == this.remoteAliases.size) {
            "Remote aliases cannot contain duplicates."
        }
        require(remoteModel == null || remoteModel.isNotBlank()) {
            "Remote model cannot be blank when present."
        }
        require(layout.defaultQuickActions.all { it in actionSet }) {
            "Default quick actions must be supported by the profile."
        }
        require(
            shortcutMap.values.all { binding ->
                binding.sequence.steps.all { step -> step.command in commandMap }
            },
        ) { "Shortcut steps must use commands supported by the profile." }
        require(inputCapability.discreteCommands.all { it in commandMap }) {
            "Discrete input metadata must reference supported commands."
        }
        require(inputCapability.sourceMenuShortcuts.all { it in shortcutMap }) {
            "Source-menu metadata must reference supported shortcuts."
        }
        when (inputCapability.strategy) {
            InputStrategy.NONE -> Unit
            InputStrategy.SOURCE_ONLY -> require(RemoteCommand.SOURCE in commandMap) {
                "Source-only input strategy requires a Source command."
            }
            InputStrategy.SOURCE_MENU_MACROS -> require(
                inputCapability.sourceMenuShortcuts.all { shortcut ->
                    shortcutMap.getValue(shortcut).sequence.steps.first().command ==
                        RemoteCommand.SOURCE
                },
            ) { "Source-menu input shortcuts must start with Source." }
            InputStrategy.DISCRETE_COMMANDS -> Unit
        }
    }

    val supportedCommands: Set<RemoteCommand>
        get() = commandSet

    val supportedShortcuts: Set<RemoteShortcut>
        get() = shortcutSet

    val supportedActions: Set<RemoteAction>
        get() = actionSet

    fun commandBindingFor(command: RemoteCommand): CommandBinding? = commandMap[command]

    fun shortcutBindingFor(shortcut: RemoteShortcut): ShortcutBinding? = shortcutMap[shortcut]

    fun commandFor(command: RemoteCommand): IrCommand? = commandBindingFor(command)?.irCommand

    fun shortcutFor(shortcut: RemoteShortcut): RemoteSequence? =
        shortcutBindingFor(shortcut)?.sequence

    fun evidenceFor(command: RemoteCommand): ProfileEvidence? =
        commandBindingFor(command)?.evidence

    fun evidenceFor(shortcut: RemoteShortcut): ProfileEvidence? =
        shortcutBindingFor(shortcut)?.evidence

    fun evidenceFor(action: RemoteAction): ProfileEvidence? = when (action) {
        is RemoteAction.Command -> evidenceFor(action.command)
        is RemoteAction.Macro -> null
        is RemoteAction.Shortcut -> evidenceFor(action.shortcut)
    }

    /** Compiles a user macro with repeat frames for toggle-bit protocols such as RC5. */
    fun sequenceFor(macro: SavedMacro): RemoteSequence? {
        if (macro.steps.any { it.command !in commandMap }) return null
        return RemoteSequence(
            macro.steps.flatMap { macroStep ->
                val usesToggleBit = commandMap.getValue(macroStep.command).irCommand.usesToggleBit
                List(macroStep.repeatCount) {
                    RemoteSequenceStep(
                        command = macroStep.command,
                        delayAfterMillis = macroStep.delayAfterMillis,
                        transmissionCount = if (usesToggleBit) RC5_RELIABILITY_FRAMES else 1,
                        transmissionGapMillis = if (usesToggleBit) RC5_FRAME_GAP_MILLIS else 0L,
                    )
                }
            },
        )
    }

    private fun <T> immutableSet(values: Collection<T>): Set<T> =
        Collections.unmodifiableSet(LinkedHashSet(values))

    private fun List<String>.distinctNormalized(): Set<String> =
        mapTo(LinkedHashSet()) { it.trim().lowercase(Locale.ROOT) }

    private companion object {
        const val RC5_RELIABILITY_FRAMES = 2
        const val RC5_FRAME_GAP_MILLIS = 89L
    }
}
