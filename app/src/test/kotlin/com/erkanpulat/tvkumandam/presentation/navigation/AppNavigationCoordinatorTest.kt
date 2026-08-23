package com.erkanpulat.tvkumandam.presentation.navigation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppNavigationCoordinatorTest {
    @Test
    fun `rapid requests serialize handoff and the latest request wins`() = runTest {
        var current: AppDestination = AppDestination.Remote
        val firstHandoffEntered = CompletableDeferred<Unit>()
        val releaseFirstHandoff = CompletableDeferred<Unit>()
        val committed = mutableListOf<AppDestination>()
        val coordinator = AppNavigationCoordinator(
            scope = this,
            currentDestination = { current },
            beforeLeave = { source ->
                if (source == AppDestination.Remote) {
                    firstHandoffEntered.complete(Unit)
                    releaseFirstHandoff.await()
                }
            },
            beforeEnter = {},
            commit = { destination ->
                current = destination
                committed += destination
            },
        )

        val first = coordinator.navigate(AppDestination.Devices)
        firstHandoffEntered.await()
        val latest = coordinator.navigate(AppDestination.Settings)
        runCurrent()

        assertFalse(first.isCompleted)
        assertFalse(latest.isCompleted)
        assertEquals(AppDestination.Remote, current)

        releaseFirstHandoff.complete(Unit)
        first.join()
        latest.join()

        assertEquals(listOf(AppDestination.Devices, AppDestination.Settings), committed)
        assertEquals(AppDestination.Settings, current)
    }
}
