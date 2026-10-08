package io.github.iamtoolino.dayline

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Calendar weeks use local dates; reading a chart needs only seven stored daily totals. */
object WeeklyUsage {
    fun start(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    fun days(week: LocalDate): List<LocalDate> = (0L..6L).map { start(week).plusDays(it) }
    fun duration(ms: Long): String {
        val minutes = ms.coerceAtLeast(0) / 60_000
        return when {
            ms in 1..59_999 -> "<1m"
            minutes < 60 -> "${minutes}m"
            minutes % 60 == 0L -> "${minutes / 60}h"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
}
