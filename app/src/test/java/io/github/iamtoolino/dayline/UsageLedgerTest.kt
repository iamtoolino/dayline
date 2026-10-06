package io.github.iamtoolino.dayline

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class UsageLedgerTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private fun time(value: String) = ZonedDateTime.parse(value).toInstant().toEpochMilli()
    @Test fun `midnight assigns seconds to their own days`() {
        val result = mutableMapOf<String, Long>()
        UsageLedger.split(time("2026-10-06T23:59:50+02:00"), time("2026-10-07T00:00:20+02:00"), zone) { day, ms -> result[day] = ms }
        assertEquals(mapOf("2026-10-06" to 10_000L, "2026-10-07" to 20_000L), result)
    }
    @Test fun `DST fallback day has twenty five hours`() {
        val result = mutableMapOf<String, Long>()
        UsageLedger.split(time("2026-10-25T00:00:00+02:00"), time("2026-10-26T00:00:00+01:00"), zone) { day, ms -> result[day] = ms }
        assertEquals(25 * 3_600_000L, result["2026-10-25"])
    }
    @Test fun `DST spring day has twenty three hours`() {
        var total = 0L
        UsageLedger.split(time("2026-03-29T00:00:00+01:00"), time("2026-03-30T00:00:00+02:00"), zone) { _, ms -> total += ms }
        assertEquals(23 * 3_600_000L, total)
    }
    @Test fun `empty or reversed intervals add nothing`() {
        var calls = 0
        UsageLedger.split(1000, 1000, zone) { _, _ -> calls++ }
        UsageLedger.split(2000, 1000, zone) { _, _ -> calls++ }
        assertEquals(0, calls)
    }
    @Test fun `counter never wraps at one hour`() {
        assertEquals("00:00", UsageLedger.format(-1))
        assertEquals("59:59", UsageLedger.format(3_599_000))
        assertEquals("1:00:00", UsageLedger.format(3_600_000))
        assertEquals("25:01:02", UsageLedger.format(90_062_000))
    }
}
