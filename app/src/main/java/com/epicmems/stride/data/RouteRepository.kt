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

/** Storage for recorded GPS routes. */
class RouteRepository(private val dao: RouteDao) {

    fun observeRoutes(): Flow<List<RouteSummary>> =
        dao.observeRoutes().map { list -> list.map { it.toSummary() } }

    fun observePoints(routeId: Long): Flow<List<RoutePointEntity>> = dao.observePoints(routeId)

    suspend fun routes(): List<RouteSummary> = dao.routes().map { it.toSummary() }

    suspend fun points(routeId: Long): List<RoutePointEntity> = dao.points(routeId)

    suspend fun allPoints(): List<RoutePointEntity> = dao.allPoints()

    suspend fun startRoute(startTime: Long): Long =
        dao.insertRoute(RouteEntity(startTime = startTime, endTime = startTime))

    suspend fun appendPoints(points: List<RoutePointEntity>) {
        if (points.isNotEmpty()) dao.insertPoints(points)
    }

    suspend fun finishRoute(routeId: Long, endTime: Long, distanceMeters: Double, pointCount: Int) {
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

    suspend fun deleteRoute(id: Long) {
        dao.deletePoints(id)
        dao.deleteRouteById(id)
    }

    /** Loads a few sample routes (they draw recognisable shapes) for testing the UI. */
    suspend fun loadSampleRoutes() {
        SampleData.routes().forEach { (route, coords) ->
            val id = dao.insertRoute(route)
            dao.insertPoints(
                coords.mapIndexed { index, p ->
                    RoutePointEntity(
                        routeId = id,
                        seq = index,
                        lat = p.lat,
                        lng = p.lng,
                        time = route.startTime + index * 4_000L
                    )
                }
            )
            val distance = Geo.pathLength(coords)
            dao.updateRoute(
                route.copy(
                    id = id,
                    endTime = route.startTime + coords.size * 4_000L,
                    distanceMeters = distance,
                    durationMillis = coords.size * 4_000L,
                    pointCount = coords.size
                )
            )
        }
    }

    suspend fun clearAll() {
        dao.clearPoints()
        dao.clearRoutes()
    }

    private fun RouteEntity.toSummary() = RouteSummary(
        id = id,
        startTime = startTime,
        endTime = endTime,
        distanceMeters = distanceMeters,
        durationMillis = durationMillis,
        pointCount = pointCount
    )
}
