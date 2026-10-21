package com.epicmems.stride.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A recorded route segment. Created and finalised automatically as you move. */
@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long = 0,
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0,
    val pointCount: Int = 0,
    /**
     * True when the path was estimated from motion sensors because GPS was unavailable
     * (indoors). Such a path is a rough shape, not a surveyed track.
     */
    val estimated: Boolean = false
)
