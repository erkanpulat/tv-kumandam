package com.erkanpulat.tvkumandam

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationIdentityTest {
    @Test
    fun applicationUsesTheTvKumandamIdentity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val label = context.applicationInfo.loadLabel(context.packageManager)

        assertEquals("com.erkanpulat.tvkumandam", context.packageName)
        assertEquals("TV Kumandam", label)
    }
}
