package com.epicmems.stride.track

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.epicmems.stride.domain.LatLng
import kotlin.math.cos
import kotlin.math.sin

/**
 * Estimates an indoor walking path from motion sensors alone.
 *
 * GPS needs a clear view of the sky, so inside a building there are simply no fixes. This does
 * pedestrian dead reckoning: each detected step advances the position by one stride, in the
 * direction the phone is currently pointing.
 *
 * It is an **estimate**. Heading drifts, stride length is assumed rather than measured, and
 * without a position fix there is nothing to correct against — so the shape is a rough record
 * of where you went, not a survey. It is deliberately only used when GPS is unavailable.
 */
class IndoorPathEstimator(
    context: Context,
    base: LatLng,
    private val onPoint: (LatLng) -> Unit
) {

    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepDetector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    /** Game rotation vector ignores the magnetometer, so indoor magnetic interference can't skew it. */
    private val headingSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val baseLat = base.lat
    private val baseLng = base.lng

    private var headingRad = 0f
    private var east = 0.0
    private var north = 0.0
    private var listening = false

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                    runCatching {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        // orientation[0] is the azimuth in radians: 0 = ahead, clockwise positive.
                        headingRad = orientation[0]
                    }
                }

                Sensor.TYPE_STEP_DETECTOR -> advance()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun hasStepDetector(): Boolean = stepDetector != null

    fun start() {
        if (listening || stepDetector == null) return
        if (!hasActivityPermission()) return
        sensorManager.registerListener(listener, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
        headingSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        listening = true
    }

    fun stop() {
        if (!listening) return
        sensorManager.unregisterListener(listener)
        listening = false
    }

    private fun hasActivityPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /** One step forwards, in the direction we are currently facing. */
    private fun advance() {
        east += STRIDE_METERS * sin(headingRad)
        north += STRIDE_METERS * cos(headingRad)
        onPoint(toLatLng(east, north))
    }

    /**
     * Local metres -> lat/lng offsets around the base. The absolute position is arbitrary
     * (there is no fix to anchor to); only the shape matters, and the map view normalises it.
     */
    private fun toLatLng(east: Double, north: Double): LatLng {
        val lat = baseLat + north / METERS_PER_DEGREE
        val lng = baseLng + east / (METERS_PER_DEGREE * cos(Math.toRadians(baseLat)))
        return LatLng(lat, lng)
    }

    companion object {
        private const val STRIDE_METERS = 0.72
        private const val METERS_PER_DEGREE = 111_320.0
    }
}
