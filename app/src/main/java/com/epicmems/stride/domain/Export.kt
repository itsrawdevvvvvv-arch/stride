package com.epicmems.stride.domain

import com.epicmems.stride.data.local.RoutePointEntity
import com.epicmems.stride.data.local.StepDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builders for the data-export formats. Everything here is pure string building so it
 * can be unit-tested without Android.
 */
object Export {

    private fun isoDay(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun isoTime(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }

    /** Daily steps as CSV. */
    fun stepsCsv(days: List<StepDay>): String {
        val sb = StringBuilder()
        sb.append("date,steps,goal,distance_m,marked\n")
        days.sortedBy { it.date }.forEach { d ->
            sb.append(d.date).append(',')
                .append(d.steps).append(',')
                .append(d.goal).append(',')
                .append(String.format(Locale.US, "%.1f", d.distanceMeters)).append(',')
                .append(if (d.marked) 1 else 0).append('\n')
        }
        return sb.toString()
    }

    /** Everything as JSON — days, routes and route points. */
    fun json(days: List<StepDay>, routes: List<RouteSummary>, points: List<RoutePointEntity>): String {
        val sb = StringBuilder()
        sb.append("{\n  \"app\": \"Stride\",\n  \"exportedAt\": \"").append(isoTime().format(Date())).append("\",\n")
        sb.append("  \"days\": [\n")
        days.sortedBy { it.date }.forEachIndexed { i, d ->
            sb.append("    { \"date\": \"").append(d.date)
                .append("\", \"steps\": ").append(d.steps)
                .append(", \"goal\": ").append(d.goal)
                .append(", \"distanceMeters\": ").append(String.format(Locale.US, "%.1f", d.distanceMeters))
                .append(", \"marked\": ").append(d.marked)
                .append(" }")
            if (i < days.size - 1) sb.append(',')
            sb.append('\n')
        }
        sb.append("  ],\n  \"routes\": [\n")
        routes.forEachIndexed { i, r ->
            sb.append("    { \"id\": ").append(r.id)
                .append(", \"startTime\": ").append(r.startTime)
                .append(", \"endTime\": ").append(r.endTime)
                .append(", \"distanceMeters\": ").append(String.format(Locale.US, "%.1f", r.distanceMeters))
                .append(", \"durationMillis\": ").append(r.durationMillis)
                .append(", \"points\": [")
            val pts = points.filter { it.routeId == r.id }.sortedBy { it.seq }
            pts.forEachIndexed { j, p ->
                sb.append("[").append(String.format(Locale.US, "%.6f", p.lat))
                    .append(",").append(String.format(Locale.US, "%.6f", p.lng)).append("]")
                if (j < pts.size - 1) sb.append(',')
            }
            sb.append("] }")
            if (i < routes.size - 1) sb.append(',')
            sb.append('\n')
        }
        sb.append("  ]\n}\n")
        return sb.toString()
    }

    /** A single route as GPX 1.1. */
    fun gpx(route: RouteSummary, points: List<RoutePointEntity>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"Stride\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <trk>\n    <name>Stride route ").append(isoDay().format(Date(route.startTime))).append("</name>\n    <trkseg>\n")
        points.sortedBy { it.seq }.forEach { p ->
            sb.append("      <trkpt lat=\"").append(String.format(Locale.US, "%.6f", p.lat))
                .append("\" lon=\"").append(String.format(Locale.US, "%.6f", p.lng)).append("\">\n")
            sb.append("        <time>").append(isoTime().format(Date(p.time))).append("</time>\n")
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n  </trk>\n</gpx>\n")
        return sb.toString()
    }
}
