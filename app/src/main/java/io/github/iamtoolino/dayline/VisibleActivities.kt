package io.github.iamtoolino.dayline

/** Pausing loses focus, not visibility. Stop is the boundary for removing an activity. */
class VisibleActivities {
    private data class Activity(val packageName: String, val className: String?)
    private val visible = linkedSetOf<Activity>()
    val foreground: String? get() = visible.lastOrNull()?.packageName

    fun resume(packageName: String, className: String?) {
        val activity = Activity(packageName, className)
        visible.remove(activity)
        visible.add(activity)
    }
    fun stop(packageName: String, className: String?) {
        visible.remove(Activity(packageName, className))
    }
    fun clear() = visible.clear()
}
