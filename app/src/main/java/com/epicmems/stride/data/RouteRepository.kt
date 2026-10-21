package com.epicmems.stride.data

import com.epicmems.stride.data.local.RouteDao
import com.epicmems.stride.data.local.RouteEntity
import com.epicmems.stride.data.local.RoutePointEntity
import com.epicmems.stride.domain.Geo
import com.epicmems.stride.domain.LatLng
import com.epicmems.stride.domain.RouteSummary
import com.epicmems.stride.util.SampleData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Storage for recorded route segments. */
class RouteRepository(private val dao: RouteDao) {

    fun observeRoutes(): Flow<List<RouteSummary>> =
        dao.observeRoutes().map { list -> list.map { it.toSummary() } }

    fun observePoints(routeId: Long): Flow<List<RoutePointEntity>> = dao.observePoints(routeId)

    suspend fun routes(): List<RouteEntity> = dao.routes()

    suspend fun points(routeId: Long): List<RoutePointEntity> = dao.points(routeId)

    suspend fun allPoints(): List<RoutePointEntity> = dao.allPoints()

    /** Opens a new segment. [estimated] marks a path reconstructed from motion sensors. */
    suspend fun startRoute(startTime: Long, estimated: Boolean = false): Long =
        dao.insertRoute(
            RouteEntity(startTime = startTime, endTime = startTime, estimated = estimated)
        )

    suspend fun appendPoints(routeId: Long, points: List<RoutePointEntity>) =
        dao.insertPoints(points)

    suspend fun finishRoute(
        routeId: Long,
        endTime: Long,
        distanceMeters: Double,
        pointCount: Int
    ) {
        val route = dao.route(routeId) ?: return
        dao.updateRoute(
            route.copy(
                endTime = endTime,
                distanceMeters = distanceMeters,
                durationMillis = (endTime - route.startTime).coerceAtLeast(0),
                pointCount = pointCount
            )
        )
    }

    /** Drops a segment that turned out to be too short to be a walk. */
    suspend fun discardRoute(routeId: Long) {
        dao.deletePoints(routeId)
        dao.deleteRouteById(routeId)
    }

    suspend fun deleteRoute(id: Long) {
        dao.deletePoints(id)
        dao.deleteRouteById(id)
    }

    suspend fun clearAll() {
        dao.clearPoints()
        dao.clearRoutes()
    }

    suspend fun loadSampleRoutes() {
        val (routes, points) = SampleData.routes()
        routes.forEachIndexed { index, route ->
            val id = dao.insertRoute(route.copy(id = 0))
            dao.insertPoints(points.filter { it.routeId == route.id }.map { it.copy(routeId = id) })
            if (index < 0) return@forEachIndexed
        }
    }

    private fun RouteEntity.toSummary() = RouteSummary(
        id = id,
        startTime = startTime,
        endTime = endTime,
        distanceMeters = distanceMeters,
        durationMillis = durationMillis,
        pointCount = pointCount,
        estimated = estimated
    )
}
