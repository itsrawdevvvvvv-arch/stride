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
 * Keeps the step sensor registered, shows a permanent ongoing notification with today's count,
 * and — because there is no start/stop button any more — decides when to record a route.
 *
 * Route recording is driven purely by movement: a step in the last [MOVING_WINDOW_MS] means
 * "walking, record", and [STOP_WINDOW_MS] without a step means "stopped, close the segment".
 * Each segment is saved against the day it happened, and the day rolling over closes it too.
 */
class StepTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var engine: StepSensorEngine
    private var widgetJob: Job? = null
    private var watcherJob: Job? = null
    private var lastSteps = 0
    private var lastMovementAt = 0L
    private var lastDayKey = ""

    override fun onCreate() {
        super.onCreate()

        if (!hasActivityPermission(this)) {
            stopSelf()
            return
        }

        val container = (application as StrideApp).container
        engine = container.stepSensorEngine

        Notifications.ensureChannels(this)
        startInForeground(lastSteps)
        engine.start()

        lastDayKey = Dates.key(Dates.today())

        scope.launch {
            container.stepRepository.observeDay(Dates.key(Dates.today()))
                .collectLatest { day ->
                    val steps = day?.steps ?: 0
                    if (steps > lastSteps) lastMovementAt = System.currentTimeMillis()
                    lastSteps = steps
                    updateNotification(steps)
                }
        }

        widgetJob = scope.launch {
            while (isActive) {
                runCatching { WidgetUpdater.updateAll(this@StepTrackingService) }
                delay(60_000L)
            }
        }

        watcherJob = scope.launch {
            while (isActive) {
                delay(20_000L)
                runCatching { superviseRouteRecording() }
            }
        }
    }

    /**
     * Starts and stops the route recorder based on whether the user is actually moving.
     * This is what replaced the start/stop buttons.
     */
    private suspend fun superviseRouteRecording() {
        val container = (application as StrideApp).container
        val auto = runCatching { container.settingsRepository.isAutoRecordEnabled() }.getOrDefault(true)
        val now = System.currentTimeMillis()
        val recording = TrackingState.recording.value

        // A new day closes whatever was running so segments stay grouped per day.
        val dayKey = Dates.key(Dates.today())
        if (dayKey != lastDayKey) {
            lastDayKey = dayKey
            if (recording) RouteTrackingService.stop(this)
            return
        }

        if (!auto) {
            if (recording) RouteTrackingService.stop(this)
            return
        }

        val movedRecently = now - lastMovementAt < MOVING_WINDOW_MS
        val stillForAges = now - lastMovementAt > STOP_WINDOW_MS

        if (movedRecently && !recording) {
            RouteTrackingService.start(this)
        } else if (recording && stillForAges) {
            RouteTrackingService.stop(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        engine.start()
        startInForeground(lastSteps)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

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

        /** A step within this window counts as "currently walking". */
        private const val MOVING_WINDOW_MS = 60_000L

        /** No step for this long closes the current route segment. */
        private const val STOP_WINDOW_MS = 180_000L

        fun hasActivityPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED

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
