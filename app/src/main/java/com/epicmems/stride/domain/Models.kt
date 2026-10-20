package com.epicmems.stride.domain

import com.epicmems.stride.util.Dates
import java.time.LocalDate

/** One day of step data, ready for the UI. */
data class DayStat(
    val date: LocalDate,
    val steps: Int,
    val goal: Int,
    val isDemo: Boolean
) {
    val goalHit: Boolean get() = steps >= goal

    val progress: Float
        get() = if (goal <= 0) 0f else (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
}

/** Everything the screens render. Produced by [com.epicmems.stride.ui.StrideViewModel]. */
data class StrideUiState(
    val loading: Boolean = true,
    val today: LocalDate = Dates.today(),
    val todaySteps: Int = 0,
    val goal: Int = 8000,
    val unitLabel: String = "km",
    val hasSensor: Boolean = true,
    val demoActive: Boolean = false,
    val last7: List<DayStat> = emptyList(),
    val last30: List<DayStat> = emptyList(),
    val monthDays: List<DayStat> = emptyList(),
    val todayHours: List<Int> = List(24) { 0 },
    val avg7: Int = 0,
    val avg30: Int = 0,
    val best: DayStat? = null,
    val streak: Int = 0,
    val goalHitRate30: Int = 0,
    val total30: Int = 0
)
