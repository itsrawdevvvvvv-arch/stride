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
 * Records the route automatically. There is no start or stop button — [StepTrackingService]
 * starts this service when it sees you moving and stops it when you have been still for a
 * while, and each run becomes one saved segment.
 *
 * There are two sources, chosen per segment:
 *  - **GPS** when a fix is available (outdoors);
 *  - **[IndoorPathEstimator]** — step + heading dead reckoning — when no fix arrives within a
 *    few seconds, which is the normal case inside a building.
 *
 * If an estimated segment later picks up a GPS fix, the estimated segment is closed and a new
 * GPS segment begins, so a walk that starts indoors and continues outdoors is captured as two
 * clean pieces instead of one that jumps.
 */
class RouteTrackingService : Service() {

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var locationManager: LocationManager
    private lateinit var repository: RouteRepository
    private var indoorEstimator: IndoorPathEstimator? = null

    @Volatile
    private var routeId: Long = 0L

    private var segmentActive = false
    private var sourceEstimated = false
    private var gpsFixAt = 0L
    private var gpsRegistered = false
    private var seq = 0
    private var distance = 0.0
    private var last: LatLng? = null
    private var startTime = 0L
    private var buffer = mutableListOf<RawFix>()
    private var timerJob: Job? = null

    private data class RawFix(
        val seq: Int,
        val lat: Double,
        val lng: Double,
        val time: Long,
        val accuracy: Float
    )

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onGpsFix(location)

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
        startInForeground("0.00 km", "0:00", false)

        if (!hasLocationPermission()) {
            TrackingState.status.value = RecordingStatus.PERMISSION_MISSING
            stopSelf()
            return
        }

        registerGps()

        // No fix shortly after starting? We are almost certainly indoors — fall back to the
        // motion-sensor estimate so the route still moves.
        mainScope.launch {
            delay(GPS_GRACE_MS)
            if (gpsFixAt == 0L && !segmentActive) {
                Log.i(TAG, "no GPS fix after ${GPS_GRACE_MS}ms — estimating the path indoors")
                switchToSegment(estimated = true)
            }
        }

        timerJob = mainScope.launch {
            while (isActive) {
                if (segmentActive) {
                    TrackingState.durationMillis.value = System.currentTimeMillis() - startTime
                }
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
        runCatching { if (gpsRegistered) locationManager.removeUpdates(listener) }
        timerJob?.cancel()

        val job = finaliseSegment()
        runBlocking { withTimeoutOrNull(2_500L) { job?.join() } }

        TrackingState.recording.value = false
        TrackingState.status.value = RecordingStatus.IDLE
        TrackingState.estimated.value = false
        mainScope.cancel()
        ioScope.cancel()
        super.onDestroy()
    }

    // -------------------------------------------------------------- segments

    /** Closes the current segment (discarding it if it was too short) and opens a new one. */
    private fun switchToSegment(estimated: Boolean) {
        if (segmentActive && sourceEstimated == estimated) return
        if (segmentActive) finaliseSegment()

        sourceEstimated = estimated
        segmentActive = true
        seq = 0
        distance = 0.0
        last = null
        buffer = mutableListOf()
        startTime = System.currentTimeMillis()

        TrackingState.points.value = emptyList()
        TrackingState.distanceMeters.value = 0.0
        TrackingState.durationMillis.value = 0.0
        TrackingState.recording.value = true
        TrackingState.estimated.value = estimated
        TrackingState.status.value = RecordingStatus.TRACKING

        val started = startTime
        ioScope.launch { runCatching { routeId = repository.startRoute(started, estimated) } }

        if (estimated) startEstimator()
    }

    private fun finaliseSegment(): Job? {
        val id = routeId
        routeId = 0L

        val points = buffer.toList()
        buffer = mutableListOf()
        val count = seq
        val total = distance
        val end = System.currentTimeMillis()
        val wasEstimated = sourceEstimated

        segmentActive = false
        indoorEstimator?.stop()
        indoorEstimator = null
        seq = 0
        distance = 0.0
        last = null
        TrackingState.estimated.value = false

        if (id == 0L) return null
        Log.i(TAG, "closing segment $id ($count points, estimated=$wasEstimated)")
        return ioScope.launch {
            runCatching {
                if (points.isNotEmpty()) repository.appendPoints(points.map { it.toEntity(id) })
                if (count >= MIN_POINTS) {
                    repository.finishRoute(id, end, total, count)
                } else {
                    // A couple of points is just noise, not a walk.
                    repository.discardRoute(id)
                }
            }
        }
    }

    private fun startEstimator() {
        if (indoorEstimator != null) return
        val base = lastKnownBase() ?: LatLng(0.0, 0.0)
        val estimator = IndoorPathEstimator(this, base) { point ->
            appendPoint(point, accuracy = 0f, time = System.currentTimeMillis())
        }
        if (!estimator.hasStepDetector()) {
            Log.w(TAG, "no step detector — cannot estimate an indoor path")
            TrackingState.status.value = RecordingStatus.UNAVAILABLE
            return
        }
        estimator.start()
        indoorEstimator = estimator
    }

    // ----------------------------------------------------------------- points

    private fun onGpsFix(location: Location) {
        gpsFixAt = System.currentTimeMillis()

        // Hand over cleanly from an estimated path to real GPS.
        if (segmentActive && sourceEstimated) {
            switchToSegment(estimated = false)
        } else if (!segmentActive) {
            switchToSegment(estimated = false)
        }

        appendPoint(
            point = LatLng(location.latitude, location.longitude),
            accuracy = location.accuracy,
            time = location.time
        )
    }

    private fun appendPoint(point: LatLng, accuracy: Float, time: Long) {
        last?.let { distance += Geo.haversine(it, point) }
        last = point

        buffer += RawFix(seq, point.lat, point.lng, time, accuracy)
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

    /** Writes buffered points on the IO scope. Never blocks the UI thread. */
    private fun flush() {
        val id = routeId
        if (id == 0L || buffer.isEmpty()) return
        val batch = buffer.map { it.toEntity(id) }
        buffer = mutableListOf()
        ioScope.launch { runCatching { repository.appendPoints(batch) } }
        updateNotification()
    }

    // ---------------------------------------------------------------- location

    @SuppressLint("MissingPermission") // guarded by hasLocationPermission() in onCreate
    private fun registerGps() {
        if (gpsRegistered) return
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
            gpsRegistered = true
            TrackingState.status.value = RecordingStatus.ACQUIRING
        } catch (e: SecurityException) {
            TrackingState.status.value = RecordingStatus.PERMISSION_MISSING
        } catch (e: Exception) {
            Log.w(TAG, "could not register for location", e)
            TrackingState.status.value = RecordingStatus.UNAVAILABLE
        }
    }

    @SuppressLint("MissingPermission") // guarded by hasLocationPermission() in onCreate
    private fun lastKnownBase(): LatLng? {
        val now = System.currentTimeMillis()
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .filter { now - it.time < LAST_KNOWN_MAX_AGE_MS }
            .maxByOrNull { it.time }
            ?.let { LatLng(it.latitude, it.longitude) }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    // ------------------------------------------------------------- notification

    private fun startInForeground(distanceLabel: String, durationLabel: String, estimated: Boolean) {
        runCatching {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                Notifications.ID_ROUTE,
                Notifications.routeNotification(this, distanceLabel, durationLabel, estimated),
                type
            )
        }.onFailure { Log.w(TAG, "could not enter the foreground", it) }
    }

    @SuppressLint("MissingPermission") // FGS notification; POST_NOTIFICATIONS is optional
    private fun updateNotification() {
        runCatching {
            NotificationManagerCompat.from(this).notify(
                Notifications.ID_ROUTE,
                Notifications.routeNotification(
                    this,
                    Format.distance(distance, "km"),
                    Format.duration(System.currentTimeMillis() - startTime),
                    sourceEstimated
                )
            )
        }
    }

    companion object {
        private const val TAG = "RouteTrackingService"

        private const val MIN_TIME_MS = 1_000L
        private const val MIN_DISTANCE_M = 1f
        private const val FLUSH_EVERY = 6
        private const val MIN_POINTS = 4
        private const val GPS_GRACE_MS = 10_000L
        private const val LAST_KNOWN_MAX_AGE_MS = 60 * 60_000L

        fun hasLocationPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        /** Idempotent: safe to call repeatedly while a segment is already running. */
        fun start(context: Context) {
            if (!hasLocationPermission(context)) return
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, RouteTrackingService::class.java)
                )
            }.onFailure { Log.w(TAG, "start refused", it) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, RouteTrackingService::class.java)) }
        }
    }
}
