package com.erkanpulat.tvkumandam.data.preferences

import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.SavedMacroStep
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.preferences.ThemePreference

/** Catalog-aware, defensive normalization of decoded or caller-provided settings. */
internal class RemoteSettingsNormalizer(
    private val catalog: RemoteProfileCatalog,
) {
    fun normalize(settings: RemoteSettings): RemoteSettings {
        val seenIds = mutableSetOf<String>()
        val remotes = settings.savedRemotes.mapNotNull { remote ->
            val profile = catalog.findOrNull(remote.profileId) ?: return@mapNotNull null
            if (!seenIds.add(remote.id)) return@mapNotNull null
            val macros = remote.macros
                .filter { macro -> macro.steps.all { it.command in profile.supportedCommands } }
                .distinctBy(SavedMacro::id)
                .take(SavedRemote.MAX_MACROS)

            val validMacroIds = macros.mapTo(mutableSetOf(), SavedMacro::id)
            val quickActions = remote.quickActions
                .mapNotNull { action ->
                    when (action) {
                        is RemoteAction.Command -> action.takeIf { it.command != RemoteCommand.POWER && it in profile.supportedActions }
                        is RemoteAction.Macro -> action.takeIf { it.macroId in validMacroIds }
                        is RemoteAction.Shortcut -> null
                    }
                }
                .distinct()
                .take(SavedRemote.MAX_QUICK_ACTIONS)
            SavedRemote(
                id = remote.id,
                name = remote.name,
                profileId = remote.profileId,
                quickActions = quickActions,
                isConfirmed = remote.isConfirmed,
                macros = macros,
            )
        }
        return settings.copy(
            savedRemotes = remotes,
            selectedSavedRemoteId = settings.selectedSavedRemoteId,
        )
    }

    companion object {
        const val COMMAND_PREFIX = "C:"
        const val MACRO_PREFIX = "M:"
    }
}

/**
 * Hand-written first-release schema. String fields escape every non-safe UTF-16 code unit as
 * ^HHHH, so every
 * delimiter remains structural. The parser refuses incomplete, duplicate, oversized, malformed, and
 * future schemas.
 */
internal object RemoteSettingsCodec {
    private const val VERSION = "1"
    private const val NULL = "-"
    internal const val MAX_PAYLOAD_CHARS = 32 * 1024
    internal const val MAX_DEVICES = 64
    private const val FIELD_COUNT = 7

    fun encode(settings: RemoteSettings): String {
        require(settings.savedRemotes.size <= MAX_DEVICES) {
            "Settings cannot contain more than $MAX_DEVICES saved remotes."
        }
        return buildString {
            append(VERSION)
            append("|s=").append(encodeOptional(settings.selectedSavedRemoteId))
            append("|t=").append(settings.theme.name)
            append("|h=").append(if (settings.hapticsEnabled) '1' else '0')
            append("|r=").append(settings.handedness.name)
            append("|o=").append(if (settings.onboardingCompleted) '1' else '0')
            append("|d=")
            settings.savedRemotes.forEachIndexed { index, remote ->
                if (index > 0) append(';')
                append(encodeToken(remote.id)).append('~')
                append(encodeToken(remote.name)).append('~')
                append(encodeToken(remote.profileId)).append('~')
                append(if (remote.isConfirmed) '1' else '0').append('~')
                remote.quickActions.forEachIndexed { actionIndex, action ->
                    if (actionIndex > 0) append(',')
                    append(action.toToken())
                }
                append('~')
                remote.macros.forEachIndexed { macroIndex, macro ->
                    if (macroIndex > 0) append('!')
                    append(encodeToken(macro.id)).append('*')
                    append(encodeToken(macro.name)).append('*')
                    macro.steps.forEachIndexed { stepIndex, step ->
                        if (stepIndex > 0) append('+')
                        append(step.command.name).append('.')
                        append(step.repeatCount).append('.')
                        append(step.delayAfterMillis)
                    }
                }
            }
        }.also { payload ->
            require(payload.length <= MAX_PAYLOAD_CHARS) {
                "Encoded settings exceed the $MAX_PAYLOAD_CHARS character limit."
            }
        }
    }

    fun decode(payload: String, normalizer: RemoteSettingsNormalizer): RemoteSettings {
        if (payload.length > MAX_PAYLOAD_CHARS) return RemoteSettings()
        val fields = payload.split('|')
        if (fields.size != FIELD_COUNT || fields.firstOrNull() != VERSION) return RemoteSettings()
        val values = linkedMapOf<String, String>()
        fields.drop(1).forEach { field ->
            val equalsIndex = field.indexOf('=')
            if (equalsIndex <= 0 || values.put(field.substring(0, equalsIndex), field.substring(equalsIndex + 1)) != null) {
                return RemoteSettings()
            }
        }
        if (values.keys != setOf("s", "t", "h", "r", "o", "d")) return RemoteSettings()
        val theme = enumValueOrNull<ThemePreference>(values.getValue("t")) ?: return RemoteSettings()
        val handedness = enumValueOrNull<Handedness>(values.getValue("r")) ?: return RemoteSettings()
        val haptics = values.getValue("h").toBooleanFlag() ?: return RemoteSettings()
        val onboarding = values.getValue("o").toBooleanFlag() ?: return RemoteSettings()
        val selected = when (val encodedSelection = values.getValue("s")) {
            NULL -> null
            else -> decodeToken(encodedSelection) ?: return RemoteSettings()
        }
        val records = values.getValue("d").takeIf(String::isNotEmpty)?.split(';') ?: emptyList()
        if (records.size > MAX_DEVICES) return RemoteSettings()
        val remotes = records.map { record -> decodeRemote(record) ?: return RemoteSettings() }
        return normalizer.normalize(
            RemoteSettings(
                savedRemotes = remotes,
                selectedSavedRemoteId = selected,
                theme = theme,
                hapticsEnabled = haptics,
                handedness = handedness,
                onboardingCompleted = onboarding,
            ),
        )
    }

    private fun decodeRemote(record: String): SavedRemote? {
        val fields = record.split('~')
        if (fields.size != 6) return null
        val id = decodeToken(fields[0]) ?: return null
        val name = decodeToken(fields[1]) ?: return null
        val profileId = decodeToken(fields[2]) ?: return null
        val confirmed = fields[3].toBooleanFlag() ?: return null
        val actions = if (fields[4].isEmpty()) emptyList() else fields[4].split(',').mapNotNull { token ->
            when {
                token.startsWith(RemoteSettingsNormalizer.COMMAND_PREFIX) -> RemoteCommand.entries
                    .firstOrNull { it.name == token.removePrefix(RemoteSettingsNormalizer.COMMAND_PREFIX) }
                    ?.let(RemoteAction::Command)
                token.startsWith(RemoteSettingsNormalizer.MACRO_PREFIX) -> decodeToken(
                    token.removePrefix(RemoteSettingsNormalizer.MACRO_PREFIX),
                )?.let(RemoteAction::Macro)
                else -> null
            }
        }
            .filter { it != RemoteAction.Command(RemoteCommand.POWER) }
            .distinct()
            .take(SavedRemote.MAX_QUICK_ACTIONS)
        val macros = if (fields[5].isEmpty()) emptyList() else {
            fields[5].split('!').map { decodeMacro(it) ?: return null }
        }
        return runCatching {
            SavedRemote(id, name, profileId, actions, confirmed, macros)
        }.getOrNull()
    }

    private fun decodeMacro(value: String): SavedMacro? {
        val fields = value.split('*')
        if (fields.size != 3) return null
        val id = decodeToken(fields[0]) ?: return null
        val name = decodeToken(fields[1]) ?: return null
        val steps = fields[2].split('+').map { stepValue ->
            val stepFields = stepValue.split('.')
            if (stepFields.size != 3) return null
            val command = RemoteCommand.entries.firstOrNull { it.name == stepFields[0] } ?: return null
            val repeatCount = stepFields[1].toIntOrNull() ?: return null
            val delay = stepFields[2].toLongOrNull() ?: return null
            runCatching { SavedMacroStep(command, repeatCount, delay) }.getOrNull() ?: return null
        }
        return runCatching { SavedMacro(id, name, steps) }.getOrNull()
    }

    private fun RemoteAction.toToken(): String = when (this) {
        is RemoteAction.Command -> RemoteSettingsNormalizer.COMMAND_PREFIX + command.name
        is RemoteAction.Macro -> RemoteSettingsNormalizer.MACRO_PREFIX + encodeToken(macroId)
        is RemoteAction.Shortcut -> error("Profile shortcuts cannot be persisted as user actions.")
    }

    private fun encodeOptional(value: String?): String = value?.let(::encodeToken) ?: NULL

    private fun encodeToken(value: String): String = buildString {
        value.forEach { character ->
            if (character in SAFE_TOKEN_CHARS) append(character) else {
                append('^')
                append(character.code.toString(16).uppercase().padStart(4, '0'))
            }
        }
    }

    private fun decodeToken(value: String): String? = runCatching {
        if (value.isEmpty() || value == NULL) return null
        buildString {
            var index = 0
            while (index < value.length) {
                val character = value[index]
                if (character == '^') {
                    if (index + 4 >= value.length) return null
                    val escaped = value.substring(index + 1, index + 5)
                    if (escaped.any { it !in HEX_CHARS }) return null
                    append(escaped.toInt(16).toChar())
                    index += 5
                } else {
                    if (character !in SAFE_TOKEN_CHARS) return null
                    append(character)
                    index += 1
                }
            }
        }.takeIf(String::isNotBlank)
    }.getOrNull()

    private fun String.toBooleanFlag(): Boolean? = when (this) {
        "0" -> false
        "1" -> true
        else -> null
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value }

    private const val SAFE_TOKEN_CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._"
    private const val HEX_CHARS = "0123456789ABCDEF"
}
