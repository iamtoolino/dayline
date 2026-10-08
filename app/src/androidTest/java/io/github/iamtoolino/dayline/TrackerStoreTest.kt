package io.github.iamtoolino.dayline

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

/** Isolated preference file: never modifies the user's tracker settings or usage. */
class TrackerStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "shared-budget-test"
    private val prefs get() = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    @Before fun setup() { prefs.edit().clear().commit() }
    @After fun cleanup() { prefs.edit().clear().commit() }

    @Test fun reminderPreferenceSurvivesRestartAndHistoryDeletion() {
        val store = TrackerStore(context, name)
        assertFalse(store.tenMinuteTicks)
        store.tenMinuteTicks = true
        store.clearUsage()
        assertTrue(TrackerStore(context, name).tenMinuteTicks)
    }
    @Test fun upgradePreservesAndCombinesExistingHistoryExactlyOnce() {
        val day = LocalDate.now().toString()
        prefs.edit().putLong("usage:$day:app.a", 30_000).putLong("usage:$day:app.b", 20_000)
            .putInt("limit:app.a", 60).commit()
        assertEquals(50_000L, TrackerStore(context, name).combinedToday())
        assertEquals(50_000L, TrackerStore(context, name).combinedToday())
        assertEquals(0, TrackerStore(context, name).dailyLimitMinutes)
        assertEquals(2, TrackerStore(context, name).history()[day]!!.size)
    }
    @Test fun switchingAppsAndChangingSelectionDoesNotResetTheSharedCounter() {
        val store = TrackerStore(context, name)
        val now = System.currentTimeMillis()
        store.setSelected(setOf("app.a", "app.b"))
        store.add("app.a", now - 5000, now - 2000)
        store.add("app.b", now - 2000, now)
        assertEquals(5000L, store.combinedToday())
        store.setSelected(setOf("app.b"))
        assertEquals(5000L, store.combinedToday())
        assertEquals(setOf("app.b"), store.selected)
    }
    @Test fun oneSharedWarningSurvivesRestartAndHistoryDeletionPreservesSettings() {
        val store = TrackerStore(context, name)
        val day = LocalDate.now().toString()
        store.setSelected(setOf("app.a", "app.b")); store.dailyLimitMinutes = 30
        assertFalse(store.warned(day))
        store.markWarned(day)
        val restarted = TrackerStore(context, name)
        assertTrue(restarted.warned(day))
        restarted.clearUsage()
        assertFalse(restarted.warned(day))
        assertEquals(0L, restarted.combinedToday())
        assertEquals(30, restarted.dailyLimitMinutes)
        assertEquals(setOf("app.a", "app.b"), restarted.selected)
    }
}
