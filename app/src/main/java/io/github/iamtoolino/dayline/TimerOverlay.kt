package io.github.iamtoolino.dayline

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.input.InputManager
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import kotlin.math.roundToInt

class TimerOverlay(private val context: Context, private val store: TrackerStore) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val density = context.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).roundToInt()
    private val pill = object : TextView(context) {
        override fun performClick(): Boolean { super.performClick(); return true }
    }.apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        elevation = dp(6).toFloat()
        fontFeatureSettings = "tnum"
        setPadding(dp(14), dp(8), dp(14), dp(8))
    }
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; x = store.x; y = store.y }
    private var attached = false
    private var visibleWanted = false
    private var visibility = 0f
    private var visibilityAnimator: ValueAnimator? = null
    private val maximumTouchOpacity by lazy {
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(InputManager::class.java).maximumObscuringOpacityForTouch else 1f
    }
    private fun motionEnabled() = store.visualReminders && ValueAnimator.areAnimatorsEnabled()
    private var tone = TimerTone.CYAN
    private var originX = 0
    private var originY = 0
    private var touchX = 0f
    private var touchY = 0f
    private var animator: ValueAnimator? = null
    private var cue = VisualCue.NONE
    private var progress = 0f
    private var haloAttached = false
    private val haloParams = WindowManager.LayoutParams(
        1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        title = "Dayline milestone halo"
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ringBounds = RectF()
    private val halo = object : View(context) {
        override fun onDraw(canvas: Canvas) {
            val elapsed = progress * (700 + (cue.waves - 1) * 300)
            for (wave in 0 until cue.waves) {
                val phase = (elapsed - wave * 300) / 700f
                if (phase < 0f || phase > 1f) continue
                val spread = dp(14) * (1 - (1 - phase) * (1 - phase))
                ringBounds.set(
                    (params.x - haloParams.x).toFloat(), (params.y - haloParams.y).toFloat(),
                    (params.x - haloParams.x + pill.measuredWidth).toFloat(),
                    (params.y - haloParams.y + pill.measuredHeight).toFloat()
                )
                ringBounds.inset(-spread, -spread)
                ring.color = toneColor()
                ring.alpha = (230 * (1 - phase) * this@TimerOverlay.visibility).roundToInt()
                ring.strokeWidth = density * 2f
                canvas.drawRoundRect(ringBounds, ringBounds.height() / 2, ringBounds.height() / 2, ring)
            }
        }
    }

    init {
        pill.setOnTouchListener { _: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    stopAnimation()
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
    private fun toneColor() = context.getColor(when (tone) {
        TimerTone.CYAN -> R.color.dayline_primary
        TimerTone.AMBER -> R.color.dayline_warning
        TimerTone.RED -> R.color.dayline_over_limit
    })
    fun show(label: String, ms: Long, tone: TimerTone) {
        if (this.tone != tone) stopAnimation()
        this.tone = tone
        val appearing = !visibleWanted
        visibleWanted = true
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        if (!attached) { params.x = store.x; params.y = store.y }
        pill.text = context.getString(R.string.today_timer, UsageLedger.format(ms))
        val state = when (tone) {
            TimerTone.CYAN -> ""
            TimerTone.AMBER -> ", nearing daily budget"
            TimerTone.RED -> ", over daily budget"
        }
        pill.contentDescription = "$label, today ${UsageLedger.format(ms)}$state. Drag to move."
        pill.textSize = store.textSize.toFloat()
        pill.setTextColor(toneColor())
        pill.background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(context.getColor(R.color.dayline_background))
            setStroke(dp(1), if (tone == TimerTone.CYAN) context.getColor(R.color.dayline_outline) else toneColor())
        }
        params.alpha = store.opacity / 100f
        pill.alpha = visibility
        pill.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        clamp()
        if (!attached) { wm.addView(pill, params); attached = true }
        else wm.updateViewLayout(pill, params)
        if (haloAttached) updateHalo()
        if (appearing) fadeTo(1f)
    }
    fun animateCue(next: VisualCue) {
        if (next == VisualCue.NONE || !attached || !motionEnabled()) return
        stopAnimation()
        cue = next
        // Android 12+ requires a sufficiently transparent overlay to pass touches through it.
        haloParams.alpha = minOf(store.opacity / 100f, maximumTouchOpacity)
        updateHalo()
        wm.addView(halo, haloParams); haloAttached = true
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700L + (cue.waves - 1) * 300; interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                halo.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { finishAnimation() }
            })
            start()
        }
    }
    private fun updateHalo() {
        val bounds = wm.maximumWindowMetrics.bounds
        val margin = dp(16)
        haloParams.x = (params.x - margin).coerceAtLeast(0)
        haloParams.y = (params.y - margin).coerceAtLeast(0)
        haloParams.width = (params.x + pill.measuredWidth + margin).coerceAtMost(bounds.width()) - haloParams.x
        haloParams.height = (params.y + pill.measuredHeight + margin).coerceAtMost(bounds.height()) - haloParams.y
        if (haloAttached) wm.updateViewLayout(halo, haloParams)
    }
    fun stopAnimation() {
        animator?.removeAllListeners(); animator?.cancel()
        finishAnimation()
        if (!motionEnabled() && visibilityAnimator != null) {
            cancelFade()
            if (visibleWanted) { visibility = 1f; pill.alpha = 1f } else removePill()
        }
    }
    private fun finishAnimation() {
        animator = null
        if (haloAttached) { wm.removeView(halo); haloAttached = false }
        cue = VisualCue.NONE; progress = 0f
    }
    private fun cancelFade() {
        visibilityAnimator?.removeAllListeners()
        visibilityAnimator?.cancel()
        visibilityAnimator = null
    }
    private fun fadeTo(target: Float) {
        cancelFade()
        if (!attached) return
        if (!motionEnabled() || visibility == target) {
            visibility = target
            applyVisibility()
            if (target == 0f) removePill()
            return
        }
        visibilityAnimator = ValueAnimator.ofFloat(visibility, target).apply {
            duration = ((if (target == 1f) 220 else 160) * kotlin.math.abs(target - visibility)).toLong().coerceAtLeast(1)
            interpolator = if (target == 1f) DecelerateInterpolator() else AccelerateInterpolator()
            addUpdateListener {
                visibility = it.animatedValue as Float
                applyVisibility()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibilityAnimator = null
                    if (!visibleWanted) removePill()
                }
            })
            start()
        }
    }
    private fun applyVisibility() {
        if (!attached) return
        val opacity = store.opacity / 100f * visibility
        // Release input as soon as Android permits touch-through. This changes only opacity
        // once; every animation frame otherwise uses view alpha without window relayout.
        if (!visibleWanted && params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE == 0 && opacity <= maximumTouchOpacity) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            params.alpha = opacity.coerceAtLeast(.001f)
            wm.updateViewLayout(pill, params)
        }
        pill.alpha = (opacity / params.alpha).coerceIn(0f, 1f)
    }
    private fun removePill() {
        cancelFade()
        if (attached) { wm.removeView(pill); attached = false }
        visibility = 0f
        pill.alpha = 0f
    }
    fun hide(immediate: Boolean = false) {
        stopAnimation()
        if (immediate) { visibleWanted = false; removePill(); return }
        if (!visibleWanted) return
        visibleWanted = false
        fadeTo(0f)
    }
}
