package com.epicmems.stride.track

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
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
 * Keeps the step sensor registered while the app is in the background, with a quiet ongoing
 * notification that tracks today's count. Started on app launch and after a reboot.
 */
class StepTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var engine: StepSensorEngine
    private var widgetJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as StrideApp).container
        engine = container.stepSensorEngine

        Notifications.ensureChannels(this)
        startInForeground(0)
        engine.start()

        scope.launch {
            container.stepRepository.observeDay(Dates.key(Dates.today()))
                .collectLatest { day -> updateNotification(day?.steps ?: 0) }
        }

        // Keep the home-screen widget reasonably fresh while tracking is alive.
        widgetJob = scope.launch {
            while (isActive) {
                runCatching { WidgetUpdater.updateAll(this@StepTrackingService) }
                delay(60_000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Notifications.Action.STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground(steps: Int) {
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
    }

    private fun updateNotification(steps: Int) {
        runCatching {
            NotificationManagerCompat.from(this)
                .notify(Notifications.ID_STEPS, Notifications.stepsNotification(this, steps))
        }
    }

    companion object {
        fun start(context: android.content.Context) {
            androidx.core.content.ContextCompat.startForegroundService(
                context,
                Intent(context, StepTrackingService::class.java)
            )
        }
    }
}
