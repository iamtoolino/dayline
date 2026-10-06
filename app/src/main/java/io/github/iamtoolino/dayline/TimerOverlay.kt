package io.github.iamtoolino.dayline

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.roundToInt

class TimerOverlay(private val context: Context, private val store: TrackerStore) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val density = context.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).roundToInt()
    private val pill = object : TextView(context) {
        override fun performClick(): Boolean { super.performClick(); return true }
    }.apply {
        setPadding(dp(14), dp(8), dp(14), dp(8))
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        elevation = dp(6).toFloat()
    }
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; x = store.x; y = store.y }
    private var attached = false
    private var originX = 0
    private var originY = 0
    private var touchX = 0f
    private var touchY = 0f

    init {
        pill.setOnTouchListener { _: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    originX = params.x; originY = params.y; touchX = event.rawX; touchY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = originX + (event.rawX - touchX).roundToInt()
                    params.y = originY + (event.rawY - touchY).roundToInt()
                    clamp(); if (attached) wm.updateViewLayout(pill, params)
                }
                MotionEvent.ACTION_UP -> {
                    store.x = params.x; store.y = params.y
                    if (kotlin.math.abs(event.rawX - touchX) < dp(8) && kotlin.math.abs(event.rawY - touchY) < dp(8)) pill.performClick()
                }
                MotionEvent.ACTION_CANCEL -> { store.x = params.x; store.y = params.y }
            }
            true
        }
    }
    private fun clamp() {
        val bounds = wm.maximumWindowMetrics.bounds
        params.x = params.x.coerceIn(0, (bounds.width() - pill.measuredWidth).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (bounds.height() - dp(80) - pill.measuredHeight).coerceAtLeast(0))
    }
    fun show(label: String, ms: Long, over: Boolean) {
        if (!attached) { params.x = store.x; params.y = store.y }
        pill.text = context.getString(if (over) R.string.over_goal_timer else R.string.today_timer, UsageLedger.format(ms))
        pill.contentDescription = "$label, today ${UsageLedger.format(ms)}${if (over) ", over goal" else ""}. Drag to move."
        pill.textSize = store.textSize.toFloat()
        pill.setTextColor(context.getColor(if (over) R.color.dayline_warning else R.color.dayline_primary))
        pill.background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat(); setColor(context.getColor(R.color.dayline_background))
            setStroke(dp(1), context.getColor(if (over) R.color.dayline_warning else R.color.dayline_outline))
        }
        params.alpha = store.opacity / 100f
        pill.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        clamp()
        if (!attached) { wm.addView(pill, params); attached = true }
        else wm.updateViewLayout(pill, params)
    }
    fun hide() { if (attached) { wm.removeView(pill); attached = false } }
}
