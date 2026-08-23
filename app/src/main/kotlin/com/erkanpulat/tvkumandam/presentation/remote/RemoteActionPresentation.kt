package com.erkanpulat.tvkumandam.presentation.remote

import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteSection
import com.erkanpulat.tvkumandam.domain.model.RemoteShortcut
import com.erkanpulat.tvkumandam.domain.model.SavedMacro

data class RemoteActionCopy(
    val label: String,
    val description: String,
)

/** Protocol-free Turkish presentation metadata shared by state tests and Compose. */
object RemoteActionPresentation {
    fun forAction(action: RemoteAction, macros: List<SavedMacro> = emptyList()): RemoteActionCopy = when (action) {
        is RemoteAction.Command -> forCommand(action.command)
        is RemoteAction.Macro -> macros.firstOrNull { it.id == action.macroId }?.let { macro ->
            copy(macro.name, "${macro.logicalPressCount} komutluk makroyu çalıştır")
        } ?: copy("Makro", "Kayıtlı makroyu çalıştır")
        is RemoteAction.Shortcut -> forShortcut(action.shortcut)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun forCommand(command: RemoteCommand): RemoteActionCopy = when (command) {
        RemoteCommand.POWER -> copy("Güç", "Televizyonu aç veya kapat")
        RemoteCommand.MUTE -> copy("Sessiz", "Sesi kapat veya aç")
        RemoteCommand.SOURCE -> copy("Kaynak", "Kaynak menüsünü aç")
        RemoteCommand.VOLUME_UP -> copy("Ses +", "Sesi artır")
        RemoteCommand.VOLUME_DOWN -> copy("Ses −", "Sesi azalt")
        RemoteCommand.CHANNEL_UP -> copy("Kanal +", "Sonraki kanala geç")
        RemoteCommand.CHANNEL_DOWN -> copy("Kanal −", "Önceki kanala geç")
        RemoteCommand.MENU -> copy("Menü", "Televizyon menüsünü aç")
        RemoteCommand.HOME -> copy("Ana ekran", "Televizyonun ana ekranını aç")
        RemoteCommand.BACK -> copy("Geri", "Önceki ekrana dön")
        RemoteCommand.EXIT -> copy("Çıkış", "Televizyon menüsünden çık")
        RemoteCommand.INFO -> copy("Bilgi", "Bilgi ekranını aç")
        RemoteCommand.GUIDE -> copy("Rehber", "Yayın rehberini aç")
        RemoteCommand.LAST_CHANNEL -> copy("Önceki", "Son izlenen kanala dön")
        RemoteCommand.PICTURE_FORMAT -> copy("Görüntü", "Görüntü biçimini değiştir")
        RemoteCommand.OK -> copy("OK", "Seçimi onayla")
        RemoteCommand.UP -> copy("Yukarı", "Yukarı git")
        RemoteCommand.DOWN -> copy("Aşağı", "Aşağı git")
        RemoteCommand.LEFT -> copy("Sol", "Sola git")
        RemoteCommand.RIGHT -> copy("Sağ", "Sağa git")
        RemoteCommand.DIGIT_0 -> digitCopy(0)
        RemoteCommand.DIGIT_1 -> digitCopy(1)
        RemoteCommand.DIGIT_2 -> digitCopy(2)
        RemoteCommand.DIGIT_3 -> digitCopy(3)
        RemoteCommand.DIGIT_4 -> digitCopy(4)
        RemoteCommand.DIGIT_5 -> digitCopy(5)
        RemoteCommand.DIGIT_6 -> digitCopy(6)
        RemoteCommand.DIGIT_7 -> digitCopy(7)
        RemoteCommand.DIGIT_8 -> digitCopy(8)
        RemoteCommand.DIGIT_9 -> digitCopy(9)
        RemoteCommand.RED -> copy("Kırmızı", "Kırmızı işlev tuşunu gönder")
        RemoteCommand.GREEN -> copy("Yeşil", "Yeşil işlev tuşunu gönder")
        RemoteCommand.YELLOW -> copy("Sarı", "Sarı işlev tuşunu gönder")
        RemoteCommand.BLUE -> copy("Mavi", "Mavi işlev tuşunu gönder")
        RemoteCommand.TELETEXT -> copy("Teletekst", "Teletekst ekranını aç")
        RemoteCommand.PLAY_PAUSE -> copy("Oynat / Duraklat", "Oynatmayı başlat veya duraklat")
        RemoteCommand.PLAY -> copy("Oynat", "Oynatmayı başlat")
        RemoteCommand.PAUSE -> copy("Duraklat", "Oynatmayı duraklat")
        RemoteCommand.STOP -> copy("Durdur", "Oynatmayı durdur")
        RemoteCommand.PREVIOUS -> copy("Önceki medya", "Önceki medya öğesine geç")
        RemoteCommand.NEXT -> copy("Sonraki medya", "Sonraki medya öğesine geç")
        RemoteCommand.REWIND -> copy("Geri sar", "Medyayı geri sar")
        RemoteCommand.FAST_FORWARD -> copy("İleri sar", "Medyayı ileri sar")
        RemoteCommand.HDMI1 -> copy("HDMI 1", "HDMI 1 girişine anında geç")
        RemoteCommand.HDMI2 -> copy("HDMI 2", "HDMI 2 girişine anında geç")
        RemoteCommand.HDMI3 -> copy("HDMI 3", "HDMI 3 girişine anında geç")
        RemoteCommand.HDMI4 -> copy("HDMI 4", "HDMI 4 girişine anında geç")
    }

    fun forShortcut(shortcut: RemoteShortcut): RemoteActionCopy = when (shortcut) {
        RemoteShortcut.HDMI1 -> copy(
            label = "HDMI 1",
            description = "HDMI 1'e geçiş makrosunu çalıştır",
        )
    }

    fun quickActions(profile: RemoteProfile, actions: List<RemoteAction>): List<RemoteAction> =
        actions.asSequence()
            .filter { it != RemoteAction.Command(RemoteCommand.POWER) }
            .filter { action -> action is RemoteAction.Macro || action in profile.supportedActions }
            .distinct()
            .take(4)
            .toList()

    fun visibleSections(profile: RemoteProfile): List<RemoteSection> =
        profile.layout.sections.filter { section -> sectionHasContent(profile, section) }

    fun supportedAdvancedCommands(profile: RemoteProfile): List<RemoteCommand> =
        ADVANCED_COMMANDS.filter { it in profile.supportedCommands }

    fun effectiveAdvancedCommands(profile: RemoteProfile): List<RemoteCommand> {
        val sectionOwnedCommands = buildSet {
            if (RemoteSection.NUMERIC_KEYPAD in profile.layout.sections) addAll(DIGITS)
            if (RemoteSection.COLOR_AND_TELETEXT in profile.layout.sections) addAll(COLOR_AND_TEXT)
            if (RemoteSection.MEDIA in profile.layout.sections) addAll(MEDIA)
            if (RemoteSection.DIRECT_INPUTS in profile.layout.sections) {
                addAll(verifiedDiscreteCommands(profile))
            }
        }
        return supportedAdvancedCommands(profile).filterNot { it in sectionOwnedCommands }
    }

    fun verifiedDiscreteCommands(profile: RemoteProfile): List<RemoteCommand> =
        profile.inputCapability.discreteCommands
            .filter { command -> profile.evidenceFor(command)?.tier != EvidenceTier.EXPERIMENTAL }
            .sortedBy(RemoteCommand::ordinal)

    private fun sectionHasContent(profile: RemoteProfile, section: RemoteSection): Boolean =
        when (section) {
            RemoteSection.QUICK_ACTIONS -> true
            RemoteSection.NAVIGATION -> NAVIGATION.any { it in profile.supportedCommands }
            RemoteSection.VOLUME_AND_CHANNEL -> ROCKERS.any { it in profile.supportedCommands }
            RemoteSection.PRIMARY_CONTROLS -> PRIMARY.any { it in profile.supportedCommands }
            RemoteSection.NUMERIC_KEYPAD -> DIGITS.any { it in profile.supportedCommands }
            RemoteSection.COLOR_AND_TELETEXT -> COLOR_AND_TEXT.any { it in profile.supportedCommands }
            RemoteSection.MEDIA -> MEDIA.any { it in profile.supportedCommands }
            RemoteSection.DIRECT_INPUTS -> verifiedDiscreteCommands(profile).isNotEmpty()
            RemoteSection.ADVANCED -> effectiveAdvancedCommands(profile).isNotEmpty()
        }

    private fun copy(label: String, description: String) = RemoteActionCopy(label, description)

    private fun digitCopy(digit: Int) = copy(digit.toString(), "$digit rakamını gönder")

    val NAVIGATION = listOf(
        RemoteCommand.UP,
        RemoteCommand.DOWN,
        RemoteCommand.LEFT,
        RemoteCommand.RIGHT,
        RemoteCommand.OK,
    )
    val ROCKERS = listOf(
        RemoteCommand.VOLUME_UP,
        RemoteCommand.VOLUME_DOWN,
        RemoteCommand.CHANNEL_UP,
        RemoteCommand.CHANNEL_DOWN,
    )
    val PRIMARY = listOf(
        RemoteCommand.BACK,
        RemoteCommand.MENU,
        RemoteCommand.HOME,
        RemoteCommand.GUIDE,
    )
    val DIGITS = listOf(
        RemoteCommand.DIGIT_1,
        RemoteCommand.DIGIT_2,
        RemoteCommand.DIGIT_3,
        RemoteCommand.DIGIT_4,
        RemoteCommand.DIGIT_5,
        RemoteCommand.DIGIT_6,
        RemoteCommand.DIGIT_7,
        RemoteCommand.DIGIT_8,
        RemoteCommand.DIGIT_9,
        RemoteCommand.DIGIT_0,
    )
    val COLOR_AND_TEXT = listOf(
        RemoteCommand.RED,
        RemoteCommand.GREEN,
        RemoteCommand.YELLOW,
        RemoteCommand.BLUE,
        RemoteCommand.TELETEXT,
    )
    val MEDIA = listOf(
        RemoteCommand.PREVIOUS,
        RemoteCommand.REWIND,
        RemoteCommand.PLAY_PAUSE,
        RemoteCommand.PLAY,
        RemoteCommand.PAUSE,
        RemoteCommand.STOP,
        RemoteCommand.FAST_FORWARD,
        RemoteCommand.NEXT,
    )
    private val ADVANCED_COMMANDS = listOf(
        RemoteCommand.EXIT,
        RemoteCommand.INFO,
        RemoteCommand.LAST_CHANNEL,
        RemoteCommand.PICTURE_FORMAT,
        *DIGITS.toTypedArray(),
        *COLOR_AND_TEXT.toTypedArray(),
        *MEDIA.toTypedArray(),
    )
}
