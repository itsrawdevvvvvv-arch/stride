package com.epicmems.stride.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epicmems.stride.data.RouteRepository
import com.epicmems.stride.data.SettingsRepository
import com.epicmems.stride.data.StepRepository
import com.epicmems.stride.data.local.RoutePointEntity
import com.epicmems.stride.data.local.StepDay
import com.epicmems.stride.data.local.StepHour
import com.epicmems.stride.di.AppContainer
import com.epicmems.stride.domain.DayStat
import com.epicmems.stride.domain.Export
import com.epicmems.stride.domain.Patterns
import com.epicmems.stride.domain.RecordingState
import com.epicmems.stride.domain.RouteSummary
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.track.StepSensorEngine
import com.epicmems.stride.track.StepTrackingService
import com.epicmems.stride.track.TrackingState
import com.epicmems.stride.track.TrackingWatchdogWorker
import com.epicmems.stride.util.BatteryOptimization
import com.epicmems.stride.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class SettingsSnapshot(
    val goal: Int,
    val unit: String,
    val notifications: Boolean,
    val trackingEnabled: Boolean
)

/**
 * Everything derived from the stored days. Kept in its own snapshot so that
 * [StrideViewModel.statsFlow] can suppress re-emission with `distinctUntilChanged` — that
 * keeps the *same list instances* flowing while a route is recording, which lets Compose skip
 * the charts and stat cards entirely instead of rebuilding them every second.
 */
private data class StatsSnapshot(
    val month: LocalDate,
    val last7: List<DayStat>,
    val last30: List<DayStat>,
    val monthDays: List<DayStat>,
    val todayStat: DayStat?,
    val avg7: Int,
    val best: DayStat?,
    val streak: Int,
    val goalHitRate30: Int,
    val completedThisMonth: Int
)

/** The parts that legitimately change every second while recording. */
private data class VolatileSnapshot(
    val todayHours: List<Int>,
    val recording: RecordingState,
    val routes: List<RouteSummary>
)

class StrideViewModel(private val container: AppContainer) : ViewModel() {

    private val stepRepository: StepRepository = container.stepRepository
    private val routeRepository: RouteRepository = container.routeRepository
    private val settings: SettingsRepository = container.settingsRepository
    private val engine: StepSensorEngine = container.stepSensorEngine

    private val today: LocalDate = Dates.today()
    private val todayKey: String = Dates.key(today)
    private val selectedMonth = MutableStateFlow(today.withDayOfMonth(1))

    private val settingsFlow = combine(
        settings.dailyGoal, settings.unit, settings.notificationsEnabled, settings.trackingEnabled
    ) { goal, unit, notifications, tracking ->
        SettingsSnapshot(goal, unit, notifications, tracking)
    }

    /** Refreshed from the Settings screen — reading it on every emission would hit the system service. */
    private val batteryExempt = MutableStateFlow(BatteryOptimization.isExempt(container.appContext))

    private val trackingFlow = combine(
        TrackingState.recording,
        TrackingState.distanceMeters,
        TrackingState.durationMillis,
        TrackingState.points,
        TrackingState.status
    ) { recording, distance, duration, points, status ->
        RecordingState(
            recording = recording,
            distanceMeters = distance,
            durationMillis = duration,
            points = points,
            status = status
        )
    }

    private val statsFlow = combine(
        stepRepository.observeAll(), settingsFlow, selectedMonth
    ) { days, snapshot, month -> buildStats(days, snapshot, month) }
        .distinctUntilChanged()

    private val volatileFlow = combine(
        stepRepository.observeHours(todayKey), trackingFlow, routeRepository.observeRoutes()
    ) { hours, recording, routes ->
        VolatileSnapshot(
            todayHours = (0 until 24).map { h -> hours.firstOrNull { it.hour == h }?.steps ?: 0 },
            recording = recording,
            routes = routes
        )
    }

    val uiState: StateFlow<StrideUiState> = combine(
        statsFlow, volatileFlow, settingsFlow, batteryExempt
    ) { stats, volatile, snapshot, exempt ->
        StrideUiState(
            loading = false,
            today = today,
            todaySteps = stats.todayStat?.steps ?: 0,
            todayDistanceMeters = stats.todayStat?.distanceMeters ?: 0.0,
            goal = snapshot.goal,
            unit = snapshot.unit,
            hasStepSensor = engine.hasSensor(),
            hasGps = container.appContext.packageManager
                .hasSystemFeature(android.content.pm.PackageManager.FEATURE_LOCATION_GPS),
            notificationsEnabled = snapshot.notifications,
            last7 = stats.last7,
            last30 = stats.last30,
            monthDays = stats.monthDays,
            month = stats.month,
            todayHours = volatile.todayHours,
            avg7 = stats.avg7,
            best = stats.best,
            streak = stats.streak,
            goalHitRate30 = stats.goalHitRate30,
            completedThisMonth = stats.completedThisMonth,
            routes = volatile.routes,
            recording = volatile.recording,
            trackingEnabled = snapshot.trackingEnabled,
            batteryExempt = exempt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StrideUiState()
    )

    private fun buildStats(
        days: List<StepDay>,
        settings: SettingsSnapshot,
        month: LocalDate
    ): StatsSnapshot {
        val stats = Patterns.toStats(days)
        val last7 = Patterns.window(stats, 7, today)
        val last30 = Patterns.window(stats, 30, today)
        val monthDays = Patterns.monthWindow(stats, month)
        return StatsSnapshot(
            month = month,
            last7 = last7,
            last30 = last30,
            monthDays = monthDays,
            todayStat = stats.firstOrNull { it.date == today },
            avg7 = Patterns.average(last7.filter { it.steps > 0 }),
            best = stats.filter { it.steps > 0 }.maxByOrNull { it.steps },
            streak = Patterns.streak(stats, today),
            goalHitRate30 = Patterns.goalHitRate(last30.filter { it.steps > 0 }),
            completedThisMonth = Patterns.completedCount(monthDays)
        )
    }

    // ---- actions ----

    fun setGoal(value: Int) = viewModelScope.launch { settings.setDailyGoal(value) }

    fun setUnit(value: String) = viewModelScope.launch { settings.setUnit(value) }

    fun setNotificationsEnabled(value: Boolean) =
        viewModelScope.launch { settings.setNotificationsEnabled(value) }

    /** Turns always-on tracking on or off: the service plus its watchdog. */
    fun setTrackingEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setTrackingEnabled(enabled)
        val context = container.appContext
        if (enabled) {
            TrackingWatchdogWorker.schedule(context)
            StepTrackingService.start(context)
        } else {
            TrackingWatchdogWorker.cancel(context)
            StepTrackingService.stop(context)
        }
    }

    /** Call when the Settings screen becomes visible, to pick up a battery-exemption change. */
    fun refreshBatteryState() {
        batteryExempt.value = BatteryOptimization.isExempt(container.appContext)
    }

    fun toggleMarked(date: LocalDate) = viewModelScope.launch { stepRepository.toggleMarked(date) }

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun loadSampleData() = viewModelScope.launch {
        stepRepository.loadSampleData(30)
        routeRepository.loadSampleRoutes()
    }

    fun clearAllData() = viewModelScope.launch {
        stepRepository.clearAll()
        routeRepository.clearAll()
    }

    fun deleteRoute(id: Long) = viewModelScope.launch { routeRepository.deleteRoute(id) }

    fun routePoints(routeId: Long): Flow<List<RoutePointEntity>> = routeRepository.observePoints(routeId)

    // ---- exports (suspend so the caller can write to the chosen document) ----

    suspend fun exportCsv(): String = Export.stepsCsv(stepRepository.allDays())

    suspend fun exportJson(): String =
        Export.json(stepRepository.allDays(), routeRepository.routes(), routeRepository.allPoints())

    suspend fun exportGpx(routeId: Long): String {
        val route = routeRepository.routes().firstOrNull { it.id == routeId }
            ?: return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\" creator=\"Stride\"/>\n"
        return Export.gpx(route, routeRepository.points(routeId))
    }
}

class StrideViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StrideViewModel(container) as T
    }
}
