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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Wraps the hardware `TYPE_STEP_COUNTER` sensor — the same battery-efficient,
 * hardware-backed counter Google Fit and Strava use. It reports the total number of steps
 * taken since the device last booted, so we persist the last raw reading and store the
 * difference. A reading lower than the stored one means the device rebooted, so we simply
 * re-baseline instead of recording a negative delta.
 *
 * Steps are **buffered and flushed on a short interval** rather than written on every sensor
 * event. That keeps database churn (and therefore recomposition) low, and gives the UI a
 * steady stream of small updates to animate between — which is what makes the counter look
 * smooth rather than jumpy.
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
    private val lock = Mutex()

    private var listener: SensorEventListener? = null
    private var ticker: Job? = null

    /** Steps observed but not yet written to the database. */
    private var pending = 0

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

        ticker = scope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                flush()
            }
        }
    }

    fun stop() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
        ticker?.cancel()
        ticker = null
        // Best-effort final write so nothing buffered is lost.
        scope.launch { flush() }
    }

    private fun onRawReading(raw: Float) {
        scope.launch {
            lock.withLock {
                val last = settings.lastRaw()
                if (last <= 0f || raw < last) {
                    // First reading, or the counter reset after a reboot: re-baseline only.
                    settings.setLastRaw(raw)
                    return@withLock
                }
                val delta = (raw - last).toInt()
                settings.setLastRaw(raw)
                if (delta <= 0) return@withLock

                pending += delta
                // A burst (walking fast, or after the app was dozing) is written immediately
                // so the displayed count never lags behind reality.
                if (pending >= IMMEDIATE_FLUSH) writeLocked()
            }
        }
    }

    private suspend fun flush() {
        lock.withLock { writeLocked() }
    }

    /** Caller must hold [lock]. */
    private suspend fun writeLocked() {
        if (pending <= 0) return
        val steps = pending
        pending = 0
        repository.addSteps(steps)
    }

    companion object {
        /** How often buffered steps are written: live enough to feel instant, calm enough to stay cheap. */
        private const val FLUSH_INTERVAL_MS = 1_200L

        /** A burst this large is written straight away. */
        private const val IMMEDIATE_FLUSH = 120
    }
}
