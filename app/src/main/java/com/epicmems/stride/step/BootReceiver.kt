package com.epicmems.stride.step

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Best-effort restart of tracking after a reboot (the raw counter resets at boot). */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, StepCounterService::class.java)
            )
        } catch (_: Exception) {
            // Some OEMs block background FGS starts; the app will start it on next launch.
        }
    }
}
