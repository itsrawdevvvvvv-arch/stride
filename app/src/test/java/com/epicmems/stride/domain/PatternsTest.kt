package com.epicmems.stride.domain

import com.epicmems.stride.data.local.StepDay
import com.epicmems.stride.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PatternsTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 20)

    private fun day(date: LocalDate, steps: Int, goal: Int = 8000, marked: Boolean = false) =
        StepDay(date = Dates.key(date), steps = steps, goal = goal, marked = marked)

    @Test
    fun `window is zero filled and ends on the requested day`() {
        val stats = Patterns.toStats(
            listOf(day(today, 5000), day(today.minusDays(2), 9000))
        )
        val window = Patterns.window(stats, 3, today)

        assertEquals(3, window.size)
        assertEquals(today.minusDays(2), window.first().date)
        assertEquals(today, window.last().date)
        // the day with no rows is filled with zeroes rather than dropped
        assertEquals(0, window[1].steps)
    }

    @Test
    fun `average ignores nothing when caller pre-filters`() {
        val stats = Patterns.toStats(
            listOf(day(today, 4000), day(today.minusDays(1), 6000))
        )
        assertEquals(5000, Patterns.average(stats))
        assertEquals(0, Patterns.average(emptyList()))
    }

    @Test
    fun `goal hit rate counts days at or above the goal`() {
        val stats = Patterns.toStats(
            listOf(
                day(today, 8000),
                day(today.minusDays(1), 7999),
                day(today.minusDays(2), 12000)
            )
        )
        assertEquals(66, Patterns.goalHitRate(stats))
    }

    @Test
    fun `streak counts consecutive completed days ending today`() {
        val stats = Patterns.toStats(
            listOf(
                day(today, 9000),
                day(today.minusDays(1), 8500),
                day(today.minusDays(2), 3000), // breaks the streak
                day(today.minusDays(3), 10000)
            )
        )
        assertEquals(2, Patterns.streak(stats, today))
    }

    @Test
    fun `streak falls back to yesterday when today is not done yet`() {
        val stats = Patterns.toStats(
            listOf(
                day(today, 100), // today not completed
                day(today.minusDays(1), 9000),
                day(today.minusDays(2), 9500)
            )
        )
        assertEquals(2, Patterns.streak(stats, today))
    }

    @Test
    fun `a manually marked day counts as completed even without steps`() {
        val stats = Patterns.toStats(
            listOf(
                day(today, 0, marked = true),
                day(today.minusDays(1), 9000)
            )
        )
        assertTrue(stats.first { it.date == today }.completed)
        assertFalse(stats.first { it.date == today }.goalHit)
        assertEquals(2, Patterns.streak(stats, today))
    }

    @Test
    fun `progress is clamped between zero and one`() {
        val low = Patterns.toStats(listOf(day(today, 100))).first()
        val high = Patterns.toStats(listOf(day(today, 99999))).first()
        assertEquals(0.0125f, low.progress, 0.001f)
        assertEquals(1f, high.progress, 0.0001f)
    }
}
