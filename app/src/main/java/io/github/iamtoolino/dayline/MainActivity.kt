package io.github.iamtoolino.dayline

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : TrackerActivity() {
    private lateinit var store: TrackerStore
    private lateinit var status: TextView
    private lateinit var startButton: Button
    private lateinit var trackingOptions: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var permissionsWereReady = false
    private val refresh = object : Runnable {
        override fun run() {
            if (permissionsWereReady != Access.ready(this@MainActivity)) render()
            if (permissionsWereReady) updateTracking()
            handler.postDelayed(this, 1000)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); store = TrackerStore(this) }
    override fun onResume() { super.onResume(); render(); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }

    private fun render() {
        permissionsWereReady = Access.ready(this)
        if (!permissionsWereReady) { setup(); return }
        val body = screen("Your day, in view.", "Track only the apps you choose.")
        val summary = card()
        val trackingRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        status = text("", 16f, accent)
        trackingRow.addView(status, LinearLayout.LayoutParams(0, -2, 1f))
        trackingOptions = text("⋮", 24f, muted).apply {
            gravity = Gravity.CENTER
            contentDescription = "Tracking options"
            background = ripple(surface)
            isClickable = true; isFocusable = true
            setOnClickListener {
                PopupMenu(this@MainActivity, this).apply {
                    menu.add("Pause tracking")
                    setOnMenuItemClickListener {
                        store.enabled = false
                        stopService(Intent(this@MainActivity, TrackingService::class.java))
                        handler.postDelayed({ updateTracking() }, 250)
                        true
                    }
                    show()
                }
            }
        }
        trackingRow.addView(trackingOptions, LinearLayout.LayoutParams(dp(48), dp(48)))
        summary.addView(trackingRow)
        startButton = button("Start tracking", primary = true) {
            if (store.selected.isEmpty()) startActivity(Intent(this, AppPickerActivity::class.java))
            else {
                store.enabled = true; startForegroundService(Intent(this, TrackingService::class.java))
                handler.postDelayed({ updateTracking() }, 250)
            }
        }
        summary.addView(startButton)
        updateTracking()
        body.addView(summary)
        body.addView(setting("History", "Weekly graph and totals") { startActivity(Intent(this, StatisticsActivity::class.java)) })
        section(body, "YOUR APPS")
        val apps = card()
        if (store.selected.isEmpty()) apps.addView(text("Choose which apps to count.", 15f, muted))
        store.selected.sortedBy { label(it) }.forEach { apps.addView(appRow(it)) }
        apps.addView(button(if (store.selected.isEmpty()) "Choose apps" else "Edit selected apps") { startActivity(Intent(this, AppPickerActivity::class.java)) })
        body.addView(apps)
        section(body, "DAILY LIMIT")
        body.addView(setting("Daily budget", if (store.dailyLimitMinutes == 0) "Timer only · no budget" else "${store.dailyLimitMinutes} minutes · shared across selected apps") { editLimit() })
        section(body, "FLOATING TIMER")
        val appearance = card()
        appearance.addView(text("Drag the timer to position it.", 13f, muted))
        appearance.addView(text("Size", 13f, muted))
        appearance.addView(slider(12, 24, store.textSize) { store.textSize = it; TimerDisplay.showCurrentTotal() })
        appearance.addView(text("Opacity", 13f, muted))
        appearance.addView(slider(40, 100, store.opacity) { store.opacity = it; TimerDisplay.showCurrentTotal() })
        appearance.addView(Switch(this).apply {
            setText(R.string.timer_animations); textSize = 17f; setTextColor(ink)
            thumbTintList = ColorStateList.valueOf(accent)
            isChecked = store.visualReminders
            setOnCheckedChangeListener { _, checked ->
                store.visualReminders = checked
                if (checked) TimerDisplay.previewAnimation() else TimerDisplay.stopAnimation()
            }
        })
        appearance.addView(text("Brief halos mark your budget. Turn on to preview.", 13f, muted))
        body.addView(appearance)
        body.addView(setting("Notifications", "Manage in Android settings") { openNotificationSettings() })
        body.addView(text("Private by design. No account, internet access, or analytics.", 12f, muted))
        body.addView(buildIdentity())
    }
    private fun updateTracking() {
        val running = TrackingService.running
        status.text = if (running) "Tracking is on" else "Tracking is off"
        status.setTextColor(if (running) accent else muted)
        startButton.visibility = if (running) View.GONE else View.VISIBLE
        trackingOptions.visibility = if (running) View.VISIBLE else View.GONE
    }
    private fun setup() {
        val body = screen("A little setup.", "Two permissions, then you're ready.")
        val usage = card()
        usage.addView(text("1  Know which app is open", 18f))
        usage.addView(text("Usage access lets the timer count only selected apps. It doesn't read what's on your screen.", 14f, muted))
        usage.addView(button(if (Access.usage(this)) "✓ Usage access enabled" else "Allow usage access", primary = !Access.usage(this)) {
            openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")))
        })
        body.addView(usage)
        val overlay = card()
        overlay.addView(text("2  Show your floating timer", 18f))
        overlay.addView(text("Allow a small timer over other apps. You can move it wherever it fits.", 14f, muted))
        overlay.addView(button(if (Settings.canDrawOverlays(this)) "✓ Floating timer enabled" else "Allow floating timer", primary = !Settings.canDrawOverlays(this)) {
            openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        body.addView(overlay)
        body.addView(text("Everything stays on your phone. This setup disappears when both permissions are enabled.", 13f, muted))
        body.addView(buildIdentity())
    }
    private fun buildIdentity() = text(getString(R.string.build_identity,
        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.GIT_REVISION, BuildConfig.BUILD_TYPE), 12f, muted)
    private fun openNotificationSettings() {
        openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }
    private fun slider(min: Int, max: Int, value: Int, change: (Int) -> Unit) = SeekBar(this).apply {
        this.max = max - min; progress = value - min
        progressTintList = ColorStateList.valueOf(accent); thumbTintList = ColorStateList.valueOf(accent)
        setPadding(0, dp(8), 0, dp(8))
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) { if (fromUser) change(progress + min) }
            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onStopTrackingTouch(bar: SeekBar) {}
        })
    }
    private fun editLimit() {
        val content = card()
        content.addView(text("Your daily budget", 22f))
        content.addView(text("Minutes across all selected apps. Zero means no warning.", 14f, muted))
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER; hint = "Minutes"; textSize = 28f; setTextColor(ink); setHintTextColor(muted)
            setText(store.dailyLimitMinutes.takeIf { it > 0 }?.toString() ?: "")
            setPadding(dp(16), dp(14), dp(16), dp(14)); background = shape(bg, 14)
        }
        content.addView(input)
        val dialog = AlertDialog.Builder(this).setView(content).create()
        content.addView(button("Save budget", primary = true) {
            val minutes = if (input.text.isBlank()) 0 else input.text.toString().toIntOrNull()
            if (minutes == null || minutes !in 0..1440) input.error = "Enter 0–1440 minutes"
            else {
                store.dailyLimitMinutes = minutes
                dialog.dismiss(); render()
            }
        })
        content.addView(button("Cancel") { dialog.dismiss() })
        dialog.show(); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
