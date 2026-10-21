package com.epicmems.stride.domain

import androidx.compose.runtime.Immutable
import com.epicmems.stride.util.Dates
import java.time.LocalDate

/** A point on a recorded route. */
@Immutable
data class LatLng(val lat: Double, val lng: Double)

/** What the route recorder is currently doing — surfaced in the UI so failures are visible. */
enum class RecordingStatus {
    IDLE,
    /** Registered with a provider, no fix yet. */
    ACQUIRING,
    /** Receiving fixes and drawing. */
    TRACKING,
    /** Location services are switched off on the device. */
    LOCATION_OFF,
    /** The app was not granted location permission. */
    PERMISSION_MISSING,
    /** No usable location provider on this device. */
    UNAVAILABLE
}

/**
 * One day of step data, ready for the UI.
 *
 * Marked [Immutable] so Compose can skip recomposing composables that receive it when the
 * instance is equal — this is what keeps the screen calm while live data streams in.
 */
@Immutable
data class DayStat(
    val date: LocalDate,
    val steps: Int,
    val goal: Int,
    val distanceMeters: Double,
    val marked: Boolean
) {
    val goalHit: Boolean get() = steps >= goal

    val progress: Float
        get() = if (goal <= 0) 0f else (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)

    /** A day counts as "completed" when the goal is hit or the user marked it by hand. */
    val completed: Boolean get() = goalHit || marked
}

/** Summary of a recorded route segment. */
@Immutable
data class RouteSummary(
    val id: Long,
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val durationMillis: Long,
    val pointCount: Int,
    /** True when the path was estimated from motion sensors because GPS was unavailable. */
    val estimated: Boolean = false
)

/** Live state of the route recorder, exposed to the UI. */
@Immutable
data class RecordingState(
    val recording: Boolean = false,
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0L,
    val points: List<LatLng> = emptyList(),
    val status: RecordingStatus = RecordingStatus.IDLE,
    /** True while the path is being reconstructed from motion sensors (indoors, no GPS). */
    val estimated: Boolean = false
)

/** Everything the screens render. */
@Immutable
data class StrideUiState(
    val loading: Boolean = true,
    val today: LocalDate = Dates.today(),
    val todaySteps: Int = 0,
    val todayDistanceMeters: Double = 0.0,
    val goal: Int = 8000,
    val unit: String = "km",
    val hasStepSensor: Boolean = true,
    val hasGps: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val last7: List<DayStat> = emptyList(),
    val last30: List<DayStat> = emptyList(),
    val monthDays: List<DayStat> = emptyList(),
    val month: LocalDate = Dates.today().withDayOfMonth(1),
    val todayHours: List<Int> = List(24) { 0 },
    val avg7: Int = 0,
    val best: DayStat? = null,
    val streak: Int = 0,
    val goalHitRate30: Int = 0,
    val completedThisMonth: Int = 0,
    val routes: List<RouteSummary> = emptyList(),
    val recording: RecordingState = RecordingState(),
    /** Always-on tracking preference. */
    val trackingEnabled: Boolean = true,
    /** Whether the system has exempted Stride from battery optimisation. */
    val batteryExempt: Boolean = false,
    /** Routes are recorded automatically while you move. */
    val autoRecordRoutes: Boolean = true,
    val profileName: String = "Stride",
    val profileColorIndex: Int = 0
)
