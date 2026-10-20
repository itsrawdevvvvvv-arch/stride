package com.epicmems.stride.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Date helpers. Everything is stored as an ISO `yyyy-MM-dd` string. */
object Dates {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val PRETTY: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd")

    fun today(): LocalDate = LocalDate.now()

    fun key(date: LocalDate): String = date.format(ISO)

    fun parse(key: String): LocalDate = LocalDate.parse(key, ISO)

    fun weekdayShort(date: LocalDate): String =
        date.dayOfWeek.name.take(3).uppercase(Locale.ENGLISH)

    fun prettyDate(date: LocalDate): String = date.format(PRETTY)

    fun prettyWeekday(date: LocalDate): String =
        date.dayOfWeek.name.lowercase(Locale.ENGLISH).replaceFirstChar { it.uppercase(Locale.ENGLISH) }

    /** Sunday-first week that contains [date]. */
    fun weekOf(date: LocalDate): List<LocalDate> {
        val offset = date.dayOfWeek.value % 7 // Sun -> 0, Mon -> 1, ... Sat -> 6
        val sunday = date.minusDays(offset.toLong())
        return (0..6).map { sunday.plusDays(it.toLong()) }
    }

    fun daysLeftInMonth(date: LocalDate): Int = date.lengthOfMonth() - date.dayOfMonth

    fun daysLeftInYear(date: LocalDate): Int = date.lengthOfYear() - date.dayOfYear

    fun now(): LocalDateTime = LocalDateTime.now()
}
