package io.github.iamtoolino.dayline

import android.app.AlertDialog
import android.content.Intent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.os.Bundle

class StatisticsActivity : TrackerActivity() {
    override fun onResume() { super.onResume(); render() }
    private fun render() {
        val store = TrackerStore(this)
        val body = screen("Your time", "One daily total. See where it went.", back = true)
        val history = store.history()
        if (history.isEmpty()) body.addView(card().apply { addView(text("A fresh start", 20f)); addView(text("Your history will appear after you use a selected app.", 14f, muted)) })
        history.forEach { (day, apps) ->
            val row = card()
            val date = LocalDate.parse(day)
            row.addView(text(if (date == LocalDate.now()) "Today" else date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")), 14f, muted))
            row.addView(text(UsageLedger.format(apps.values.sum()), 40f, accent))
            apps.entries.sortedByDescending { it.value }.forEach { (pkg, ms) ->
                row.addView(appRow(pkg).apply { addView(text(UsageLedger.format(ms), 14f, muted)) })
            }
            body.addView(row)
        }
        body.addView(text("Resets at midnight. Past days stay here. Removing an app doesn't erase time already spent.", 13f, muted))
        body.addView(button("Delete history") {
            val content = card().apply {
                addView(text("Delete your history?", 22f))
                addView(text("Tracking stops first. Your selected apps and daily limit stay saved.", 14f, muted))
            }
            val dialog = AlertDialog.Builder(this).setView(content).create()
            content.addView(button("Delete history", primary = true) {
                store.enabled = false; stopService(Intent(this, TrackingService::class.java))
                store.clearUsage(); dialog.dismiss(); render()
            })
            content.addView(button("Keep it") { dialog.dismiss() })
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show(); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        })
    }
}
