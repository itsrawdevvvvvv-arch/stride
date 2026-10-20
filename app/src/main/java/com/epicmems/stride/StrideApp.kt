package com.epicmems.stride

import android.app.Application
import com.epicmems.stride.di.AppContainer
import com.epicmems.stride.track.Notifications

/**
 * Application entry point. Owns the manual dependency container and makes sure the
 * notification channels exist before any service tries to post.
 */
class StrideApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.ensureChannels(this)
    }
}
