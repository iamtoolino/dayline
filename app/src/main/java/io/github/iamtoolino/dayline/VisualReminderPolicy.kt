package io.github.iamtoolino.dayline

enum class TimerTone { CYAN, AMBER, RED }
enum class VisualCue(val waves: Int) { NONE(0), HALO(1), DOUBLE_HALO(2) }

/** All timing uses combined active usage, never wall-clock time or time since app launch. */
object VisualReminderPolicy {
    fun tone(total: Long, limit: Long): TimerTone = when {
        limit <= 0 -> TimerTone.CYAN
        total >= limit -> TimerTone.RED
        total * 10 >= limit * 7 -> TimerTone.AMBER
        else -> TimerTone.CYAN
    }

    fun cue(before: Long, after: Long, limit: Long, enabled: Boolean, limitShown: Boolean,
            lastCueAt: Long = -120_000): VisualCue {
        if (!enabled || after <= before) return VisualCue.NONE
        // The exact limit always wins, even shortly after an ordinary milestone.
        if (limit > 0 && before < limit && after >= limit && !limitShown) return VisualCue.DOUBLE_HALO
        if (after - lastCueAt < 120_000) return VisualCue.NONE
        if (limit <= 0) return if (before / 600_000 < after / 600_000) VisualCue.HALO else VisualCue.NONE
        if (after >= limit) {
            val previous = (before - limit).coerceAtLeast(0) / 300_000
            val current = (after - limit) / 300_000
            return if (current > previous) VisualCue.DOUBLE_HALO else VisualCue.NONE
        }
        // Ten percentage milestones; crowded crossings are skipped, never queued for later.
        return if (before * 10 / limit < after * 10 / limit) VisualCue.HALO else VisualCue.NONE
    }
}
