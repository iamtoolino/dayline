package io.github.iamtoolino.dayline

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import java.time.Instant
import java.time.ZoneId

/** Sole owner of usage writes. Stores each tick; never charges time while the service is absent. */
class TrackingService : Service() {
    private lateinit var store: TrackerStore
    private val handler = Handler(Looper.getMainLooper())
    private var cursor = 0L
    private var lastElapsed = 0L
    private var foreground: String? = null
    private var interactive = false
    private val tick = object : Runnable {
        override fun run() {
            try { update() } catch (_: SecurityException) {
                status = "Permission lost — reopen Dayline"; stopSelf()
            }
            if (running) handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = TrackerStore(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("tracking", "Background tracking", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null); enableVibration(false); setShowBadge(false)
                description = "Quiet indicator required by Android while tracking is enabled"
            }
        )
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP" || !store.enabled || !Access.ready(this)) {
            store.enabled = false; stopSelf(); return START_NOT_STICKY
        }
        if (!running) {
            if (Build.VERSION.SDK_INT >= 34) startForeground(1, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(1, notification())
            running = true; status = "Tracking selected apps"
            cursor = System.currentTimeMillis(); lastElapsed = SystemClock.elapsedRealtime()
            interactive = unlocked()
            // Initialize identity only. Earlier time is deliberately not imported.
            val events = getSystemService(UsageStatsManager::class.java).queryEvents(cursor - 86_400_000, cursor)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) { events.getNextEvent(event); transition(event) }
            interactive = unlocked()
            handler.post(tick)
        }
        return START_STICKY
    }
    private fun unlocked() = getSystemService(PowerManager::class.java).isInteractive &&
        !getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private fun transition(event: UsageEvents.Event) {
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> foreground = event.packageName
            UsageEvents.Event.ACTIVITY_PAUSED -> if (foreground == event.packageName) foreground = null
            UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN -> interactive = false
            UsageEvents.Event.SCREEN_INTERACTIVE, UsageEvents.Event.KEYGUARD_HIDDEN -> interactive = true
            UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP -> { foreground = null; interactive = false }
        }
    }
    private fun active(): String? = foreground?.takeIf { interactive && it in store.selected }
    private fun account(start: Long, end: Long) { active()?.let { if (end > start) store.add(it, start, end) } }
    private fun update() {
        if (!store.enabled || !Access.ready(this)) {
            status = "Tracking stopped — check permissions"; stopSelf(); return
        }
        val now = System.currentTimeMillis()
        val day = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val before = store.dailyTotal(day)
        val elapsed = SystemClock.elapsedRealtime()
        val gap = elapsed - lastElapsed
        // A suspension or clock jump is uncertain: don't inflate totals with an unobserved gap.
        val continuous = gap in 0..5000 && kotlin.math.abs((now - cursor) - gap) < 2000
        val events = getSystemService(UsageStatsManager::class.java).queryEvents(if (now >= cursor) cursor else now - 1000, now)
        val event = UsageEvents.Event()
        var from = if (continuous) cursor else now
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val time = event.timeStamp.coerceIn(minOf(from, now), now)
            if (continuous) account(from, time)
            from = time
            transition(event)
        }
        if (continuous) account(from, now)
        interactive = unlocked()
        cursor = now; lastElapsed = elapsed
        val pkg = active()
        if (pkg == null) TimerDisplay.hideTracked()
        else {
            val total = store.dailyTotal(day)
            val limit = store.dailyLimitMinutes * 60_000L
            val cue = VisualReminderPolicy.cue(before, total, limit, store.visualReminders, store.visualLimitShown(day), store.lastVisualCueAt(day))
            if (cue != VisualCue.NONE) {
                store.markVisualCue(day, total)
                if (limit > 0 && before < limit && total >= limit) store.markVisualLimitShown(day)
            }
            TimerDisplay.showTracked(this, total, VisualReminderPolicy.tone(total, limit), cue)
        }
        status = if (pkg == null) "Ready · waiting for a selected app" else "Tracking ${label(pkg)}"

    }
    private fun label(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: android.content.pm.PackageManager.NameNotFoundException) { pkg }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, TrackingService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "tracking").setSmallIcon(R.drawable.ic_dayline_monochrome)
            .setContentTitle(getString(R.string.app_name)).setContentText("Tracking enabled")
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open)
            .setCategory(Notification.CATEGORY_SERVICE).setVisibility(Notification.VISIBILITY_PRIVATE)
            .setShowWhen(false)
            .addAction(Notification.Action.Builder(null, "Stop tracking", stop).build())
            .build()
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); TimerDisplay.hideTracked(); running = false
        if (!store.enabled) status = "Tracking off"
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        var running = false; private set
        var status = "Tracking off"; private set
    }
}
