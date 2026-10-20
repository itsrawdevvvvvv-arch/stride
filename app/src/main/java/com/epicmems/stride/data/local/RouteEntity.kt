package com.epicmems.stride.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A recorded GPS route. */
@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long = 0,
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0,
    val pointCount: Int = 0
)
