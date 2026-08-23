package com.erkanpulat.tvkumandam.presentation.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomizeRemoteDestinationOwnerTest {
    @Test
    fun `clearing a finished editor destroys its scoped ViewModels before the next session`() {
        val owner = CustomizeRemoteDestinationOwner()
        val first = provider(owner)[ProbeViewModel::class.java]

        owner.clearDestination()

        val second = provider(owner)[ProbeViewModel::class.java]
        assertTrue(first.wasCleared)
        assertNotSame(first, second)
    }

    private fun provider(owner: CustomizeRemoteDestinationOwner) = ViewModelProvider(
        owner,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ProbeViewModel() as T
            }
        },
    )

    private class ProbeViewModel : ViewModel() {
        var wasCleared = false
            private set

        override fun onCleared() {
            wasCleared = true
        }
    }
}
