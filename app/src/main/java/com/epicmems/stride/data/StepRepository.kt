package com.epicmems.stride.data

import com.epicmems.stride.data.local.StepDao
import com.epicmems.stride.data.local.StepDay
import com.epicmems.stride.data.local.StepHour
import com.epicmems.stride.util.Dates
import com.epicmems.stride.util.DemoData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

/**
 * Single source of truth for step data. Writes come from the sensor engine; reads feed the UI.
 */
class StepRepository(
    private val dao: StepDao,
    private val settings: SettingsRepository
) {

    fun observeAll(): Flow<List<StepDay>> = dao.observeAll()
    fun observeHours(date: String): Flow<List<StepHour>> = dao.observeHours(date)
    fun observeDay(date: String): Flow<StepDay?> = dao.observeDay(date)

    /** Adds [delta] steps to the current day + hour bucket. */
    suspend fun addSteps(delta: Int, at: LocalDateTime = Dates.now()) {
        if (delta <= 0) return
        val key = Dates.key(at.toLocalDate())
        val goal = settings.currentGoal()
        val existing = dao.day(key)
        dao.upsert(
            StepDay(
                date = key,
                steps = (existing?.steps ?: 0) + delta,
                goal = goal,
                isDemo = false,
                updatedAt = System.currentTimeMillis()
            )
        )
        val hourRow = dao.hours(key).firstOrNull { it.hour == at.hour }
        dao.upsertHour(StepHour(key, at.hour, (hourRow?.steps ?: 0) + delta))
    }

    suspend fun seedDemoIfNeeded() {
        if (settings.demoSeeded.first()) return
        seedDemo()
    }

    suspend fun seedDemo() {
        val goal = settings.currentGoal()
        val (days, hours) = DemoData.generate(45, goal)
        dao.upsertAll(days)
        dao.upsertHours(hours)
        settings.setDemoSeeded(true)
    }

    suspend fun clearDemo() {
        dao.clearDemoHours()
        dao.clearDemoDays()
        settings.setDemoSeeded(true)
    }

    suspend fun clearAll() {
        dao.clearAllHours()
        dao.clearAllDays()
    }
}
