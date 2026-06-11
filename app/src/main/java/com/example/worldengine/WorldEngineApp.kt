package com.example.worldengine

import android.app.Application
import com.example.worldengine.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class WorldEngineApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@WorldEngineApp)
            modules(appModule)
        }
    }
}
