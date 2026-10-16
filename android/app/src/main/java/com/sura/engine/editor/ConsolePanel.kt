package com.sura.engine.editor

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sura.engine.core.Logger

class ConsolePanel @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val header = TextView(context).apply { text = "Console"; textSize = 14f; setPadding(8,8,8,8) }
    private val maxLines = 200
    private val lines = mutableListOf<String>()

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(0xFF1E1E2E.toInt())
        addView(header)
        val scroll = ScrollView(context)
        scroll.addView(container)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        Logger.addListener { level, tag, msg ->
            post { log("${level.name}: [$tag] $msg") }
        }
    }

    fun log(msg: String) {
        lines.add(msg)
        if (lines.size > maxLines) { lines.removeAt(0); if (container.childCount > 0) container.removeViewAt(0) }
        val tv = TextView(context).apply { text = msg; textSize = 11f; setPadding(4,1,4,1); setTextColor(colorFor(msg)) }
        container.addView(tv)
    }
    fun clear() { lines.clear(); container.removeAllViews() }

    private fun colorFor(msg: String): Int = when {
        msg.startsWith("ERROR") -> 0xFFF38BA8.toInt()
        msg.startsWith("WARN") -> 0xFFF9E2AF.toInt()
        else -> 0xFFCDD6F4.toInt()
    }
}
