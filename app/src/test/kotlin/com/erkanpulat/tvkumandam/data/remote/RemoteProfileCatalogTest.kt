package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.data.remote.profiles.HitachiCle1031Profile
import com.erkanpulat.tvkumandam.data.remote.profiles.JvcLt49Hw97URmC3311Profile
import com.erkanpulat.tvkumandam.data.remote.profiles.Lg32Lc50CbProfile
import com.erkanpulat.tvkumandam.data.remote.profiles.Lg50Pc1Dr42Lb1DrProfile
import com.erkanpulat.tvkumandam.data.remote.profiles.Lg70Profile
import com.erkanpulat.tvkumandam.data.remote.profiles.LgLc2DPc3DProfile
import com.erkanpulat.tvkumandam.data.remote.profiles.SamsungAa5900484AProfile
import com.erkanpulat.tvkumandam.data.remote.profiles.ToshibaCt8560Profile
import com.erkanpulat.tvkumandam.data.remote.protocol.NecAddressMode
import com.erkanpulat.tvkumandam.data.remote.protocol.NecIrCommand
import com.erkanpulat.tvkumandam.data.remote.protocol.Rc5IrCommand
import com.erkanpulat.tvkumandam.data.remote.protocol.Samsung32IrCommand
import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.InputStrategy
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteProfileCatalogTest {

    private val catalog = RemoteProfileCatalog()

    @Test
    fun `catalog exposes only the curated profiles with unique stable ids`() {
        assertEquals(11, catalog.profiles.size)
        assertEquals(11, catalog.profiles.map { it.id }.toSet().size)
        assertEquals(
            setOf(
                "arcelik-old-lcd",
                "beko-compatible",
                "grundig-compatible",
                "lg-50pc1dr-42lb1dr",
                "lg-32lc50cb",
                "lg-lc2d-pc3d",
                "lg-lg70",
                "samsung-aa59-00484a",
                "toshiba-ct-8560",
                "hitachi-cle-1031",
                "jvc-lt-49hw97u-rm-c3311",
            ),
            catalog.profiles.map { it.id }.toSet(),
        )
    }

    @Test
    fun `bundled profiles identify exact families evidence layouts and input strategies`() {
        val arcelik = ArcelikOldLcdProfile.profile
        assertEquals("Arçelik", arcelik.brand)
        assertEquals(listOf("82-507 B", "32HDR"), arcelik.modelAliases)
        assertEquals("RC-YC1", arcelik.remoteModel)
        assertEquals(EvidenceTier.DEVICE_VERIFIED, arcelik.defaultEvidence.tier)
        assertEquals(RemoteLayoutTemplate.CLASSIC_DPAD, arcelik.layout.template)
        assertEquals(InputStrategy.SOURCE_ONLY, arcelik.inputCapability.strategy)
        assertEquals(
            listOf(RemoteAction.Command(RemoteCommand.SOURCE)),
            arcelik.layout.defaultQuickActions,
        )

        val beko = BekoCompatibleProfile.profile
        assertEquals("Beko", beko.brand)
        assertEquals("Beko F 82-507 B 3HD (RC-YC1)", beko.displayName)
        assertEquals(listOf("F 82-507 B 3HD"), beko.modelAliases)
        assertEquals("RC-YC1", beko.remoteModel)
        assertEquals(EvidenceTier.SOURCE_VERIFIED, beko.defaultEvidence.tier)
        assertEquals(RemoteLayoutTemplate.CLASSIC_DPAD, beko.layout.template)
        assertEquals(InputStrategy.SOURCE_ONLY, beko.inputCapability.strategy)
        assertEquals(
            listOf(RemoteAction.Command(RemoteCommand.SOURCE)),
            beko.layout.defaultQuickActions,
        )

        val grundig = GrundigCompatibleProfile.profile
        assertEquals("Grundig", grundig.brand)
        assertEquals(listOf("1786 XM", "1 3018"), grundig.modelAliases)
        assertEquals("RC-YC1", grundig.remoteModel)
        assertEquals(EvidenceTier.SOURCE_VERIFIED, grundig.defaultEvidence.tier)
        assertEquals(RemoteLayoutTemplate.CLASSIC_DPAD, grundig.layout.template)
        assertEquals(InputStrategy.SOURCE_ONLY, grundig.inputCapability.strategy)
    }

    @Test
    fun `every RC-YC1 profile keeps the sourced core commands`() {
        val expectedCommands = setOf(
            RemoteCommand.POWER,
            RemoteCommand.SOURCE,
            RemoteCommand.VOLUME_UP,
            RemoteCommand.VOLUME_DOWN,
            RemoteCommand.CHANNEL_UP,
            RemoteCommand.CHANNEL_DOWN,
            RemoteCommand.MENU,
            RemoteCommand.OK,
            RemoteCommand.UP,
            RemoteCommand.DOWN,
            RemoteCommand.LEFT,
            RemoteCommand.RIGHT,
        )

        listOf(
            ArcelikOldLcdProfile.profile,
            BekoCompatibleProfile.profile,
            GrundigCompatibleProfile.profile,
        ).forEach { profile ->
            assertEquals(expectedCommands, profile.supportedCommands)
        }
    }

    @Test
    fun `catalog rejects duplicate profile ids`() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteProfileCatalog(
                listOf(ArcelikOldLcdProfile.profile, ArcelikOldLcdProfile.profile),
            )
        }
    }

    @Test
    fun `injected catalog falls back to its first profile deterministically`() {
        val injected = RemoteProfileCatalog(
            listOf(BekoCompatibleProfile.profile, GrundigCompatibleProfile.profile),
        )

        assertEquals(BekoCompatibleProfile.profile, injected.find("unknown"))
        assertEquals(BekoCompatibleProfile.profile, injected.find(null))
    }

    @Test
    fun `catalog profile order is copied and cannot be mutated by callers`() {
        val supplied = mutableListOf(
            ArcelikOldLcdProfile.profile,
            BekoCompatibleProfile.profile,
        )
        val immutableCatalog = RemoteProfileCatalog(supplied)

        supplied.reverse()

        assertEquals(ArcelikOldLcdProfile.profile, immutableCatalog.profiles.first())
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (immutableCatalog.profiles as MutableList).add(GrundigCompatibleProfile.profile)
        }
    }

    @Test
    fun `unknown persisted profile falls back to Arcelik old LCD`() {
        assertEquals(ArcelikOldLcdProfile.profile, catalog.find("removed-profile"))
    }

    @Test
    fun `Arcelik and Grundig profiles keep every literal RC-YC1 core code`() {
        val expectedCodes = listOf(
            Triple(RemoteCommand.POWER, 0, 0x0C),
            Triple(RemoteCommand.SOURCE, 0, 0x38),
            Triple(RemoteCommand.VOLUME_UP, 0, 0x10),
            Triple(RemoteCommand.VOLUME_DOWN, 0, 0x11),
            Triple(RemoteCommand.CHANNEL_UP, 0, 0x20),
            Triple(RemoteCommand.CHANNEL_DOWN, 0, 0x21),
            Triple(RemoteCommand.MENU, 0, 0x19),
            Triple(RemoteCommand.OK, 0, 0x35),
            Triple(RemoteCommand.UP, 0, 0x16),
            Triple(RemoteCommand.DOWN, 0, 0x17),
            Triple(RemoteCommand.LEFT, 0, 0x13),
            Triple(RemoteCommand.RIGHT, 0, 0x12),
        )

        expectedCodes.forEach { (remoteCommand, literalAddress, literalCommand) ->
            assertRc5Code(
                ArcelikOldLcdProfile.profile.commandFor(remoteCommand),
                address = literalAddress,
                command = literalCommand,
            )
            assertRc5Code(
                GrundigCompatibleProfile.profile.commandFor(remoteCommand),
                address = literalAddress,
                command = literalCommand,
            )
        }
    }

    @Test
    fun `RC-YC1 profiles use the distinct Enter code for OK`() {
        listOf(
            ArcelikOldLcdProfile.profile,
            BekoCompatibleProfile.profile,
            GrundigCompatibleProfile.profile,
        ).forEach { profile ->
            val menu = profile.commandFor(RemoteCommand.MENU)
            val ok = profile.commandFor(RemoteCommand.OK)

            assertRc5Code(ok, address = 0, command = 0x35)
            assertNotEquals(menu?.encode(false), ok?.encode(false))
        }
    }

    @Test
    fun `Beko counterpart uses the reviewed RC-YC1 power code`() {
        val arcelikPower = ArcelikOldLcdProfile.profile.commandFor(RemoteCommand.POWER)
        val bekoPower = BekoCompatibleProfile.profile.commandFor(RemoteCommand.POWER)

        assertRc5Code(bekoPower, address = 0, command = 0x0C)
        assertEquals(arcelikPower?.encode(false), bekoPower?.encode(false))
    }

    @Test
    fun `official LG families use NEC address 04 and only documented HDMI commands`() {
        val expectedHdmiCodes = mapOf(
            Lg50Pc1Dr42Lb1DrProfile.ID to mapOf(
                RemoteCommand.HDMI1 to 0xC6,
                RemoteCommand.HDMI2 to 0xCC,
            ),
            Lg32Lc50CbProfile.ID to mapOf(
                RemoteCommand.HDMI1 to 0xCE,
                RemoteCommand.HDMI2 to 0xCC,
            ),
            LgLc2DPc3DProfile.ID to mapOf(
                RemoteCommand.HDMI1 to 0xCE,
                RemoteCommand.HDMI2 to 0xCC,
            ),
            Lg70Profile.ID to mapOf(
                RemoteCommand.HDMI1 to 0xCE,
                RemoteCommand.HDMI2 to 0xCC,
                RemoteCommand.HDMI3 to 0xE9,
                RemoteCommand.HDMI4 to 0xDA,
            ),
        )

        expectedHdmiCodes.forEach { (profileId, codes) ->
            val profile = requireNotNull(catalog.findOrNull(profileId))
            assertEquals("LG", profile.brand)
            assertEquals(EvidenceTier.SOURCE_VERIFIED, profile.defaultEvidence.tier)
            assertEquals(RemoteLayoutTemplate.DIRECT_INPUT, profile.layout.template)
            assertEquals(InputStrategy.DISCRETE_COMMANDS, profile.inputCapability.strategy)
            assertEquals(codes.keys, profile.inputCapability.discreteCommands)
            assertNecCode(profile.commandFor(RemoteCommand.POWER), 0x04, 0x08)
            assertNecCode(profile.commandFor(RemoteCommand.SOURCE), 0x04, 0x0B)
            assertNecCode(profile.commandFor(RemoteCommand.OK), 0x04, 0x44)
            codes.forEach { (command, code) ->
                assertNecCode(profile.commandFor(command), 0x04, code)
            }
        }
    }

    @Test
    fun `LG aliases stay scoped to the exact manual families`() {
        assertEquals(
            listOf(
                "50PC1DR",
                "50PC1DRA",
                "50PC1DR-UA",
                "50PC1DRA-UA",
                "42LB1DR",
                "42LB1DRA",
            ),
            Lg50Pc1Dr42Lb1DrProfile.profile.modelAliases,
        )
        assertEquals(listOf("32LC50CB"), Lg32Lc50CbProfile.profile.modelAliases)
        assertEquals(
            listOf(
                "32LC2D",
                "32LC2DU",
                "37LC2D",
                "42LC2D",
                "42PC3D",
                "42PC3DV",
                "50PC3D",
                "60PC1D",
            ),
            LgLc2DPc3DProfile.profile.modelAliases,
        )
        assertEquals(
            listOf("32LG70", "42LG70", "47LG70", "52LG70"),
            Lg70Profile.profile.modelAliases,
        )
    }

    @Test
    fun `CC0 community profiles preserve literal protocols and essential commands`() {
        val samsung = SamsungAa5900484AProfile.profile
        assertEquals(listOf("AA59-00484A"), samsung.remoteAliases)
        assertSamsungCode(samsung.commandFor(RemoteCommand.POWER), 0x07, 0x02)
        assertSamsungCode(samsung.commandFor(RemoteCommand.SOURCE), 0x07, 0x01)
        assertSamsungCode(samsung.commandFor(RemoteCommand.OK), 0x07, 0x68)
        assertSamsungCode(samsung.commandFor(RemoteCommand.DIGIT_0), 0x07, 0x11)

        val toshiba = ToshibaCt8560Profile.profile
        assertTrue(toshiba.modelAliases.isEmpty())
        assertEquals(listOf("CT-8560"), toshiba.remoteAliases)
        assertNecCode(toshiba.commandFor(RemoteCommand.POWER), 0x40, 0x12)
        assertNecCode(toshiba.commandFor(RemoteCommand.SOURCE), 0x40, 0x14)
        assertNecCode(toshiba.commandFor(RemoteCommand.OK), 0x40, 0x21)

        val hitachi = HitachiCle1031Profile.profile
        assertEquals(
            listOf("32FHDSM6", "40FHDSM8", "50UHDSM8", "55UHDSM8"),
            hitachi.modelAliases,
        )
        assertNecCode(hitachi.commandFor(RemoteCommand.POWER), 0x50, 0x12)
        assertNecCode(hitachi.commandFor(RemoteCommand.MENU), 0x50, 0x49)
        assertNecCode(hitachi.commandFor(RemoteCommand.OK), 0x50, 0x0A)

        val jvc = JvcLt49Hw97URmC3311Profile.profile
        assertEquals(listOf("LT-49HW97U"), jvc.modelAliases)
        assertEquals(listOf("RM-C3311"), jvc.remoteAliases)
        assertSamsungCode(jvc.commandFor(RemoteCommand.POWER), 0x0E, 0x0C)
        assertSamsungCode(jvc.commandFor(RemoteCommand.SOURCE), 0x0E, 0x0F)
        assertSamsungCode(jvc.commandFor(RemoteCommand.OK), 0x0E, 0x46)

        listOf(samsung, toshiba, hitachi, jvc).forEach { profile ->
            assertEquals(EvidenceTier.SOURCE_VERIFIED, profile.defaultEvidence.tier)
            assertEquals(InputStrategy.SOURCE_ONLY, profile.inputCapability.strategy)
            assertTrue(RemoteCommand.POWER in profile.supportedCommands)
            assertTrue(RemoteCommand.SOURCE in profile.supportedCommands)
        }
    }

    @Test
    fun `catalog discovery aliases contain no case-insensitive duplicates`() {
        catalog.profiles.forEach { profile ->
            assertEquals(
                profile.modelAliases.size,
                profile.modelAliases.map(String::lowercase).toSet().size,
            )
            assertEquals(
                profile.remoteAliases.size,
                profile.remoteAliases.map(String::lowercase).toSet().size,
            )
            assertTrue(profile.compatibleBrands.none { it.equals(profile.brand, true) })
        }
        val exactModels = catalog.profiles.flatMap { profile ->
            profile.modelAliases.map { "${profile.brand.lowercase()}:${it.lowercase()}" }
        }
        assertEquals(exactModels.size, exactModels.toSet().size)
    }

    private fun assertRc5Code(value: Any?, address: Int, command: Int) {
        assertTrue(value is Rc5IrCommand)
        value as Rc5IrCommand
        assertEquals(address, value.address)
        assertEquals(command, value.command)
    }

    private fun assertNecCode(value: Any?, address: Int, command: Int) {
        assertTrue(value is NecIrCommand)
        value as NecIrCommand
        assertEquals(address, value.address)
        assertEquals(command, value.command)
        assertEquals(NecAddressMode.STANDARD, value.addressMode)
    }

    private fun assertSamsungCode(value: Any?, address: Int, command: Int) {
        assertTrue(value is Samsung32IrCommand)
        value as Samsung32IrCommand
        assertEquals(address, value.address)
        assertEquals(command, value.command)
    }
}
