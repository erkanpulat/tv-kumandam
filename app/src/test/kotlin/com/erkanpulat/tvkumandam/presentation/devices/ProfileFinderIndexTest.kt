package com.erkanpulat.tvkumandam.presentation.devices

import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.data.remote.BekoCompatibleProfile
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.data.remote.protocol.Rc5IrCommand
import com.erkanpulat.tvkumandam.domain.model.CommandBinding
import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.InputCapability
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutSpec
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFinderIndexTest {
    @Test
    fun `brands models and unknown-model candidates are unique and deterministic`() {
        val index = ProfileFinderIndex(RemoteProfileCatalog())

        assertEquals(
            listOf("Arçelik", "Beko", "Grundig", "LG", "Samsung", "Toshiba", "Hitachi", "JVC"),
            index.brands,
        )
        assertEquals(listOf("82-507 B", "32HDR"), index.modelsFor("Arçelik"))
        assertEquals(
            listOf(ArcelikOldLcdProfile.ID),
            index.candidates("Arçelik", modelAlias = null).map(RemoteProfile::id),
        )
        assertEquals(
            listOf(BekoCompatibleProfile.ID),
            index.candidates("Beko", "F 82-507 B 3HD").map(RemoteProfile::id),
        )
        assertEquals(
            emptyList<String>(),
            index.candidates("Arçelik", "RC-YC1").map(RemoteProfile::id),
        )
        assertTrue(index.candidates("Arçelik", "Missing model").isEmpty())
        assertTrue(index.candidates("Missing", null).isEmpty())
    }

    @Test
    fun `profiles without Power are not offered and duplicate profile instances collapse`() {
        val noPower = sourceOnlyProfile()
        val catalog = RemoteProfileCatalog(
            listOf(noPower, ArcelikOldLcdProfile.profile, ArcelikOldLcdProfile.profile.copyWithId("copy")),
        )

        val index = ProfileFinderIndex(catalog)

        assertEquals(listOf("Arçelik"), index.brands)
        assertEquals(
            listOf(ArcelikOldLcdProfile.ID, "copy"),
            index.candidates("Arçelik", null).map { it.id },
        )
    }

    private fun sourceOnlyProfile(): RemoteProfile {
        val evidence = ProfileEvidence(EvidenceTier.EXPERIMENTAL, "Fixture")
        return RemoteProfile(
            id = "no-power",
            brand = "Hidden",
            displayName = "Hidden fixture",
            modelAliases = listOf("No Power"),
            remoteModel = null,
            defaultEvidence = evidence,
            commands = mapOf(
                RemoteCommand.SOURCE to CommandBinding(Rc5IrCommand(0, 1), evidence),
            ),
            layout = RemoteLayoutSpec.defaultFor(RemoteLayoutTemplate.CLASSIC_DPAD),
            inputCapability = InputCapability.sourceOnly(),
        )
    }

    private fun RemoteProfile.copyWithId(id: String): RemoteProfile {
        val commands = supportedCommands.associateWith { command -> requireNotNull(commandBindingFor(command)) }
        val shortcuts = supportedShortcuts.associateWith { shortcut -> requireNotNull(shortcutBindingFor(shortcut)) }
        return RemoteProfile(
            id = id,
            brand = brand,
            displayName = displayName,
            modelAliases = modelAliases,
            remoteModel = remoteModel,
            compatibleBrands = compatibleBrands,
            remoteAliases = remoteAliases,
            defaultEvidence = defaultEvidence,
            commands = commands,
            shortcuts = shortcuts,
            layout = layout,
            inputCapability = inputCapability,
        )
    }
}
