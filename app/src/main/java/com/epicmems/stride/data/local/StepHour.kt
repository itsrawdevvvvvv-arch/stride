package com.epicmems.stride.data.local

import androidx.room.Entity

/** Steps bucketed into a single hour of a single day — powers the hour-of-day heatmap. */
@Entity(tableName = "step_hours", primaryKeys = ["date", "hour"])
data class StepHour(
    val date: String, // yyyy-MM-dd
    val hour: Int,    // 0..23
    val steps: Int
)
