package com.epicmems.stride.domain

import com.epicmems.stride.data.local.StepDay
import com.epicmems.stride.util.Dates
import java.time.LocalDate

/**
 * Pure functions that turn stored rows into the patterns the app visualises.
 * No Android dependencies, so this is trivially unit-testable.
 */
object Patterns {

    fun toStats(days: List<StepDay>): List<DayStat> =
        days.map { DayStat(Dates.parse(it.date), it.steps, it.goal, it.distanceMeters, it.marked) }
            .sortedBy { it.date }

    /** A rolling window ending on [end], zero-filled so charts keep their shape. */
    fun window(stats: List<DayStat>, length: Int, end: LocalDate): List<DayStat> {
        val byDate = stats.associateBy { it.date }
        val goal = stats.lastOrNull()?.goal ?: 8000
        return (length - 1 downTo 0).map { back ->
            val date = end.minusDays(back.toLong())
            byDate[date] ?: DayStat(date, 0, goal, 0.0, false)
        }
    }

    /** Every day of [month], zero-filled. */
    fun monthWindow(stats: List<DayStat>, month: LocalDate): List<DayStat> {
        val byDate = stats.associateBy { it.date }
        val goal = stats.lastOrNull()?.goal ?: 8000
        return Dates.daysInMonth(month).map { date ->
            byDate[date] ?: DayStat(date, 0, goal, 0.0, false)
        }
    }

    fun average(stats: List<DayStat>): Int =
        if (stats.isEmpty()) 0 else stats.sumOf { it.steps } / stats.size

    /** Consecutive completed days ending today (or yesterday if today isn't done yet). */
    fun streak(stats: List<DayStat>, end: LocalDate): Int {
        val byDate = stats.associateBy { it.date }
        var cursor = end
        val today = byDate[cursor]
        if (today == null || !today.completed) cursor = cursor.minusDays(1)
        var count = 0
        while (true) {
            val stat = byDate[cursor] ?: break
            if (stat.completed) {
                count++
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }
        return count
    }

    fun goalHitRate(stats: List<DayStat>): Int =
        if (stats.isEmpty()) 0 else (stats.count { it.goalHit } * 100) / stats.size

    fun completedCount(stats: List<DayStat>): Int = stats.count { it.completed }
}
