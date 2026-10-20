package com.epicmems.stride.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {

    @Test
    fun `haversine returns zero for the same point`() {
        val p = LatLng(28.6139, 77.2090)
        assertEquals(0.0, Geo.haversine(p, p), 0.0001)
    }

    @Test
    fun `haversine matches a known short distance`() {
        // ~111.19 km per degree of latitude
        val a = LatLng(28.0, 77.0)
        val b = LatLng(29.0, 77.0)
        assertEquals(111_195.0, Geo.haversine(a, b), 500.0)
    }

    @Test
    fun `path length sums the segments`() {
        val points = listOf(
            LatLng(28.0, 77.0),
            LatLng(28.001, 77.0),
            LatLng(28.002, 77.0)
        )
        val length = Geo.pathLength(points)
        assertEquals(222.4, length, 5.0)
    }

    @Test
    fun `path length of a single point is zero`() {
        assertEquals(0.0, Geo.pathLength(listOf(LatLng(1.0, 1.0))), 0.0)
    }

    @Test
    fun `normalise keeps every point inside the unit square`() {
        val points = listOf(
            LatLng(28.60, 77.20),
            LatLng(28.62, 77.24),
            LatLng(28.61, 77.19)
        )
        val normalised = Geo.normalise(points)
        assertEquals(points.size, normalised.size)
        normalised.forEach { (x, y) ->
            assertTrue("x in range", x >= 0f && x <= 1f)
            assertTrue("y in range", y >= 0f && y <= 1f)
        }
    }

    @Test
    fun `normalise flips y so north points up`() {
        val south = LatLng(28.60, 77.20)
        val north = LatLng(28.62, 77.20)
        val normalised = Geo.normalise(listOf(south, north))
        // the northern point should have the smaller y value (top of the canvas)
        assertTrue(normalised[1].second < normalised[0].second)
    }

    @Test
    fun `bounds of an empty list is null`() {
        assertEquals(null, Geo.bounds(emptyList()))
        assertNotNull(Geo.bounds(listOf(LatLng(1.0, 2.0))))
    }
}
