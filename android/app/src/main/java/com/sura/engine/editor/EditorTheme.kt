package com.sura.engine.editor

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Central look & feel for the editor: one dark professional palette, consistent metrics. */
object EditorTheme {
    // Catppuccin-inspired dark palette used across the whole IDE.
    val background = Color.parseColor("#11111B")
    val panel = Color.parseColor("#1E1E2E")
    val panelAlt = Color.parseColor("#181825")
    val panelLight = Color.parseColor("#313244")
    val surface = Color.parseColor("#45475A")
    val border = Color.parseColor("#2A2A3C")
    val accent = Color.parseColor("#89B4FA")
    val accentAlt = Color.parseColor("#74C7EC")
    val success = Color.parseColor("#A6E3A1")
    val warning = Color.parseColor("#F9E2AF")
    val error = Color.parseColor("#F38BA8")
    val text = Color.parseColor("#CDD6F4")
    val textDim = Color.parseColor("#A6ADC8")
    val textFaint = Color.parseColor("#6C7086")

    const val MENU_HEIGHT_DP = 38
    const val TOOLBAR_HEIGHT_DP = 44
    const val LEFT_PANEL_WIDTH_DP = 232
    const val RIGHT_PANEL_WIDTH_DP = 268
    const val CONSOLE_HEIGHT_DP = 150
    const val TAB_HEIGHT_DP = 30

    fun dp(context: Context, value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

    fun dpF(context: Context, value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)

    fun label(context: Context, text: String, sizeSp: Float = 12f, color: Int = EditorTheme.text, bold: Boolean = false): TextView {
        val tv = TextView(context)
        tv.text = text
        tv.setTextColor(color)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        tv.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4))
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        return tv
    }

    fun button(context: Context, text: String, onTap: () -> Unit = {}): Button {
        val b = Button(context)
        b.text = text
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        b.setTextColor(text)
        b.isAllCaps = false
        b.background = toolbarButtonBackground(context)
        b.setPadding(dp(context, 10), 0, dp(context, 10), 0)
        b.setOnClickListener { onTap() }
        return b
    }

    fun toolbarButtonBackground(context: Context): android.graphics.drawable.GradientDrawable {
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.setColor(panelLight)
        drawable.cornerRadius = dpF(context, 5f)
        drawable.setStroke(dp(context, 1), border)
        return drawable
    }

    fun row(context: Context, heightDp: Int = 34): LinearLayout {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(context, heightDp)
        )
        return row
    }

    fun header(context: Context, text: String): TextView {
        val tv = label(context, text, 12f, accent, true)
        tv.setBackgroundColor(panelAlt)
        return tv
    }

    fun divider(context: Context): View {
        val v = View(context)
        v.setBackgroundColor(border)
        v.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1))
        return v
    }

    fun panel(context: Context, background: Int = panel): LinearLayout {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(background)
        return layout
    }
}
