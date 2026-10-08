package io.github.iamtoolino.dayline

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.widget.*
import java.time.LocalDate

class MainActivity : TrackerActivity() {
    private lateinit var store: TrackerStore
    private lateinit var status: TextView
    private lateinit var total: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var preview: TimerOverlay? = null
    private var permissionsWereReady = false
    private val refresh = object : Runnable {
        override fun run() {
            if (permissionsWereReady != Access.ready(this@MainActivity)) render()
            if (permissionsWereReady) {
                total.text = UsageLedger.format(store.combinedToday())
                status.text = if (TrackingService.running) "Tracking is on" else "Tracking is off"
            }
            handler.postDelayed(this, 1000)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); store = TrackerStore(this) }
    override fun onResume() { super.onResume(); render(); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); preview?.hide(); preview = null; super.onPause() }

    private fun render() {
        permissionsWereReady = Access.ready(this)
        if (!permissionsWereReady) { setup(); return }
        val body = screen("A clearer line on your day.", "One timer for the apps you choose.")
        val summary = card()
        status = text(if (TrackingService.running) "Tracking is on" else "Tracking is off", 13f, accent)
        total = text(UsageLedger.format(store.combinedToday()), 46f)
        summary.addView(status); summary.addView(total)
        summary.addView(text("Across selected apps today", 13f, muted))
        summary.addView(button(if (TrackingService.running) "Pause tracking" else "Start tracking", primary = true) {
            if (TrackingService.running) { store.enabled = false; stopService(Intent(this, TrackingService::class.java)) }
            else if (store.selected.isEmpty()) startActivity(Intent(this, AppPickerActivity::class.java))
            else {
                store.enabled = true; startForegroundService(Intent(this, TrackingService::class.java))
            }
            handler.postDelayed({ render() }, 250)
        })
        body.addView(summary)
        body.addView(setting("History", "Daily totals and app breakdowns") { startActivity(Intent(this, StatisticsActivity::class.java)) })
        section(body, "YOUR APPS")
        val apps = card()
        if (store.selected.isEmpty()) apps.addView(text("Pick the apps that pull you in.", 15f, muted))
        store.selected.sortedBy { label(it) }.forEach { apps.addView(appRow(it)) }
        apps.addView(button(if (store.selected.isEmpty()) "Choose apps" else "Edit selected apps") { startActivity(Intent(this, AppPickerActivity::class.java)) })
        body.addView(apps)
        section(body, "DAILY LIMIT")
        body.addView(setting("One shared budget", if (store.dailyLimitMinutes == 0) "No limit set · timer only" else "${store.dailyLimitMinutes} minutes across all selected apps") { editLimit() })
        section(body, "REMINDERS")
        val warning = card()
        warning.addView(toggle("Vibrate at the limit", store.vibration) { store.vibration = it })
        warning.addView(text("One gentle nudge per day. The timer keeps going; your apps stay open.", 13f, muted))
        warning.addView(toggle("Gentle tick every 10 minutes", store.tenMinuteTicks) { store.tenMinuteTicks = it })
        warning.addView(text("At 10, 20, 30… minutes across selected apps. No catch-up pulses when enabled.", 13f, muted))
        warning.addView(button("Test gentle tick") { testVibration(ReminderPolicy.Pulse.TICK) })
        warning.addView(button("Test limit warning") { testVibration(ReminderPolicy.Pulse.LIMIT) })
        warning.addView(text("Uses your phone's Notification vibration setting. Silent mode and Do Not Disturb can mute it. Test pulses don't change your timer or daily warning.", 13f, muted))
        body.addView(warning)
        section(body, "FLOATING TIMER")
        val appearance = card()
        appearance.addView(text("Keep it in view", 18f))
        appearance.addView(text("Drag to position it. The same timer follows you between selected apps.", 13f, muted))
        val previewButton = button("Position with preview") {}
        previewButton.setOnClickListener {
            if (preview != null) { preview?.hide(); preview = null; previewButton.setText(R.string.position_preview) }
            else { preview = TimerOverlay(this, store); preview?.show("Selected apps", store.combinedToday().takeIf { it > 0 } ?: 754_000, false); previewButton.setText(R.string.done_positioning) }
        }
        appearance.addView(previewButton)
        appearance.addView(text("Size", 13f, muted))
        appearance.addView(slider(12, 24, store.textSize) { store.textSize = it; updatePreview() })
        appearance.addView(text("Opacity", 13f, muted))
        appearance.addView(slider(40, 100, store.opacity) { store.opacity = it; updatePreview() })
        appearance.addView(button("Reset position") {
            preview?.hide(); preview = null; previewButton.setText(R.string.position_preview); store.x = dp(16); store.y = dp(120)
        })
        body.addView(appearance)
        section(body, "STAY OUT OF THE WAY")
        val quiet = card()
        val notificationsEnabled = getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        quiet.addView(text(if (notificationsEnabled) "A quieter notification" else "Notifications are hidden", 18f))
        quiet.addView(text(if (Build.VERSION.SDK_INT >= 33) "Turn off notifications in Android settings to hide the tracking notice. The floating timer still works." else "Android requires a tracking notification. You can make it silent in notification settings.", 13f, muted))
        quiet.addView(button("Notification settings") { openNotificationSettings() })
        body.addView(quiet)
        body.addView(text("Private by design. No account, internet access, or analytics.", 12f, muted))
        body.addView(buildIdentity())
    }
    private fun testVibration(pulse: ReminderPolicy.Pulse) {
        val requested = ReminderVibration.play(this, pulse)
        Toast.makeText(this, if (requested) "Pulse requested. No buzz? Check sound mode, Notification vibration, and Do Not Disturb."
            else "This device has no vibrator.", Toast.LENGTH_LONG).show()
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
    private fun updatePreview() { preview?.show("Selected apps", store.combinedToday().takeIf { it > 0 } ?: 754_000, false) }
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
    private fun toggle(label: String, value: Boolean, change: (Boolean) -> Unit) = Switch(this).apply {
        text = label; textSize = 15f; setTextColor(ink); isChecked = value; setPadding(0, dp(8), 0, dp(8))
        thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(accent, muted))
        setOnCheckedChangeListener { _, checked -> change(checked) }
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
                if (minutes > 0 && store.combinedToday() >= minutes * 60_000L) store.markWarned(LocalDate.now().toString())
                dialog.dismiss(); render()
            }
        })
        content.addView(button("Cancel") { dialog.dismiss() })
        dialog.show(); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
