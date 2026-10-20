package com.epicmems.stride.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epicmems.stride.data.SettingsRepository
import com.epicmems.stride.data.StepRepository
import com.epicmems.stride.data.local.StepDay
import com.epicmems.stride.data.local.StepHour
import com.epicmems.stride.di.AppContainer
import com.epicmems.stride.domain.Patterns
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.step.StepSensorEngine
import com.epicmems.stride.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class StrideViewModel(
    private val repository: StepRepository,
    private val settings: SettingsRepository,
    private val engine: StepSensorEngine
) : ViewModel() {

    private val today: LocalDate = Dates.today()
    private val todayKey: String = Dates.key(today)

    val uiState: StateFlow<StrideUiState> = combine(
        repository.observeAll(),
        repository.observeHours(todayKey),
        settings.dailyGoal,
        settings.unit
    ) { days, hours, goal, unit ->
        buildState(days, hours, goal, unit)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StrideUiState()
    )

    init {
        viewModelScope.launch { repository.seedDemoIfNeeded() }
    }

    private fun buildState(
        days: List<StepDay>,
        hours: List<StepHour>,
        goal: Int,
        unit: String
    ): StrideUiState {
        val stats = Patterns.toStats(days)
        val last7 = Patterns.window(stats, 7, today)
        val last30 = Patterns.window(stats, 30, today)
        val monthDays = Patterns.monthWindow(stats, today)
        val todayStat = stats.firstOrNull { it.date == today }
        val hourValues = (0 until 24).map { h -> hours.firstOrNull { it.hour == h }?.steps ?: 0 }

        return StrideUiState(
            loading = false,
            today = today,
            todaySteps = todayStat?.steps ?: 0,
            goal = goal,
            unitLabel = unit,
            hasSensor = engine.hasSensor(),
            demoActive = stats.any { it.isDemo },
            last7 = last7,
            last30 = last30,
            monthDays = monthDays,
            todayHours = hourValues,
            avg7 = Patterns.average(last7.filter { it.steps > 0 }),
            avg30 = Patterns.average(last30.filter { it.steps > 0 }),
            best = stats.filter { it.steps > 0 }.maxByOrNull { it.steps },
            streak = Patterns.streak(stats, goal, today),
            goalHitRate30 = Patterns.goalHitRate(last30.filter { it.steps > 0 }),
            total30 = last30.sumOf { it.steps }
        )
    }

    fun setGoal(value: Int) = viewModelScope.launch { settings.setDailyGoal(value) }

    fun setUnit(value: String) = viewModelScope.launch { settings.setUnit(value) }

    fun regenerateDemo() = viewModelScope.launch {
        repository.clearDemo()
        repository.seedDemo()
    }

    fun clearDemo() = viewModelScope.launch { repository.clearDemo() }

    fun clearAll() = viewModelScope.launch { repository.clearAll() }
}

class StrideViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StrideViewModel(
            repository = container.stepRepository,
            settings = container.settingsRepository,
            engine = container.stepSensorEngine
        ) as T
    }
}
