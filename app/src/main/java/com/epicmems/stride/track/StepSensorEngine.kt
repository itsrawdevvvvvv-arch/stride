package com.epicmems.stride.track

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.epicmems.stride.data.SettingsRepository
import com.epicmems.stride.data.StepRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Wraps the hardware `TYPE_STEP_COUNTER` sensor — the same battery-efficient,
 * hardware-backed counter Google Fit and Strava use. It reports the total number of steps
 * taken since the device last booted, so we persist the last raw reading and store the
 * difference. A reading lower than the stored one means the device rebooted, so we simply
 * re-baseline instead of recording a negative delta.
 *
 * Because the sensor is hardware-backed, counting continues while the app is closed and the
 * phone is in a pocket, as long as the foreground service is alive.
 */
class StepSensorEngine(
    context: Context,
    private val repository: StepRepository,
    private val settings: SettingsRepository
) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listener: SensorEventListener? = null

    fun hasSensor(): Boolean = sensor != null

    fun start() {
        val target = sensor ?: return
        if (listener != null) return
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val raw = event.values.getOrNull(0) ?: return
                onRawReading(raw)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        sensorManager.registerListener(l, target, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
    }

    private fun onRawReading(raw: Float) {
        scope.launch {
            val last = settings.lastRaw()
            if (last <= 0f || raw < last) {
                // First reading, or the counter reset after a reboot: re-baseline only.
                settings.setLastRaw(raw)
                return@launch
            }
            val delta = (raw - last).toInt()
            settings.setLastRaw(raw)
            if (delta > 0) repository.addSteps(delta)
        }
    }
}
