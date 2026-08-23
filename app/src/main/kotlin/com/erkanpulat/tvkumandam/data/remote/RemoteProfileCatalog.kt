package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.data.remote.profiles.HitachiCle1031Profile
import com.erkanpulat.tvkumandam.data.remote.profiles.JvcLt49Hw97URmC3311Profile
import com.erkanpulat.tvkumandam.data.remote.profiles.Lg32Lc50CbProfile
import com.erkanpulat.tvkumandam.data.remote.profiles.Lg50Pc1Dr42Lb1DrProfile
import com.erkanpulat.tvkumandam.data.remote.profiles.Lg70Profile
import com.erkanpulat.tvkumandam.data.remote.profiles.LgLc2DPc3DProfile
import com.erkanpulat.tvkumandam.data.remote.profiles.SamsungAa5900484AProfile
import com.erkanpulat.tvkumandam.data.remote.profiles.ToshibaCt8560Profile
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import java.util.Collections

/** In-memory registry; unknown ids fall back to the first ordered profile. */
class RemoteProfileCatalog(
    profiles: List<RemoteProfile> = bundledProfiles,
) {
    val profiles: List<RemoteProfile> = Collections.unmodifiableList(profiles.toList())
    private val profilesById = this.profiles.associateBy(RemoteProfile::id)
    private val fallbackProfile: RemoteProfile

    init {
        require(this.profiles.isNotEmpty()) { "At least one remote profile is required." }
        require(profilesById.size == this.profiles.size) { "Remote profile ids must be unique." }
        fallbackProfile = this.profiles.first()
    }

    fun find(id: String?): RemoteProfile =
        profilesById[id] ?: fallbackProfile

    fun findOrNull(id: String): RemoteProfile? = profilesById[id]

    companion object {
        private val bundledProfiles = listOf(
            ArcelikOldLcdProfile.profile,
            BekoCompatibleProfile.profile,
            GrundigCompatibleProfile.profile,
            Lg50Pc1Dr42Lb1DrProfile.profile,
            Lg32Lc50CbProfile.profile,
            LgLc2DPc3DProfile.profile,
            Lg70Profile.profile,
            SamsungAa5900484AProfile.profile,
            ToshibaCt8560Profile.profile,
            HitachiCle1031Profile.profile,
            JvcLt49Hw97URmC3311Profile.profile,
        )
    }
}
