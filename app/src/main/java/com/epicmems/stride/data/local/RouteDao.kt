package com.epicmems.stride.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Insert
    suspend fun insertRoute(route: RouteEntity): Long

    @Insert
    suspend fun insertPoints(points: List<RoutePointEntity>)

    @Update
    suspend fun updateRoute(route: RouteEntity)

    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    suspend fun route(id: Long): RouteEntity?

    @Query("SELECT * FROM routes ORDER BY startTime DESC")
    suspend fun routes(): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE pointCount > 0 ORDER BY startTime DESC")
    fun observeRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM route_points WHERE routeId = :id ORDER BY seq ASC")
    suspend fun points(id: Long): List<RoutePointEntity>

    @Query("SELECT * FROM route_points WHERE routeId = :id ORDER BY seq ASC")
    fun observePoints(id: Long): Flow<List<RoutePointEntity>>

    @Query("SELECT * FROM route_points ORDER BY routeId ASC, seq ASC")
    suspend fun allPoints(): List<RoutePointEntity>

    @Query("DELETE FROM route_points WHERE routeId = :id")
    suspend fun deletePoints(id: Long)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteRouteById(id: Long)

    @Query("DELETE FROM routes")
    suspend fun clearRoutes()

    @Query("DELETE FROM route_points")
    suspend fun clearPoints()
}
