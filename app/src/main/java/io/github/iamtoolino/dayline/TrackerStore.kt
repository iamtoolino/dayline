package io.github.iamtoolino.dayline

import android.content.Context
import java.time.LocalDate

class TrackerStore(context: Context, preferenceName: String = "tracker") {
    private val prefs = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    init {
        if (!prefs.getBoolean("sharedBudgetMigrated", false)) {
            val days = mutableMapOf<String, Long>()
            prefs.all.forEach { (key, value) ->
                if (key.startsWith("usage:") && value is Long) {
                    val day = key.split(':', limit = 3)[1]
                    days[day] = (days[day] ?: 0) + value
                }
            }
            val edit = prefs.edit()
            days.forEach { (day, value) -> edit.putLong("daily:$day", value) }
            edit.putBoolean("sharedBudgetMigrated", true).apply()
        }
    }
    var dailyLimitMinutes: Int
        get() = prefs.getInt("dailyLimitMinutes", 0)
        set(value) { prefs.edit().putInt("dailyLimitMinutes", value).apply() }
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }
    var vibration: Boolean
        get() = prefs.getBoolean("vibration", false)
        set(value) { prefs.edit().putBoolean("vibration", value).apply() }
    var textSize: Int
        get() = prefs.getInt("textSize", 16)
        set(value) { prefs.edit().putInt("textSize", value).apply() }
    var opacity: Int
        get() = prefs.getInt("opacity", 90)
        set(value) { prefs.edit().putInt("opacity", value).apply() }
    var x: Int
        get() = prefs.getInt("x", 16)
        set(value) { prefs.edit().putInt("x", value).apply() }
    var y: Int
        get() = prefs.getInt("y", 120)
        set(value) { prefs.edit().putInt("y", value).apply() }
    val selected: Set<String> get() = prefs.getStringSet("selected", emptySet())!!.toSet()
    fun setSelected(packages: Set<String>) { prefs.edit().putStringSet("selected", packages).apply() }
    fun combinedToday() = prefs.getLong("daily:${LocalDate.now()}", 0)
    fun total(day: String, pkg: String) = prefs.getLong("usage:$day:$pkg", 0)
    fun add(pkg: String, start: Long, end: Long) {
        val editor = prefs.edit()
        UsageLedger.split(start, end, java.time.ZoneId.systemDefault()) { day, ms ->
            editor.putLong("usage:$day:$pkg", total(day, pkg) + ms)
            editor.putLong("daily:$day", prefs.getLong("daily:$day", 0) + ms)
        }
        editor.apply()
    }
    fun warned(day: String) = prefs.getBoolean("budgetWarned:$day", false)
    fun markWarned(day: String) { prefs.edit().putBoolean("budgetWarned:$day", true).apply() }
    fun history(): Map<String, Map<String, Long>> {
        val days = sortedMapOf<String, MutableMap<String, Long>>(reverseOrder())
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("usage:") && value is Long) {
                val parts = key.split(':', limit = 3)
                days.getOrPut(parts[1]) { sortedMapOf() }[parts[2]] = value
            }
        }
        return days
    }
    fun clearUsage() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("usage:") || it.startsWith("warned:") || it.startsWith("daily:") || it.startsWith("budgetWarned:") }.forEach(editor::remove)
        editor.apply()
    }
}
