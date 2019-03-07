package it.sephiroth.android.numberpicker.demo

import android.app.Application
import timber.log.Timber

class MainApplication : Application() {

    override fun onCreate() {
        Timber.plant(Timber.DebugTree())
        super.onCreate()
    }
}