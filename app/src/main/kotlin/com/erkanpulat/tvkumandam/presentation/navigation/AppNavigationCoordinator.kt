package com.erkanpulat.tvkumandam.presentation.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes destination hand-offs so rapid requests cannot overtake cancellation/join work. */
internal class AppNavigationCoordinator(
    private val scope: CoroutineScope,
    private val currentDestination: () -> AppDestination?,
    private val beforeLeave: suspend (AppDestination?) -> Unit,
    private val beforeEnter: suspend (AppDestination) -> Unit,
    private val commit: (AppDestination) -> Unit,
) {
    private val mutex = Mutex()

    fun navigate(target: AppDestination): Job = scope.launch {
        mutex.withLock {
            val source = currentDestination()
            if (source == target) return@withLock
            beforeLeave(source)
            beforeEnter(target)
            commit(target)
        }
    }
}
