package io.github.iamtoolino.dayline

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.WindowInsets
import android.widget.*

/** Shared visual primitives for the three small, native screens. */
open class TrackerActivity : Activity() {
    protected val bg get() = getColor(R.color.dayline_background)
    protected val surface get() = getColor(R.color.dayline_surface)
    protected val ink get() = getColor(R.color.dayline_text)
    protected val muted get() = getColor(R.color.dayline_muted)
    protected val accent get() = getColor(R.color.dayline_primary)
    protected fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    protected fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    protected fun text(value: String, size: Float = 16f, color: Int = ink) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); setPadding(0, dp(4), 0, dp(4))
        includeFontPadding = false
    }
    protected fun shape(color: Int, radius: Int = 22) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    protected fun ripple(color: Int, radius: Int = 18) = RippleDrawable(
        ColorStateList.valueOf(getColor(R.color.dayline_ripple)), shape(color, radius), shape(Color.WHITE, radius)
    )
    protected fun card() = column().apply {
        setPadding(dp(20), dp(18), dp(20), dp(18)); background = shape(surface)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
    }
    protected fun button(label: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 15f; setTextColor(if (primary) bg else accent); isAllCaps = false; includeFontPadding = false
        gravity = Gravity.CENTER; minHeight = dp(48); setPadding(dp(18), dp(12), dp(18), dp(12))
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        background = ripple(if (primary) accent else getColor(R.color.dayline_control), 16)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(4) }
        isClickable = true; isFocusable = true; setOnClickListener { action() }
    }
    protected fun section(body: LinearLayout, label: String) {
        body.addView(text(label, 13f, muted).apply { setPadding(dp(2), dp(12), 0, dp(12)) })
    }
    protected fun screen(title: String, subtitle: String, back: Boolean = false): LinearLayout {
        val root = column().apply { setBackgroundColor(bg) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; isVerticalScrollBarEnabled = false }
        val body = column().apply { setPadding(dp(22), dp(20), dp(22), dp(28)) }
        scroll.addView(body); root.addView(scroll); setContentView(root)
        if (back) body.addView(button("‹  Back") { finish() }.apply {
            gravity = Gravity.START; background = null; setPadding(0, dp(8), 0, dp(8))
        }) else body.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(18))
            addView(ImageView(this@TrackerActivity).apply {
                setImageResource(R.drawable.ic_dayline_foreground)
                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(8) })
            addView(text(getString(R.string.app_wordmark), 11f, accent).apply { letterSpacing = .16f })
        })
        body.addView(text(title, 30f).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
        body.addView(text(subtitle, 14f, muted).apply { setPadding(0, dp(8), 0, dp(20)) })
        return body
    }
    protected fun label(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) { pkg }
    protected fun appIcon(pkg: String, size: Int = 38): ImageView = ImageView(this).apply {
        try { setImageDrawable(packageManager.getApplicationIcon(pkg)) }
        catch (_: PackageManager.NameNotFoundException) { setImageResource(R.drawable.ic_dayline) }
        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)).apply { marginEnd = dp(14) }
    }
    protected fun appRow(pkg: String) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, dp(10))
        addView(appIcon(pkg)); addView(text(label(pkg), 16f).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(0, -2, 1f))
    }
    protected fun setting(title: String, detail: String, action: () -> Unit): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)); background = ripple(surface)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        val copy = column(); copy.addView(text(title, 16f)); copy.addView(text(detail, 13f, muted))
        addView(copy, LinearLayout.LayoutParams(0, -2, 1f)); addView(text("›", 25f, muted))
        isClickable = true; isFocusable = true; setOnClickListener { action() }
    }
    protected fun openSettings(intent: Intent) {
        try { startActivity(intent) } catch (_: android.content.ActivityNotFoundException) {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName")))
        }
    }
}
