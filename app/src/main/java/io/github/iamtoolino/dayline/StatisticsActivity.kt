package io.github.iamtoolino.dayline

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class StatisticsActivity : TrackerActivity() {
    private var week = WeeklyUsage.start(LocalDate.now())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString("week")?.let { week = LocalDate.parse(it) }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("week", week.toString())
        super.onSaveInstanceState(outState)
    }
    override fun onResume() { super.onResume(); render() }
    private fun render() {
        val store = TrackerStore(this)
        val today = LocalDate.now()
        val currentWeek = WeeklyUsage.start(today)
        if (week > currentWeek) week = currentWeek
        val days = WeeklyUsage.days(week)
        val totals = days.map { store.dailyTotal(it.toString()) }
        val body = screen("Your week", "Time in the apps you chose.", back = true)
        val navigation = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
        }
        navigation.addView(button("‹") { week = week.minusWeeks(1); render() }.apply {
            contentDescription = "Previous week"
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val rangeFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        val dateRange = "${week.format(rangeFormat)} – ${week.plusDays(6).format(rangeFormat)}"
        val year = if (week.year == week.plusDays(6).year) "${week.year}" else "${week.year} / ${week.plusDays(6).year}"
        navigation.addView(text("$dateRange\n$year", 15f).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, -2, 1f))
        navigation.addView(button("›") { week = week.plusWeeks(1); render() }.apply {
            contentDescription = "Next week"
            isEnabled = week < currentWeek
            alpha = if (isEnabled) 1f else .3f
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        body.addView(navigation)
        val chart = card()
        chart.addView(text(if (week == currentWeek) "This week so far" else "Weekly total", 14f, muted))
        chart.addView(text(WeeklyUsage.duration(totals.sum()), 38f, accent))
        val bars = LinearLayout(this).apply {
            gravity = Gravity.BOTTOM
            setPadding(0, dp(20), 0, dp(8))
        }
        val peak = totals.maxOrNull()?.coerceAtLeast(60_000) ?: 60_000
        days.forEachIndexed { index, day ->
            val total = totals[index]
            val future = day > today
            val column = column().apply {
                gravity = Gravity.CENTER_HORIZONTAL
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                contentDescription = "${day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))}: " +
                    if (future) "Upcoming" else WeeklyUsage.duration(total)
            }
            column.addView(text(if (future) "—" else WeeklyUsage.duration(total), 10f, muted).apply {
                gravity = Gravity.CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            val track = LinearLayout(this).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL }
            track.addView(View(this).apply {
                background = shape(if (total == 0L) getColor(R.color.dayline_control) else accent, 6)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(20), if (total > 0) maxOf(dp(3), (dp(140) * (total.toDouble() / peak)).toInt()) else dp(3)))
            column.addView(track, LinearLayout.LayoutParams(-1, dp(140)))
            column.addView(text(day.format(DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault())), 13f,
                if (day == today) accent else muted).apply {
                gravity = Gravity.CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            column.addView(text(day.dayOfMonth.toString(), 12f, if (day == today) accent else muted).apply {
                gravity = Gravity.CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            bars.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
        }
        chart.addView(bars)
        if (totals.all { it == 0L }) chart.addView(text("No tracked time this week.", 14f, muted))
        body.addView(chart)
        body.addView(text("Monday–Sunday · combined time across selected apps. Past weeks stay saved when you change your selection.", 13f, muted))
        body.addView(button("Delete history") {
            val content = card().apply {
                addView(text("Delete all history?", 22f))
                addView(text("All weeks will be deleted. Tracking stops first. Your selected apps and daily limit stay saved.", 14f, muted))
            }
            val dialog = AlertDialog.Builder(this).setView(content).create()
            content.addView(button("Delete history", primary = true) {
                store.enabled = false; stopService(Intent(this, TrackingService::class.java))
                store.clearUsage(); TimerDisplay.showCurrentTotal(); dialog.dismiss(); render()
            })
            content.addView(button("Keep it") { dialog.dismiss() })
            dialog.show(); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        })
    }
}
