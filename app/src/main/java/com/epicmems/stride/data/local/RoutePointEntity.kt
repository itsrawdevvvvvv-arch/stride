package com.epicmems.stride.data.local

import androidx.room.Entity

/** One GPS fix belonging to a route. `seq` keeps the ordering stable. */
@Entity(tableName = "route_points", primaryKeys = ["routeId", "seq"])
data class RoutePointEntity(
    val routeId: Long,
    val seq: Int,
    val lat: Double,
    val lng: Double,
    val time: Long,
    val accuracy: Float = 0f
)
