package com.epicmems.stride.domain

import com.epicmems.stride.data.local.StepDay
import com.epicmems.stride.util.Dates
import java.time.LocalDate

/**
 * Pure functions that turn stored step rows into the "patterns" the app visualises:
 * rolling windows, averages, streaks and goal-hit rates. No Android dependencies, so
 * these are trivially unit-testable.
 */
object Patterns {

    fun toStats(days: List<StepDay>): List<DayStat> =
        days.map { DayStat(Dates.parse(it.date), it.steps, it.goal, it.isDemo) }
            .sortedBy { it.date }

    /** A rolling window ending on [end], with zero-filled gaps so charts keep their shape. */
    fun window(stats: List<DayStat>, length: Int, end: LocalDate): List<DayStat> {
        val byDate = stats.associateBy { it.date }
        val goal = stats.lastOrNull()?.goal ?: 8000
        return (length - 1 downTo 0).map { back ->
            val date = end.minusDays(back.toLong())
            byDate[date] ?: DayStat(date, 0, goal, false)
        }
    }

    /** Every day of [month]'s calendar, zero-filled. */
    fun monthWindow(stats: List<DayStat>, month: LocalDate): List<DayStat> {
        val byDate = stats.associateBy { it.date }
        val goal = stats.lastOrNull()?.goal ?: 8000
        return (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.withDayOfMonth(dayOfMonth)
            byDate[date] ?: DayStat(date, 0, goal, false)
        }
    }

    fun average(stats: List<DayStat>): Int =
        if (stats.isEmpty()) 0 else stats.sumOf { it.steps } / stats.size

    /**
     * Consecutive days ending today (or yesterday, if today's goal is not hit yet)
     * on which the goal was reached.
     */
    fun streak(stats: List<DayStat>, goal: Int, end: LocalDate): Int {
        val byDate = stats.associateBy { it.date }
        var cursor = end
        val todayStat = byDate[cursor]
        if (todayStat == null || todayStat.steps < goal) {
            cursor = cursor.minusDays(1)
        }
        var count = 0
        while (true) {
            val stat = byDate[cursor] ?: break
            if (stat.steps >= goal) {
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
}
