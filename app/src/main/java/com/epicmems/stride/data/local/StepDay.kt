package com.epicmems.stride.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One calendar day of accumulated steps. */
@Entity(tableName = "step_days")
data class StepDay(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val steps: Int,
    val goal: Int,
    val distanceMeters: Double = 0.0,
    /** Set when the user taps the day in the calendar to mark it complete by hand. */
    val marked: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
