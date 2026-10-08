package io.github.iamtoolino.dayline

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

/** Actual overlay lifecycle, using isolated preferences and no usage/accounting writes. */
class TimerOverlayTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val name = "overlay-motion-test"
    private lateinit var store: TrackerStore
    private lateinit var overlay: TimerOverlay
    private val pill: View get() = TimerOverlay::class.java.getDeclaredField("pill").let {
        it.isAccessible = true
        it.get(overlay) as View
    }
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun awaitMain(condition: () -> Boolean) {
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        do {
            var ready = false
            main { ready = condition() }
            if (ready) return
            Thread.sleep(20)
        } while (android.os.SystemClock.uptimeMillis() < deadline)
        fail("Overlay did not reach the expected state within five seconds")
    }
    @Before fun setup() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        store = TrackerStore(context, name)
        main {
            val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
            val windowContext = context.createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
            overlay = TimerOverlay(windowContext, store)
        }
    }
    @After fun cleanup() {
        main { overlay.hide(immediate = true) }
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun interruptedExitReturnsToVisibleWithoutChangingGeometry() {
        assumeTrue(ValueAnimator.areAnimatorsEnabled())
        var width = 0
        var height = 0
        main {
            overlay.show("Test", 600_000, TimerTone.CYAN)
            width = pill.measuredWidth; height = pill.measuredHeight
            assertEquals(0f, pill.alpha, .01f)
        }
        Thread.sleep(90)
        main { overlay.hide() }
        Thread.sleep(40)
        main { overlay.show("Test", 600_000, TimerTone.CYAN) }
        awaitMain { pill.isAttachedToWindow && pill.alpha >= .99f }
        main {
            assertTrue(pill.isAttachedToWindow)
            assertEquals(1f, pill.alpha, .01f)
            assertEquals(width, pill.measuredWidth)
            assertEquals(height, pill.measuredHeight)
            overlay.hide()
            overlay.hide() // Repeated polling must not restart the exit.
        }
        awaitMain { !pill.isAttachedToWindow }
    }
    @Test fun enablingAnimationsDrawsAVisibleHalo() {
        assumeTrue(ValueAnimator.areAnimatorsEnabled())
        // Match the settings toggle: first disable motion, then enable and preview.
        store.visualReminders = false
        main { overlay.show("Test", 600_000, TimerTone.CYAN) }
        Thread.sleep(100)
        store.visualReminders = true
        main { overlay.animateCue(VisualCue.HALO) }
        Thread.sleep(150)
        main {
            val halo = TimerOverlay::class.java.getDeclaredField("halo").let {
                it.isAccessible = true
                it.get(overlay) as View
            }
            assertTrue("Preview halo must attach", halo.isAttachedToWindow)
            assertTrue(halo.width > 0 && halo.height > 0)
            val bitmap = Bitmap.createBitmap(halo.width, halo.height, Bitmap.Config.ARGB_8888)
            try {
                halo.draw(Canvas(bitmap))
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                assertTrue("Halo must draw visible pixels, not just an empty window",
                    pixels.any { Color.alpha(it) > 0 })
            } finally { bitmap.recycle() }
        }
    }
    @Test fun genuineLimitCuePlaysWhileDaylineScreenIsStarted() {
        assumeTrue(ValueAnimator.areAnimatorsEnabled())
        main {
            // Isolate the singleton from the real settings/history while exercising
            // the same started-screen + service path used by split-screen.
            val fields = listOf("overlay", "context", "store", "openScreens").associateWith {
                TimerDisplay::class.java.getDeclaredField(it).apply { isAccessible = true }
            }
            val saved = fields.mapValues { it.value.get(TimerDisplay) }
            try {
                fields.getValue("overlay").set(TimerDisplay, overlay)
                fields.getValue("context").set(TimerDisplay, context.applicationContext)
                fields.getValue("store").set(TimerDisplay, store)
                fields.getValue("openScreens").set(TimerDisplay, 0)
                TimerDisplay.openScreen(context)
                TimerDisplay.showTracked(context, 600_000, TimerTone.RED, VisualCue.DOUBLE_HALO)
                val cue = TimerOverlay::class.java.getDeclaredField("cue").apply { isAccessible = true }
                val tone = TimerOverlay::class.java.getDeclaredField("tone").apply { isAccessible = true }
                assertEquals(VisualCue.DOUBLE_HALO, cue.get(overlay))
                assertEquals(TimerTone.RED, tone.get(overlay))
            } finally {
                TimerDisplay.closeScreen()
                overlay.hide(immediate = true)
                fields.forEach { (name, field) -> field.set(TimerDisplay, saved[name]) }
            }
        }
    }
    @Test fun immediateHideCancelsAnInFlightAppearance() {
        main {
            overlay.show("Test", 600_000, TimerTone.AMBER)
            overlay.hide(immediate = true)
            assertFalse(pill.isAttachedToWindow)
        }
        Thread.sleep(300)
        main { assertFalse(pill.isAttachedToWindow) }
    }
    @Test fun disabledAnimationsShowAndRemoveSynchronously() {
        store.visualReminders = false
        main {
            overlay.show("Test", 600_000, TimerTone.RED)
            assertEquals(1f, pill.alpha, .01f)
            overlay.hide()
            assertFalse(pill.isAttachedToWindow)
            overlay.show("Test", 600_000, TimerTone.RED)
            assertEquals(1f, pill.alpha, .01f)
        }
        // addView attaches during the next traversal, even with motion disabled.
        Thread.sleep(100)
        main { assertTrue(pill.isAttachedToWindow) }
    }
}
