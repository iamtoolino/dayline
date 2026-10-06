package io.github.iamtoolino.dayline

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Calendar splitting is separate from Android so midnight/DST can be tested deterministically. */
object UsageLedger {
    fun split(start: Long, end: Long, zone: ZoneId, add: (String, Long) -> Unit) {
        var cursor = start
        while (cursor < end) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val boundary = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val stop = minOf(end, boundary)
            add(date.toString(), stop - cursor)
            cursor = stop
        }
    }

    fun format(ms: Long): String {
        val seconds = ms.coerceAtLeast(0) / 1000
        return if (seconds < 3600) String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
        else String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    }
}
