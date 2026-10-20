package com.epicmems.stride.track

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.epicmems.stride.StrideApp
import com.epicmems.stride.data.RouteRepository
import com.epicmems.stride.data.local.RoutePointEntity
import com.epicmems.stride.domain.Geo
import com.epicmems.stride.domain.LatLng
import com.epicmems.stride.util.Format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Records a GPS route (a "drawing") while the user walks. Uses the platform
 * [LocationManager] with the GPS provider, so the app needs no proprietary location
 * dependency. Points are buffered in memory for live drawing and written to the database in
 * small batches, so a route survives the app being killed.
 */
class RouteTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var locationManager: LocationManager
    private lateinit var repository: RouteRepository

    private var routeId: Long = 0L
    private var seq: Int = 0
    private var distance: Double = 0.0
    private var last: LatLng? = null
    private var startTime: Long = 0L
    private val buffer = mutableListOf<RoutePointEntity>()
    private var timerJob: Job? = null

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onFix(location)

        @Deprecated("Required for API < 30")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    @SuppressLint("MissingPermission") // guarded by the checkSelfPermission call below
    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        repository = (application as StrideApp).container.routeRepository

        Notifications.ensureChannels(this)
        startInForeground("0.00 km", "0:00")

        // A route needs location; bail out cleanly if the user refused it.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        startTime = System.currentTimeMillis()
        TrackingState.reset()
        TrackingState.recording.value = true
        TrackingState.points.value = emptyList()

        // Resolve the route id synchronously so points always have a parent row.
        routeId = runBlocking { repository.startRoute(startTime) }

        runCatching {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_MS,
                MIN_DISTANCE_M,
                listener
            )
        }

        timerJob = scope.launch {
            while (isActive) {
                TrackingState.durationMillis.value = System.currentTimeMillis() - startTime
                delay(1_000)
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
        runCatching { locationManager.removeUpdates(listener) }
        timerJob?.cancel()

        val snapshot = buffer.toList()
        buffer.clear()
        val finalDistance = distance
        val finalSeq = seq
        val id = routeId
        val endTime = System.currentTimeMillis()

        // Flush synchronously — a short write, and it must not be cancelled with the scope.
        runBlocking {
            runCatching {
                if (snapshot.isNotEmpty()) repository.appendPoints(snapshot)
                if (id != 0L) repository.finishRoute(id, endTime, finalDistance, finalSeq)
            }
        }

        TrackingState.recording.value = false
        scope.cancel()
        super.onDestroy()
    }

    private fun onFix(location: Location) {
        val point = LatLng(location.latitude, location.longitude)
        last?.let { distance += Geo.haversine(it, point) }
        last = point

        buffer += RoutePointEntity(
            routeId = routeId,
            seq = seq,
            lat = point.lat,
            lng = point.lng,
            time = location.time,
            accuracy = location.accuracy
        )
        seq++

        TrackingState.distanceMeters.value = distance
        TrackingState.points.value = TrackingState.points.value + point

        if (buffer.size >= FLUSH_EVERY) flush()
        else updateNotification()
    }

    private fun flush() {
        val batch = buffer.toList()
        buffer.clear()
        val id = routeId
        if (id == 0L) return
        runBlocking { runCatching { repository.appendPoints(batch) } }
        updateNotification()
    }

    private fun startInForeground(distanceLabel: String, durationLabel: String) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            Notifications.ID_ROUTE,
            Notifications.routeNotification(this, distanceLabel, durationLabel),
            type
        )
    }

    @SuppressLint("MissingPermission") // FGS notification; POST_NOTIFICATIONS is optional
    private fun updateNotification() {
        runCatching {
            NotificationManagerCompat.from(this).notify(
                Notifications.ID_ROUTE,
                Notifications.routeNotification(
                    this,
                    Format.distance(distance, "km"),
                    Format.duration(System.currentTimeMillis() - startTime)
                )
            )
        }
    }

    companion object {
        private const val MIN_TIME_MS = 2_000L
        private const val MIN_DISTANCE_M = 3f
        private const val FLUSH_EVERY = 8

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, RouteTrackingService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RouteTrackingService::class.java))
        }
    }
}
