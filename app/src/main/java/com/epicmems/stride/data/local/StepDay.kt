package com.epicmems.stride.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single calendar day of accumulated steps. */
@Entity(tableName = "step_days")
data class StepDay(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val steps: Int,
    val goal: Int,
    val isDemo: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
