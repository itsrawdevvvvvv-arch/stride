package com.epicmems.stride.track

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts step counting after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        runCatching { StepTrackingService.start(context) }
    }
}
