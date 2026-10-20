package com.epicmems.stride.track

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.epicmems.stride.data.SettingsRepository
import java.util.concurrent.TimeUnit

/**
 * Safety net for the step counter.
 *
 * `stopWithTask="false"` keeps [StepTrackingService] alive when the app is swiped out of
 * Recents, and `START_STICKY` asks the system to restart it after a low-memory kill. Some OEM
 * builds ignore both. This worker runs every 15 minutes and simply makes sure tracking is
 * running — it is the standard, battery-friendly way to do this (no alarm hacks).
 *
 * It does nothing when the user has switched always-on tracking off in Settings.
 */
class TrackingWatchdogWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val enabled = runCatching {
            SettingsRepository(applicationContext).isTrackingEnabled()
        }.getOrDefault(true)

        if (!enabled) return Result.success()

        // Starting a foreground service from the background can be refused on Android 12+
        // unless the app is exempt from battery optimisation. If it is refused we simply try
        // again on the next run — never crash.
        StepTrackingService.start(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "stride-tracking-watchdog"

        fun schedule(context: Context) {
            runCatching {
                val request = PeriodicWorkRequestBuilder<TrackingWatchdogWorker>(
                    15, TimeUnit.MINUTES
                ).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }

        fun cancel(context: Context) {
            runCatching { WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME) }
        }
    }
}
