package com.epicmems.stride.track

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.epicmems.stride.StrideApp
import com.epicmems.stride.util.Dates
import com.epicmems.stride.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps the step sensor registered and shows a permanent, ongoing notification with today's
 * count.
 *
 * Surviving "the user closed the app" is deliberate and layered:
 *  - the manifest declares `android:stopWithTask="false"`, so swiping Stride out of Recents
 *    does **not** stop this service;
 *  - [onTaskRemoved] re-registers the sensor and re-posts the notification anyway;
 *  - `START_STICKY` asks the system to restart us after a low-memory kill;
 *  - [TrackingWatchdogWorker] re-starts us every 15 minutes if an aggressive OEM build
 *    killed us regardless;
 *  - [BootReceiver] brings us back after a reboot.
 *
 * The one thing nothing can survive is the user tapping **Force stop** — Android disables the
 * app until it is launched again, by design.
 */
class StepTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var engine: StepSensorEngine
    private var widgetJob: Job? = null
    private var lastSteps = 0

    override fun onCreate() {
        super.onCreate()

        // Without activity-recognition permission there is no step data, and on Android 14
        // starting a health-type foreground service without it throws. Stop cleanly instead.
        if (!hasActivityPermission(this)) {
            stopSelf()
            return
        }

        val container = (application as StrideApp).container
        engine = container.stepSensorEngine

        Notifications.ensureChannels(this)
        startInForeground(lastSteps)
        engine.start()

        scope.launch {
            container.stepRepository.observeDay(Dates.key(Dates.today()))
                .collectLatest { day ->
                    val steps = day?.steps ?: 0
                    lastSteps = steps
                    updateNotification(steps)
                }
        }

        // Keep the home-screen widget reasonably fresh while tracking is alive.
        widgetJob = scope.launch {
            while (isActive) {
                runCatching { WidgetUpdater.updateAll(this@StepTrackingService) }
                delay(60_000L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Called again when the system restarts us (START_STICKY) or the watchdog pokes us.
        // Make sure we are actually working and visible.
        engine.start()
        startInForeground(lastSteps)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * The user swiped Stride out of Recents. Counting must not stop: re-register the sensor
     * and re-post the ongoing notification in case it had been dismissed.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "task removed — keeping tracking alive")
        engine.start()
        startInForeground(lastSteps)
    }

    override fun onDestroy() {
        engine.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground(steps: Int) {
        runCatching {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                Notifications.ID_STEPS,
                Notifications.stepsNotification(this, steps),
                type
            )
        }.onFailure { Log.w(TAG, "could not enter the foreground", it) }
    }

    @SuppressLint("MissingPermission") // FGS notification; POST_NOTIFICATIONS is optional
    private fun updateNotification(steps: Int) {
        runCatching {
            NotificationManagerCompat.from(this)
                .notify(Notifications.ID_STEPS, Notifications.stepsNotification(this, steps))
        }
    }

    companion object {
        private const val TAG = "StepTrackingService"

        fun hasActivityPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED

        /**
         * Safe to call from anywhere — including the background and from the watchdog.
         * Never throws: a refused background start simply means we try again later.
         */
        fun start(context: Context) {
            if (!hasActivityPermission(context)) return
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, StepTrackingService::class.java)
                )
            }.onFailure { Log.w(TAG, "start refused", it) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, StepTrackingService::class.java)) }
        }
    }
}
