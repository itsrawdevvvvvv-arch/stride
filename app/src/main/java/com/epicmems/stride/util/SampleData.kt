package com.epicmems.stride.util

import com.epicmems.stride.data.local.RouteEntity
import com.epicmems.stride.data.local.StepDay
import com.epicmems.stride.data.local.StepHour
import com.epicmems.stride.domain.LatLng
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Optional sample content, loaded only when the user taps "Load sample data" in Settings.
 * The step history is plausible; the routes deliberately draw recognisable shapes so the
 * GPS "drawing" feature is easy to see.
 */
object SampleData {

    fun history(days: Int, goal: Int): Pair<List<StepDay>, List<StepHour>> {
        val random = Random(seed = 11)
        val today = Dates.today()
        val dayRows = mutableListOf<StepDay>()
        val hourRows = mutableListOf<StepHour>()

        for (back in days downTo 0) {
            val date = today.minusDays(back.toLong())
            val weekend = date.dayOfWeek.value >= 6
            val base = if (weekend) 10_200 else 7_900
            var steps = (base + random.nextInt(-2_400, 2_400)).coerceAtLeast(650)
            if (random.nextInt(10) == 0) steps = (steps * 1.6).toInt()

            dayRows += StepDay(
                date = Dates.key(date),
                steps = steps,
                goal = goal,
                distanceMeters = steps * 0.75
            )

            val weights = FloatArray(24) { hour -> weightFor(hour) }
            val sum = weights.sum()
            var remaining = steps
            for (hour in 0 until 24) {
                val share = (weights[hour] / sum * steps).toInt()
                val value = share.coerceAtMost(remaining)
                hourRows += StepHour(Dates.key(date), hour, value)
                remaining -= value
            }
        }
        return dayRows to hourRows
    }

    private fun weightFor(hour: Int): Float = when (hour) {
        in 0..5 -> 0.02f
        in 6..8 -> 1.20f
        in 9..11 -> 0.80f
        in 12..13 -> 0.90f
        in 14..16 -> 0.60f
        in 17..19 -> 1.30f
        in 20..22 -> 0.70f
        else -> 0.10f
    }

    /** Three sample routes that draw a heart, a star and a loop. */
    fun routes(): List<Pair<RouteEntity, List<LatLng>>> {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        return listOf(
            RouteEntity(startTime = now - 2 * day) to heart(28.6139, 77.2090),
            RouteEntity(startTime = now - 4 * day) to star(28.6139, 77.2090),
            RouteEntity(startTime = now - 6 * day) to loop(28.6139, 77.2090)
        )
    }

    private fun heart(lat0: Double, lng0: Double, n: Int = 180, scale: Double = 0.0016): List<LatLng> =
        (0 until n).map { i ->
            val t = i.toDouble() / n * 2 * Math.PI
            val x = 16 * sin(t).pow(3)
            val y = 13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)
            LatLng(lat0 + y * scale / 16.0, lng0 + x * scale / 16.0)
        }

    private fun star(lat0: Double, lng0: Double, spikes: Int = 5, n: Int = 220, scale: Double = 0.0016): List<LatLng> =
        (0 until n).map { i ->
            val t = i.toDouble() / n * 2 * Math.PI
            val r = if ((i / (n / (spikes * 2))) % 2 == 0) 1.0 else 0.45
            LatLng(lat0 + cos(t) * r * scale, lng0 + sin(t) * r * scale)
        }

    private fun loop(lat0: Double, lng0: Double, n: Int = 160, scale: Double = 0.0016): List<LatLng> =
        (0 until n).map { i ->
            val t = i.toDouble() / n * 2 * Math.PI
            LatLng(
                lat0 + sin(t) * scale * 0.7 + sin(3 * t) * scale * 0.15,
                lng0 + cos(t) * scale + cos(2 * t) * scale * 0.2
            )
        }
}
