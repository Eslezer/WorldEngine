package com.example.worldengine

import android.app.Application
import com.example.worldengine.core.data.prefs.CalendarRepository
import com.example.worldengine.core.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WorldEngineApp : Application(), KoinComponent {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val calendarRepository: CalendarRepository by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@WorldEngineApp)
            modules(appModule)
        }
        // Make the built-in default calendar available out of the box (first run only).
        applicationScope.launch { calendarRepository.seedDefaultsIfNeeded() }
    }
}
