package io.github.iamtoolino.dayline

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyUsageTest {
    @Test fun mondayAndSundayBelongToTheSameWeek() {
        val monday = LocalDate.parse("2026-10-05")
        assertEquals(monday, WeeklyUsage.start(monday))
        assertEquals(monday, WeeklyUsage.start(LocalDate.parse("2026-10-11")))
        assertEquals(monday.plusWeeks(1), WeeklyUsage.start(LocalDate.parse("2026-10-12")))
    }
    @Test fun yearBoundaryAndLeapDayHaveSevenConsecutiveDates() {
        assertEquals((29L..31L).map { LocalDate.of(2025, 12, it.toInt()) } +
            (1L..4L).map { LocalDate.of(2026, 1, it.toInt()) },
            WeeklyUsage.days(LocalDate.parse("2026-01-01")))
        assertEquals(LocalDate.parse("2024-02-29"), WeeklyUsage.days(LocalDate.parse("2024-02-29"))[3])
    }
    @Test fun totalsAreReadableWithoutWrappingClockHours() {
        assertEquals("0m", WeeklyUsage.duration(0))
        assertEquals("<1m", WeeklyUsage.duration(59_999))
        assertEquals("1m", WeeklyUsage.duration(60_000))
        assertEquals("1h", WeeklyUsage.duration(3_600_000))
        assertEquals("25h 30m", WeeklyUsage.duration(91_800_000))
    }
}
