package com.epicmems.stride.util

import java.util.Locale

/** Small formatting helpers shared by the UI, the widget and exports. */
object Format {

    /** 7842 -> "7,842" (Indian grouping). */
    fun steps(value: Int): String = String.format(Locale("en", "IN"), "%,d", value)

    /** Metres to a short distance string in the given unit. */
    fun distance(meters: Double, unit: String): String {
        val km = meters / 1000.0
        return if (unit == "mi") {
            String.format(Locale.US, "%.2f mi", km * 0.621371)
        } else {
            String.format(Locale.US, "%.2f km", km)
        }
    }

    /** Milliseconds to "h:mm:ss" (or "m:ss" under an hour). */
    fun duration(millis: Long): String {
        val totalSeconds = millis / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun percent(value: Float): String = "${(value * 100).toInt()}%"
}
