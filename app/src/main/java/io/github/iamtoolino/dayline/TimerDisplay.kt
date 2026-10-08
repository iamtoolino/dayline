package io.github.iamtoolino.dayline

import android.annotation.SuppressLint
import android.content.Context
import android.app.KeyguardManager
import android.os.PowerManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/** One overlay shared by tracking and Dayline's screens. All calls run on the main thread. */
// The retained context is a dedicated overlay WindowContext, never an Activity or Service.
@SuppressLint("StaticFieldLeak")
object TimerDisplay {
    private var overlay: TimerOverlay? = null
    private var context: Context? = null
    private var store: TrackerStore? = null
    private var openScreens = 0
    private val screenOpen get() = openScreens > 0
    private val handler = Handler(Looper.getMainLooper())
    private var hidePending = false
    private val delayedHide = Runnable {
        hidePending = false
        if (!screenOpen) overlay?.hide(immediate = locked())
    }
    private fun cancelPendingHide() {
        handler.removeCallbacks(delayedHide)
        hidePending = false
    }
    private val refresh = object : Runnable {
        override fun run() {
            if (!screenOpen) return
            showCurrentTotal()
            handler.postDelayed(this, 1000)
        }
    }
    private fun initialize(context: Context) {
        if (this.context != null) return
        val application = context.applicationContext
        val display = application.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        this.context = application.createDisplayContext(display)
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        store = TrackerStore(application)
    }
    private fun timer(): TimerOverlay? {
        val context = context ?: return null
        if (!Settings.canDrawOverlays(context)) {
            cancelPendingHide(); overlay?.hide(immediate = true); return null
        }
        return overlay ?: TimerOverlay(context, store!!).also { overlay = it }
    }
    fun openScreen(context: Context) {
        initialize(context)
        cancelPendingHide()
        openScreens++
        handler.removeCallbacks(refresh)
        refresh.run()
    }
    fun closeScreen() {
        openScreens = (openScreens - 1).coerceAtLeast(0)
        if (screenOpen) return
        handler.removeCallbacks(refresh)
        hideOverlay()
    }
    fun showCurrentTotal() {
        if (!screenOpen) return
        cancelPendingHide()
        val store = store ?: return
        val total = store.combinedToday()
        timer()?.show("Selected apps", total, VisualReminderPolicy.tone(total, store.dailyLimitMinutes * 60_000L))
    }
    fun previewAnimation() {
        showCurrentTotal()
        val store = store ?: return
        val tone = VisualReminderPolicy.tone(store.combinedToday(), store.dailyLimitMinutes * 60_000L)
        overlay?.animateCue(if (tone == TimerTone.RED) VisualCue.DOUBLE_HALO else VisualCue.HALO)
    }
    fun stopAnimation() { overlay?.stopAnimation() }
    fun showTracked(context: Context, total: Long, tone: TimerTone, cue: VisualCue) {
        initialize(context)
        cancelPendingHide()
        // Dayline may remain started beside a selected app in split-screen.
        // Genuine milestones must still reach the same shared overlay.
        timer()?.let {
            it.show("Selected apps", total, tone)
            if (store?.visualReminders == false) it.stopAnimation()
            it.animateCue(cue)
        }
    }
    private fun locked(): Boolean {
        val context = context ?: return true
        return context.getSystemService(KeyguardManager::class.java).isKeyguardLocked ||
            !context.getSystemService(PowerManager::class.java).isInteractive
    }
    private fun hideOverlay() {
        val context = context ?: return
        if (locked() || !Settings.canDrawOverlays(context)) {
            cancelPendingHide()
            overlay?.hide(immediate = true)
        } else if (!hidePending) {
            // Bridge one tracking poll (1s) without extending usage accounting.
            // Repeated hide requests must not postpone a genuine departure forever.
            hidePending = true
            handler.postDelayed(delayedHide, 1250)
        }
    }
    fun hideTracked() { if (!screenOpen) hideOverlay() }
}
