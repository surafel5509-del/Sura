package com.sura.engine.editor

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sura.engine.scene.GameObject
import com.sura.engine.scene.Transform

class InspectorPanel @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val header = TextView(context).apply { text = "Inspector"; textSize = 14f; setPadding(8,8,8,8) }

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(0xFF313244.toInt())
        addView(header)
        val scroll = ScrollView(context)
        scroll.addView(container)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun inspect(go: GameObject?) {
        container.removeAllViews()
        if (go == null) { addLabel("No selection"); return }
        addLabel("GameObject: ${go.name}", true)
        addLabel("Tag: ${go.tag}")
        addLabel("Layer: ${go.layer}")
        addLabel("Active: ${go.active}")
        addSection("Transform")
        addLabel("Position: (${go.transform.position.x}, ${go.transform.position.y})")
        addLabel("Rotation: ${go.transform.rotation}")
        addLabel("Scale: (${go.transform.scale.x}, ${go.transform.scale.y})")
        addSection("Components (${go.components.size})")
        for (c in go.components) {
            addLabel("- ${c::class.simpleName}  ${if (c.enabled) "[on]" else "[off]"}")
        }
        addLabel("+ Add Component", false).setOnClickListener { }
    }

    private fun addSection(text: String) {
        val tv = TextView(context).apply { this.text = text; textSize = 12f; setPadding(8,12,8,4); setTextColor(0xFF89B4FA.toInt()) }
        container.addView(tv)
    }
    private fun addLabel(text: String, bold: Boolean = false): TextView {
        val tv = TextView(context).apply {
            this.text = text; textSize = 12f; setPadding(8,2,8,2)
            setTextColor(0xFFCDD6F4.toInt())
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }
        container.addView(tv)
        return tv
    }
}
