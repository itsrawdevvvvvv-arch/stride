package com.epicmems.stride.step

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
 * Wraps the hardware `TYPE_STEP_COUNTER` sensor.
 *
 * That sensor reports the total steps taken since the device last booted, so we persist the
 * last raw reading and store the difference. A reading lower than the stored one means the
 * device rebooted (or the counter reset) — we simply re-baseline rather than record a
 * negative delta.
 */
class StepSensorEngine(
    context: Context,
    private val repository: StepRepository,
    private val settings: SettingsRepository
) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listener: SensorEventListener? = null

    fun hasSensor(): Boolean = sensor != null

    fun start() {
        val target = sensor ?: return
        if (listener != null) return
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onRawReading(event.values.getOrNull(0) ?: return)
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
            if (delta > 0) {
                repository.addSteps(delta)
            }
        }
    }
}
