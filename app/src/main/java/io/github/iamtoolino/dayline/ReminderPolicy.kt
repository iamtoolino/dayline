package io.github.iamtoolino.dayline

/** Choose at most one pulse per accounting tick; elapsed usage is the only clock. */
object ReminderPolicy {
    enum class Pulse { NONE, TICK, LIMIT }
    private const val TEN_MINUTES = 600_000L

    fun choose(before: Long, after: Long, limit: Long, warnAtLimit: Boolean,
               alreadyWarned: Boolean, tenMinuteTicks: Boolean): Pulse {
        if (warnAtLimit && limit > 0 && after >= limit && !alreadyWarned) return Pulse.LIMIT
        if (tenMinuteTicks && before >= 0 && after > before &&
            after / TEN_MINUTES > before / TEN_MINUTES) return Pulse.TICK
        return Pulse.NONE
    }
}
