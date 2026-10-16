package com.sura.engine.editor

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

class ProjectPanel @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val header = TextView(context).apply { text = "Project"; textSize = 14f; setPadding(8,8,8,8) }
    var rootPath: String = ""
    var onFileSelected: ((File) -> Unit)? = null

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(0xFF313244.toInt())
        addView(header)
        val scroll = ScrollView(context)
        scroll.addView(container)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun refresh(dir: File?) {
        container.removeAllViews()
        if (dir == null || !dir.exists()) { addLabel("No project"); return }
        addLabel(dir.name, true)
        for (f in (dir.listFiles() ?: emptyArray()).sortedBy { it.name }) {
            addLabel("${if (f.isDirectory) "[D] " else "    "}${f.name}")
        }
    }
    private fun addLabel(text: String, bold: Boolean = false) {
        val tv = TextView(context).apply { this.text = text; textSize = 12f; setPadding(8,2,8,2); setTextColor(0xFFCDD6F4.toInt()) }
        container.addView(tv)
    }
}
