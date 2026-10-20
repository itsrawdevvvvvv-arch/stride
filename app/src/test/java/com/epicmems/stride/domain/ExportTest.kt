package com.epicmems.stride.domain

import com.epicmems.stride.data.local.RoutePointEntity
import com.epicmems.stride.data.local.StepDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTest {

    private val days = listOf(
        StepDay(date = "2026-10-19", steps = 9000, goal = 8000, distanceMeters = 6750.0, marked = false),
        StepDay(date = "2026-10-20", steps = 4200, goal = 8000, distanceMeters = 3150.0, marked = true)
    )

    private val route = RouteSummary(
        id = 1L,
        startTime = 1_700_000_000_000L,
        endTime = 1_700_000_600_000L,
        distanceMeters = 2500.0,
        durationMillis = 600_000L,
        pointCount = 2
    )

    private val points = listOf(
        RoutePointEntity(routeId = 1L, seq = 0, lat = 28.6139, lng = 77.2090, time = 1_700_000_000_000L),
        RoutePointEntity(routeId = 1L, seq = 1, lat = 28.6149, lng = 77.2100, time = 1_700_000_004_000L)
    )

    @Test
    fun `csv has a header and one row per day`() {
        val csv = Export.stepsCsv(days)
        val lines = csv.trim().split("\n")
        assertEquals("date,steps,goal,distance_m,marked", lines[0])
        assertEquals(3, lines.size)
        assertTrue(lines[1].startsWith("2026-10-19,9000,8000,6750.0,0"))
        assertTrue(lines[2].endsWith(",1"))
    }

    @Test
    fun `csv is ordered by date`() {
        val reversed = Export.stepsCsv(days.reversed())
        val lines = reversed.trim().split("\n")
        assertTrue(lines[1].startsWith("2026-10-19"))
        assertTrue(lines[2].startsWith("2026-10-20"))
    }

    @Test
    fun `json contains days and routes`() {
        val json = Export.json(days, listOf(route), points)
        assertTrue(json.contains("\"app\": \"Stride\""))
        assertTrue(json.contains("\"days\""))
        assertTrue(json.contains("\"routes\""))
        assertTrue(json.contains("2026-10-20"))
        assertTrue(json.contains("\"marked\": true"))
        assertTrue(json.contains("[28.613900,77.209000]"))
    }

    @Test
    fun `gpx contains a track point per coordinate`() {
        val gpx = Export.gpx(route, points)
        assertTrue(gpx.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(gpx.contains("<gpx version=\"1.1\" creator=\"Stride\""))
        assertEquals(2, Regex("<trkpt").findAll(gpx).count())
        assertTrue(gpx.contains("lat=\"28.613900\""))
        assertTrue(gpx.contains("lon=\"77.210000\""))
    }
}
