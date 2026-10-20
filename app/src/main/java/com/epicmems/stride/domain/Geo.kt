package com.epicmems.stride.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Geometry for GPS routes: distance and normalisation for drawing. */
object Geo {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Great-circle distance between two points, in metres. */
    fun haversine(a: LatLng, b: LatLng): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))
    }

    /** Total length of a polyline, in metres. */
    fun pathLength(points: List<LatLng>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) total += haversine(points[i - 1], points[i])
        return total
    }

    /** Axis-aligned bounds of a polyline: (minLat, minLng, maxLat, maxLng). */
    fun bounds(points: List<LatLng>): DoubleArray? {
        if (points.isEmpty()) return null
        var minLat = points[0].lat
        var maxLat = points[0].lat
        var minLng = points[0].lng
        var maxLng = points[0].lng
        for (p in points) {
            minLat = min(minLat, p.lat); maxLat = max(maxLat, p.lat)
            minLng = min(minLng, p.lng); maxLng = max(maxLng, p.lng)
        }
        return doubleArrayOf(minLat, minLng, maxLat, maxLng)
    }

    /**
     * Projects a polyline into the unit square (0..1, 0..1) with y flipped so that
     * north is up. Preserves aspect ratio and centres the drawing — this is what lets
     * the app render a route as a "drawing".
     */
    fun normalise(points: List<LatLng>): List<Pair<Float, Float>> {
        if (points.isEmpty()) return emptyList()
        // A single point has no shape to fit — park it in the middle.
        if (points.size == 1) return listOf(0.5f to 0.5f)
        val b = bounds(points) ?: return emptyList()
        val minLat = b[0]; val minLng = b[1]; val maxLat = b[2]; val maxLng = b[3]

        val latSpan = max(maxLat - minLat, 1e-6)
        val lngSpan = max(maxLng - minLng, 1e-6)
        // Longitude degrees are shorter than latitude degrees away from the equator.
        val midLat = Math.toRadians((minLat + maxLat) / 2)
        val aspect = lngSpan * cos(midLat) / latSpan
        val scale = if (aspect > 1.0) 1.0 / aspect else 1.0
        val w = if (aspect > 1.0) 1.0 else aspect
        val offX = (1.0 - w) / 2.0
        val offY = (1.0 - scale) / 2.0

        return points.map { p ->
            val x = offX + ((p.lng - minLng) / lngSpan) * w
            val y = offY + (1.0 - (p.lat - minLat) / latSpan) * scale
            x.toFloat() to y.toFloat()
        }
    }
}
