package com.epicmems.stride.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Date helpers. Days are keyed by an ISO `yyyy-MM-dd` string everywhere. */
object Dates {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val PRETTY: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd")

    fun today(): LocalDate = LocalDate.now()

    fun now(): LocalDateTime = LocalDateTime.now()

    fun key(date: LocalDate): String = date.format(ISO)

    fun parse(key: String): LocalDate = LocalDate.parse(key, ISO)

    fun pretty(date: LocalDate): String = date.format(PRETTY)

    fun weekdayShort(date: LocalDate): String =
        date.dayOfWeek.name.take(3).uppercase(Locale.ENGLISH)

    fun weekdayLong(date: LocalDate): String =
        date.dayOfWeek.name.lowercase(Locale.ENGLISH).replaceFirstChar { it.uppercase(Locale.ENGLISH) }

    fun monthName(date: LocalDate): String =
        date.month.name.lowercase(Locale.ENGLISH).replaceFirstChar { it.uppercase(Locale.ENGLISH) }

    /** Sunday-first week containing [date]. */
    fun weekOf(date: LocalDate): List<LocalDate> {
        val offset = date.dayOfWeek.value % 7
        val sunday = date.minusDays(offset.toLong())
        return (0..6).map { sunday.plusDays(it.toLong()) }
    }

    /** The last [n] days ending today, oldest first. */
    fun lastNDays(n: Int, end: LocalDate = today()): List<LocalDate> =
        (n - 1 downTo 0).map { end.minusDays(it.toLong()) }

    /** Every calendar day of [month]. */
    fun daysInMonth(month: LocalDate): List<LocalDate> =
        (1..month.lengthOfMonth()).map { month.withDayOfMonth(it) }

    fun daysLeftInMonth(date: LocalDate): Int = date.lengthOfMonth() - date.dayOfMonth

    fun daysLeftInYear(date: LocalDate): Int = date.lengthOfYear() - date.dayOfYear
}
