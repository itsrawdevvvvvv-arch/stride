package com.epicmems.stride.data

import com.epicmems.stride.data.local.StepDao
import com.epicmems.stride.data.local.StepDay
import com.epicmems.stride.data.local.StepHour
import com.epicmems.stride.util.Dates
import com.epicmems.stride.util.SampleData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Single source of truth for step data. Writes come from the tracking service;
 * reads feed the UI, the widget and exports.
 */
class StepRepository(
    private val dao: StepDao,
    private val settings: SettingsRepository
) {

    fun observeAll(): Flow<List<StepDay>> = dao.observeAll()
    fun observeDay(date: String): Flow<StepDay?> = dao.observeDay(date)
    fun observeHours(date: String): Flow<List<StepHour>> = dao.observeHours(date)

    suspend fun allDays(): List<StepDay> = dao.all()
    suspend fun daysBetween(from: LocalDate, to: LocalDate): List<StepDay> =
        dao.daysBetween(Dates.key(from), Dates.key(to))

    /** Adds [delta] steps to the current day and hour bucket. */
    suspend fun addSteps(delta: Int, at: LocalDateTime = Dates.now()) {
        if (delta <= 0) return
        val key = Dates.key(at.toLocalDate())
        val goal = settings.currentGoal()
        val existing = dao.day(key)
        val steps = (existing?.steps ?: 0) + delta
        dao.upsert(
            StepDay(
                date = key,
                steps = steps,
                goal = goal,
                distanceMeters = steps * METERS_PER_STEP,
                marked = existing?.marked ?: false,
                updatedAt = System.currentTimeMillis()
            )
        )
        val hourRow = dao.hours(key).firstOrNull { it.hour == at.hour }
        dao.upsertHour(StepHour(key, at.hour, (hourRow?.steps ?: 0) + delta))
    }

    /** Tapping a day in the calendar toggles its manual "completed" mark. */
    suspend fun toggleMarked(date: LocalDate) {
        val key = Dates.key(date)
        val existing = dao.day(key)
        if (existing == null) {
            dao.upsert(StepDay(key, steps = 0, goal = settings.currentGoal(), marked = true))
        } else {
            dao.setMarked(key, !existing.marked)
        }
    }

    /** Loads a realistic sample history so the charts have something to show. */
    suspend fun loadSampleData(days: Int = 30) {
        val goal = settings.currentGoal()
        val (dayRows, hourRows) = SampleData.history(days, goal)
        dao.upsertAll(dayRows)
        dao.upsertHours(hourRows)
    }

    suspend fun clearAll() {
        dao.clearHours()
        dao.clearDays()
    }

    companion object {
        /** Average stride length used to turn steps into an approximate distance. */
        const val METERS_PER_STEP = 0.75
    }
}
