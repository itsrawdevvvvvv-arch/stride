package com.epicmems.stride.track

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.epicmems.stride.MainActivity
import com.epicmems.stride.R
import com.epicmems.stride.util.Format

/** Notification channels and the two ongoing notifications the services show. */
object Notifications {

    const val CHANNEL_STEPS = "stride_steps"
    const val CHANNEL_ROUTES = "stride_routes"

    const val ID_STEPS = 1001
    const val ID_ROUTE = 1002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STEPS,
                context.getString(R.string.channel_steps_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_steps_desc)
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ROUTES,
                context.getString(R.string.channel_routes_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_routes_desc)
                setShowBadge(false)
            }
        )
    }

    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun stopIntent(context: Context, service: Class<*>): PendingIntent =
        PendingIntent.getService(
            context,
            1,
            Intent(context, service).setAction(Action.STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun stepsNotification(context: Context, steps: Int): Notification {
        val text = if (steps > 0) "${Format.steps(steps)} steps today"
        else context.getString(R.string.notif_steps_idle)
        return NotificationCompat.Builder(context, CHANNEL_STEPS)
            .setContentTitle(context.getString(R.string.notif_steps_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_stride)
            .setContentIntent(contentIntent(context))
            .addAction(0, context.getString(R.string.notif_stop), stopIntent(context, StepTrackingService::class.java))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun routeNotification(context: Context, distanceLabel: String, durationLabel: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ROUTES)
            .setContentTitle(context.getString(R.string.notif_route_title))
            .setContentText("$distanceLabel · $durationLabel")
            .setSmallIcon(R.drawable.ic_stat_stride)
            .setContentIntent(contentIntent(context))
            .addAction(0, context.getString(R.string.notif_stop), stopIntent(context, RouteTrackingService::class.java))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    object Action {
        const val STOP = "com.epicmems.stride.action.STOP"
    }
}
