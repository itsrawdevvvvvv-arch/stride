package com.epicmems.stride.util

import com.epicmems.stride.data.local.StepDay
import com.epicmems.stride.data.local.StepHour
import java.time.DayOfWeek
import kotlin.random.Random

/**
 * Generates a plausible 45-day step history (with a realistic hour-of-day shape) so the
 * charts and heatmaps are meaningful on first launch, before the device has collected real
 * data. Clearly flagged as demo and removable from Settings.
 */
object DemoData {

    fun generate(days: Int, goal: Int): Pair<List<StepDay>, List<StepHour>> {
        val random = Random(seed = 7)
        val today = Dates.today()
        val dayRows = mutableListOf<StepDay>()
        val hourRows = mutableListOf<StepHour>()

        for (back in days downTo 1) {
            val date = today.minusDays(back.toLong())
            val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            val base = if (weekend) 9_400 else 7_100
            var steps = base + random.nextInt(-2_600, 2_600)
            if (random.nextInt(10) == 0) steps = (steps * 1.6).toInt() // occasional big day
            steps = steps.coerceAtLeast(650)

            dayRows += StepDay(
                date = Dates.key(date),
                steps = steps,
                goal = goal,
                isDemo = true
            )

            val weights = FloatArray(24) { hour -> weightFor(hour) }
            val total = weights.sum()
            var remaining = steps
            for (hour in 0 until 24) {
                val share = (weights[hour] / total * steps).toInt()
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
}
