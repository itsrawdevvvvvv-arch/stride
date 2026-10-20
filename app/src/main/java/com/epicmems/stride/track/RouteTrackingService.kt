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
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.epicmems.stride.StrideApp
import com.epicmems.stride.data.RouteRepository
import com.epicmems.stride.data.local.RoutePointEntity
import com.epicmems.stride.domain.Geo
import com.epicmems.stride.domain.LatLng
import com.epicmems.stride.domain.RecordingStatus
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
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Records a GPS route (a "drawing") while the user walks.
 *
 * Design notes:
 * - **All database work happens on [ioScope].** Nothing blocks the main thread, so the live
 *   drawing and the rest of the UI stay smooth.
 * - The route is seeded from the last known location when it is recent and accurate, so the
 *   drawing appears immediately instead of after a cold GPS lock.
 * - Failures are *visible*: if location is switched off, permission is missing or no provider
 *   exists, that is published through [TrackingState.status] and shown in the UI rather than
 *   leaving the user staring at "waiting for GPS".
 */
class RouteTrackingService : Service() {

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var locationManager: LocationManager
    private lateinit var repository: RouteRepository

    @Volatile
    private var routeId: Long = 0L

    private var seq = 0
    private var distance = 0.0
    private var last: LatLng? = null
    private var startTime = 0L
    private var hasFix = false
    private var registered = false
    private var networkFallbackRegistered = false
    private val buffer = mutableListOf<RawFix>()
    private var timerJob: Job? = null

    /** A GPS fix held in memory until the route id is known. */
    private data class RawFix(
        val seq: Int,
        val lat: Double,
        val lng: Double,
        val time: Long,
        val accuracy: Float
    )

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onFix(location)

        @Deprecated("Required for API < 30")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        repository = (application as StrideApp).container.routeRepository

        Notifications.ensureChannels(this)
        startInForeground("0.00 km", "0:00")

        startTime = System.currentTimeMillis()
        TrackingState.reset()
        TrackingState.recording.value = true
        TrackingState.points.value = emptyList()

        if (!hasLocationPermission()) {
            TrackingState.status.value = RecordingStatus.PERMISSION_MISSING
            return
        }

        // Resolve the route row on a background thread — never block the UI thread.
        ioScope.launch {
            runCatching { repository.startRoute(startTime) }
                .onSuccess { routeId = it }
                .onFailure { Log.w(TAG, "could not create route row", it) }
        }

        registerUpdates()

        // A recent, accurate last-known fix lets the drawing start straight away.
        seedFromLastKnown()

        timerJob = mainScope.launch {
            while (isActive) {
                TrackingState.durationMillis.value = System.currentTimeMillis() - startTime
                delay(1_000L)
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

        // Bounded wait so shutdown can't hang, but the route still gets its final write.
        val job = ioScope.launch {
            runCatching {
                if (snapshot.isNotEmpty() && id != 0L) {
                    repository.appendPoints(snapshot.map { it.toEntity(id) })
                }
                if (id != 0L) {
                    repository.finishRoute(id, endTime, finalDistance, finalSeq)
                }
            }
        }
        runBlocking { withTimeoutOrNull(2_500L) { job.join() } }

        TrackingState.recording.value = false
        TrackingState.status.value = RecordingStatus.IDLE
        mainScope.cancel()
        ioScope.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- location

    @SuppressLint("MissingPermission") // guarded by hasLocationPermission() in onCreate
    private fun registerUpdates() {
        if (registered) return

        val gpsOn = runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }
            .getOrDefault(false)
        val networkOn = runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }
            .getOrDefault(false)

        val provider = when {
            gpsOn -> LocationManager.GPS_PROVIDER
            networkOn -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            TrackingState.status.value = RecordingStatus.LOCATION_OFF
            return
        }

        try {
            locationManager.requestLocationUpdates(provider, MIN_TIME_MS, MIN_DISTANCE_M, listener)
            registered = true
            TrackingState.status.value = RecordingStatus.ACQUIRING
            if (provider == LocationManager.GPS_PROVIDER) scheduleNetworkFallback()
        } catch (e: SecurityException) {
            Log.w(TAG, "location permission refused", e)
            TrackingState.status.value = RecordingStatus.PERMISSION_MISSING
        } catch (e: Exception) {
            Log.w(TAG, "could not register for location updates", e)
            TrackingState.status.value = RecordingStatus.UNAVAILABLE
        }
    }

    /**
     * If GPS hasn't produced anything after a while (walking between buildings, or a cold
     * start), also listen to the network provider so the user still gets a drawing.
     */
    private fun scheduleNetworkFallback() {
        mainScope.launch {
            delay(FALLBACK_AFTER_MS)
            if (hasFix || networkFallbackRegistered) return@launch
            val networkOn = runCatching {
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }.getOrDefault(false)
            if (!networkOn) return@launch
            runCatching {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    listener
                )
                networkFallbackRegistered = true
                Log.i(TAG, "GPS silent — also listening to the network provider")
            }
        }
    }

    @SuppressLint("MissingPermission") // guarded by hasLocationPermission() in onCreate
    private fun seedFromLastKnown() {
        val now = System.currentTimeMillis()
        val candidates = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val recent = candidates
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { now - it.time < LAST_KNOWN_MAX_AGE_MS && it.accuracy < LAST_KNOWN_MAX_ACCURACY_M }
            .maxByOrNull { it.time }

        if (recent != null) {
            Log.i(TAG, "seeded route from last known location (age ${now - recent.time} ms)")
            onFix(recent)
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    // ------------------------------------------------------------------- fixes

    private fun onFix(location: Location) {
        val point = LatLng(location.latitude, location.longitude)
        last?.let { distance += Geo.haversine(it, point) }
        last = point
        hasFix = true

        buffer += RawFix(
            seq = seq,
            lat = point.lat,
            lng = point.lng,
            time = location.time,
            accuracy = location.accuracy
        )
        seq++

        TrackingState.distanceMeters.value = distance
        TrackingState.points.value = TrackingState.points.value + point
        TrackingState.status.value = RecordingStatus.TRACKING

        if (buffer.size >= FLUSH_EVERY) flush() else updateNotification()
    }

    private fun RawFix.toEntity(id: Long) = RoutePointEntity(
        routeId = id,
        seq = seq,
        lat = lat,
        lng = lng,
        time = time,
        accuracy = accuracy
    )

    /** Writes buffered fixes on the IO scope. Never blocks the main thread. */
    private fun flush() {
        val id = routeId
        if (id == 0L || buffer.isEmpty()) return
        val batch = buffer.map { it.toEntity(id) }
        buffer.clear()
        ioScope.launch { runCatching { repository.appendPoints(batch) } }
        updateNotification()
    }

    // ------------------------------------------------------------- notification

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
        private const val TAG = "RouteTrackingService"

        /** Responsive enough to feel live while walking, cheap enough not to drain the battery. */
        private const val MIN_TIME_MS = 1_000L
        private const val MIN_DISTANCE_M = 1f

        private const val FLUSH_EVERY = 6
        private const val FALLBACK_AFTER_MS = 12_000L
        private const val LAST_KNOWN_MAX_AGE_MS = 5 * 60_000L
        private const val LAST_KNOWN_MAX_ACCURACY_M = 100f

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
