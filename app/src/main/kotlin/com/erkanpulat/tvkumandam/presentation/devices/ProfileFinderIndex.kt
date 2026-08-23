package com.erkanpulat.tvkumandam.presentation.devices

import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile

/** Deterministic, exact-match projection of profiles that are safe to test with Power. */
class ProfileFinderIndex(catalog: RemoteProfileCatalog) {
    private val candidates = catalog.profiles
        .filter { RemoteCommand.POWER in it.supportedCommands }
        .distinctBy(RemoteProfile::id)

    val brands: List<String> = candidates
        .flatMap { profile -> listOf(profile.brand) + profile.compatibleBrands }
        .distinct()

    fun modelsFor(brand: String): List<String> = candidates
        .filter { it.brand == brand }
        .flatMap(RemoteProfile::modelAliases)
        .distinct()

    /** A null model means “Modelimi bilmiyorum” and stays within declared brand compatibility. */
    fun candidates(brand: String, modelAlias: String?): List<RemoteProfile> = candidates.filter { profile ->
        val supportsBrand = profile.brand == brand || brand in profile.compatibleBrands
        supportsBrand && (modelAlias == null || modelAlias in profile.modelAliases)
    }
}
