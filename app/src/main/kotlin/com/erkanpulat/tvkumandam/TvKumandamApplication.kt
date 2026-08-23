package com.erkanpulat.tvkumandam

import android.app.Application

class TvKumandamApplication : Application() {
    val appContainer: AppContainer by lazy(LazyThreadSafetyMode.NONE) {
        AppContainer(applicationContext)
    }
}
