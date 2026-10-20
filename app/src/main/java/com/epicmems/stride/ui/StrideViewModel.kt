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
import com.epicmems.stride.domain.Export
import com.epicmems.stride.domain.Patterns
import com.epicmems.stride.domain.RecordingState
import com.epicmems.stride.domain.RouteSummary
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.track.StepSensorEngine
import com.epicmems.stride.track.TrackingState
import com.epicmems.stride.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class SettingsSnapshot(
    val goal: Int,
    val unit: String,
    val notifications: Boolean
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
        settings.dailyGoal, settings.unit, settings.notificationsEnabled
    ) { goal, unit, notifications -> SettingsSnapshot(goal, unit, notifications) }

    private val trackingFlow = combine(
        TrackingState.recording,
        TrackingState.distanceMeters,
        TrackingState.durationMillis,
        TrackingState.points
    ) { recording, distance, duration, points ->
        RecordingState(recording = recording, distanceMeters = distance, durationMillis = duration, points = points)
    }

    private val coreFlow = combine(
        stepRepository.observeAll(), stepRepository.observeHours(todayKey), settingsFlow
    ) { days, hours, snapshot -> Triple(days, hours, snapshot) }

    private val sideFlow = combine(
        routeRepository.observeRoutes(), trackingFlow, selectedMonth
    ) { routes, recording, month -> Triple(routes, recording, month) }

    val uiState: StateFlow<StrideUiState> = combine(coreFlow, sideFlow) { core, side ->
        buildState(
            days = core.first,
            hours = core.second,
            settings = core.third,
            routes = side.first,
            recording = side.second,
            month = side.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StrideUiState()
    )

    private fun buildState(
        days: List<StepDay>,
        hours: List<StepHour>,
        settings: SettingsSnapshot,
        routes: List<RouteSummary>,
        recording: RecordingState,
        month: LocalDate
    ): StrideUiState {
        val stats = Patterns.toStats(days)
        val last7 = Patterns.window(stats, 7, today)
        val last30 = Patterns.window(stats, 30, today)
        val monthDays = Patterns.monthWindow(stats, month)
        val todayStat = stats.firstOrNull { it.date == today }
        val hourValues = (0 until 24).map { h -> hours.firstOrNull { it.hour == h }?.steps ?: 0 }

        return StrideUiState(
            loading = false,
            today = today,
            todaySteps = todayStat?.steps ?: 0,
            todayDistanceMeters = todayStat?.distanceMeters ?: 0.0,
            goal = settings.goal,
            unit = settings.unit,
            hasStepSensor = engine.hasSensor(),
            hasGps = container.appContext.packageManager
                .hasSystemFeature(android.content.pm.PackageManager.FEATURE_LOCATION_GPS),
            notificationsEnabled = settings.notifications,
            last7 = last7,
            last30 = last30,
            monthDays = monthDays,
            month = month,
            todayHours = hourValues,
            avg7 = Patterns.average(last7.filter { it.steps > 0 }),
            best = stats.filter { it.steps > 0 }.maxByOrNull { it.steps },
            streak = Patterns.streak(stats, today),
            goalHitRate30 = Patterns.goalHitRate(last30.filter { it.steps > 0 }),
            completedThisMonth = Patterns.completedCount(monthDays),
            routes = routes,
            recording = recording
        )
    }

    // ---- actions ----

    fun setGoal(value: Int) = viewModelScope.launch { settings.setDailyGoal(value) }

    fun setUnit(value: String) = viewModelScope.launch { settings.setUnit(value) }

    fun setNotificationsEnabled(value: Boolean) =
        viewModelScope.launch { settings.setNotificationsEnabled(value) }

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
