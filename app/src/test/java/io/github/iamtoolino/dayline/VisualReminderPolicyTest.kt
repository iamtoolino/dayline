package io.github.iamtoolino.dayline

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualReminderPolicyTest {
    @Test fun colorsFollowBudgetIncludingDisabledRemindersAndNoBudget() {
        assertEquals(TimerTone.CYAN, VisualReminderPolicy.tone(4_199_999, 6_000_000))
        assertEquals(TimerTone.AMBER, VisualReminderPolicy.tone(4_200_000, 6_000_000))
        assertEquals(TimerTone.RED, VisualReminderPolicy.tone(6_000_000, 6_000_000))
        assertEquals(TimerTone.CYAN, VisualReminderPolicy.tone(9_000_000, 0))
    }
    @Test fun tenMilestonesAreRelativeToBudget() {
        assertEquals(VisualCue.NONE, VisualReminderPolicy.cue(598_000, 599_000, 6_000_000, true, false))
        assertEquals(VisualCue.HALO, VisualReminderPolicy.cue(599_000, 600_000, 6_000_000, true, false))
        assertEquals(VisualCue.HALO, VisualReminderPolicy.cue(359_000, 360_000, 3_600_000, true, false))
        assertEquals(VisualCue.HALO, VisualReminderPolicy.cue(4_199_000, 4_200_000, 6_000_000, true, false))
    }
    @Test fun shortBudgetSkipsCrowdedMilestonesButLimitAlwaysWins() {
        assertEquals(VisualCue.HALO, VisualReminderPolicy.cue(59_000, 60_000, 600_000, true, false))
        assertEquals(VisualCue.NONE, VisualReminderPolicy.cue(119_000, 120_000, 600_000, true, false, 60_000))
        assertEquals(VisualCue.HALO, VisualReminderPolicy.cue(179_000, 180_000, 600_000, true, false, 60_000))
        assertEquals(VisualCue.DOUBLE_HALO, VisualReminderPolicy.cue(599_000, 600_000, 600_000, true, false, 540_000))
    }
    @Test fun overBudgetRepeatsAtFiveMinutesBeyondLimitRatherThanClockMultiples() {
        val limit = 420_000L
        assertEquals(VisualCue.DOUBLE_HALO, VisualReminderPolicy.cue(limit - 1, limit, limit, true, false))
        assertEquals(VisualCue.NONE, VisualReminderPolicy.cue(599_000, 600_000, limit, true, true))
        assertEquals(VisualCue.DOUBLE_HALO, VisualReminderPolicy.cue(719_000, 720_000, limit, true, true))
        assertEquals(VisualCue.DOUBLE_HALO, VisualReminderPolicy.cue(1_019_000, 1_020_000, limit, true, true))
    }
    @Test fun noBudgetRetainsTenMinuteCyanRhythm() {
        assertEquals(VisualCue.NONE, VisualReminderPolicy.cue(299_000, 300_000, 0, true, false))
        assertEquals(VisualCue.HALO, VisualReminderPolicy.cue(599_000, 600_000, 0, true, false))
    }
    @Test fun disablingRestartingOrLoweringBudgetDoesNotReplay() {
        assertEquals(VisualCue.NONE, VisualReminderPolicy.cue(599_000, 600_000, 600_000, false, false))
        assertEquals(VisualCue.NONE, VisualReminderPolicy.cue(900_000, 901_000, 600_000, true, false))
        assertEquals(VisualCue.NONE, VisualReminderPolicy.cue(600_000, 600_000, 600_000, true, false))
        assertEquals(VisualCue.NONE, VisualReminderPolicy.cue(599_000, 600_000, 600_000, true, true))
        assertEquals(VisualCue.NONE, VisualReminderPolicy.cue(0, 1000, 600_000, true, false))
    }
}
