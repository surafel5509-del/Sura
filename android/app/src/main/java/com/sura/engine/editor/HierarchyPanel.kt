package com.sura.engine.editor

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sura.engine.scene.GameObject
import com.sura.engine.scene.Scene

class HierarchyPanel @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val listContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val header = TextView(context).apply { text = "Hierarchy"; textSize = 14f; setPadding(8,8,8,8) }
    var onObjectSelected: ((GameObject?) -> Unit)? = null
    var selected: GameObject? = null

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(0xFF313244.toInt())
        addView(header)
        val scroll = ScrollView(context)
        scroll.addView(listContainer)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun refresh(scene: Scene?) {
        listContainer.removeAllViews()
        if (scene == null) return
        for (go in scene.rootObjects) {
            addNode(go, 0)
        }
    }

    private fun addNode(go: GameObject, depth: Int) {
        val tv = TextView(context).apply {
            text = "${"  ".repeat(depth)}${if (go.active) "" else "(disabled) "}${go.name}"
            textSize = 12f
            setPadding(8 + depth*16, 4, 8, 4)
            setTextColor(if (go == selected) 0xFF89B4FA.toInt() else 0xFFCDD6F4.toInt())
            setOnClickListener { selected = go; onObjectSelected?.invoke(go); refresh(go.scene) }
        }
        listContainer.addView(tv)
        for (child in go.transform.children) {
            // find gameobject? transform has no backref easily; skip deep
        }
    }

    fun addObject(name: String, scene: Scene?): GameObject? {
        val s = scene ?: return null
        val go = s.createGameObject(name)
        refresh(s)
        return go
    }
}
