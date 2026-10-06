package io.github.iamtoolino.dayline

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import java.util.Locale

class AppPickerActivity : TrackerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = TrackerStore(this)
        val installed = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }.filter { it != packageName }.distinct().sortedBy { label(it).lowercase(Locale.getDefault()) }
        val chosen = (savedInstanceState?.getStringArrayList("chosen")?.toSet() ?: store.selected).toMutableSet()
        selected = chosen
        val body = screen("Choose your apps", "Only these apps share the daily timer.", back = true)
        val count = text(getString(R.string.apps_selected, chosen.size), 13f, accent)
        val search = EditText(this).apply {
            hint = "Search apps"; textSize = 16f; setTextColor(ink); setHintTextColor(muted)
            isSingleLine = true; setPadding(dp(18), dp(14), dp(18), dp(14)); background = shape(surface, 16)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        }
        body.addView(search); body.addView(count)
        body.addView(button("Save selection", primary = true) { store.setSelected(chosen); finish() })
        val list = column(); body.addView(list)
        fun populate(query: String) {
            list.removeAllViews()
            val matches = installed.filter { label(it).contains(query, ignoreCase = true) }
            if (matches.isEmpty()) list.addView(text("No matching apps", 14f, muted))
            matches.forEach { pkg ->
                val row = appRow(pkg).apply {
                    background = ripple(surface, 18); setPadding(dp(16), dp(12), dp(10), dp(12))
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
                }
                val check = CheckBox(this).apply {
                    isChecked = pkg in chosen; buttonTintList = ColorStateList.valueOf(accent)
                    contentDescription = label(pkg)
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) chosen.add(pkg) else chosen.remove(pkg)
                        count.text = getString(R.string.apps_selected, chosen.size)
                    }
                }
                row.addView(check)
                row.isFocusable = true; row.setOnClickListener { check.isChecked = !check.isChecked }
                list.addView(row)
            }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { populate(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        populate("")
    }
    private var selected: Set<String> = emptySet()
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("chosen", ArrayList(selected)); super.onSaveInstanceState(outState)
    }
}
