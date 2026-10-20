package com.epicmems.stride

import android.app.Application
import com.epicmems.stride.di.AppContainer
import com.epicmems.stride.track.Notifications
import com.epicmems.stride.track.TrackingWatchdogWorker

/**
 * Application entry point. Owns the manual dependency container, makes sure the notification
 * channels exist, and arms the watchdog that keeps step counting alive.
 */
class StrideApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.ensureChannels(this)

        // Cheap and idempotent (unique periodic work, KEEP policy). Actual permission checks
        // happen inside the worker, so this can never crash on a fresh install.
        TrackingWatchdogWorker.schedule(this)
    }
}
