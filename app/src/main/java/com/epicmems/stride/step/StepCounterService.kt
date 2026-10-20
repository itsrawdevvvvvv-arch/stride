package com.epicmems.stride.step

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.epicmems.stride.MainActivity
import com.epicmems.stride.R
import com.epicmems.stride.StrideApp
import com.epicmems.stride.util.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Keeps the step sensor registered while the app is in the background, via a low-importance
 * ongoing notification. The notification text tracks today's live step count.
 */
class StepCounterService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var engine: StepSensorEngine

    override fun onCreate() {
        super.onCreate()
        val container = (application as StrideApp).container
        engine = container.stepSensorEngine

        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(0),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            } else {
                0
            }
        )

        engine.start()

        scope.launch {
            container.stepRepository.observeDay(Dates.key(Dates.today()))
                .collectLatest { day ->
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(NOTIF_ID, buildNotification(day?.steps ?: 0))
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(steps: Int): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = if (steps > 0) "$steps steps today" else getString(R.string.notif_text_idle)
        return NotificationCompat.Builder(this, StrideApp.CHANNEL_TRACKING)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_stride)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIF_ID = 4242
    }
}
