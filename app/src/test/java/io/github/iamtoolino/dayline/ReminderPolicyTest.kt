package io.github.iamtoolino.dayline

import org.junit.Assert.assertEquals
import org.junit.Test
import io.github.iamtoolino.dayline.ReminderPolicy.Pulse

class ReminderPolicyTest {
    private fun choose(before: Long, after: Long, limit: Long = 0, warn: Boolean = false,
                       warned: Boolean = false, ticks: Boolean = true) =
        ReminderPolicy.choose(before, after, limit, warn, warned, ticks)

    @Test fun sharedTimeCrossesOnlyOneMilestone() {
        assertEquals(Pulse.NONE, choose(598_000, 599_999))
        assertEquals(Pulse.TICK, choose(599_999, 600_000))
        assertEquals(Pulse.NONE, choose(600_000, 601_000))
        assertEquals(Pulse.TICK, choose(1_199_500, 1_200_500))
    }
    @Test fun disabledTicksAndRestartNeverCatchUp() {
        assertEquals(Pulse.NONE, choose(599_000, 600_000, ticks = false))
        assertEquals(Pulse.NONE, choose(650_000, 651_000))
        assertEquals(Pulse.NONE, choose(600_000, 600_000))
    }
    @Test fun midnightAndPausedTimeProduceNoPulse() {
        assertEquals(Pulse.NONE, choose(0, 1000))
        assertEquals(Pulse.NONE, choose(599_000, 599_000))
        assertEquals(Pulse.NONE, choose(700_000, 1000))
    }
    @Test fun limitWinsAndWarnsOnlyOnce() {
        assertEquals(Pulse.LIMIT, choose(599_000, 600_000, 600_000, warn = true))
        assertEquals(Pulse.NONE, choose(600_000, 601_000, 600_000, warn = true, warned = true))
        assertEquals(Pulse.TICK, choose(1_199_000, 1_200_000, 600_000, warn = true, warned = true))
    }
    @Test fun disabledLimitDoesNotSuppressIndependentTicks() {
        assertEquals(Pulse.TICK, choose(599_000, 600_000, 600_000, warn = false))
        assertEquals(Pulse.NONE, choose(599_000, 600_000, 600_000, warn = false, ticks = false))
    }
}
