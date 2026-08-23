package com.erkanpulat.tvkumandam.presentation.navigation

sealed interface AppDestination {
    data object Welcome : AppDestination
    data object Remote : AppDestination
    data object Devices : AppDestination
    data object Settings : AppDestination
    data class Customize(val remoteId: String) : AppDestination
    data object AddDevice : AppDestination
}
