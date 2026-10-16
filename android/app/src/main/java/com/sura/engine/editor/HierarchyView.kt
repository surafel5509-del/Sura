package com.sura.engine.editor

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import com.sura.engine.scene.GameObject

/** Scene tree panel: search, select, rename, duplicate, delete, toggle active. */
class HierarchyView(context: Context, private val state: EditorState) : LinearLayout(context) {

    private val listContainer = LinearLayout(context).apply { orientation = VERTICAL }
    private val searchField = EditText(context).apply {
        hint = "Search objects..."
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setTextColor(EditorTheme.text)
        setHintTextColor(EditorTheme.textFaint)
        setBackgroundColor(EditorTheme.panelAlt)
        setPadding(EditorTheme.dp(context, 8), EditorTheme.dp(context, 6), EditorTheme.dp(context, 8), EditorTheme.dp(context, 6))
        setSingleLine()
    }
    private val searchQuery = StringBuilder()
    private val collapsed = mutableSetOf<String>()

    init {
        orientation = VERTICAL
        setBackgroundColor(EditorTheme.panel)
        addView(EditorTheme.header(context, "  HIERARCHY"))
        addView(searchField, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(EditorTheme.divider(context))
        val scroll = ScrollView(context)
        scroll.addView(listContainer)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery.clear(); searchQuery.append(s?.toString() ?: "")
                refresh()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        state.subscribe { post { refresh() } }
        refresh()
    }

    fun refresh() {
        listContainer.removeAllViews()
        val scene = state.scene ?: run {
            listContainer.addView(EditorTheme.label(context, "No scene loaded", 11f, EditorTheme.textFaint))
            return
        }
        val query = searchQuery.toString().trim().lowercase()
        if (query.isNotEmpty()) {
            val matches = scene.allObjects.filter { it.name.lowercase().contains(query) }
            if (matches.isEmpty()) listContainer.addView(EditorTheme.label(context, "No matches", 11f, EditorTheme.textFaint))
            for (go in matches) listContainer.addView(makeRow(go, 0))
            return
        }
        if (scene.rootObjects.isEmpty()) {
            listContainer.addView(EditorTheme.label(context, "Scene is empty - tap + to create", 11f, EditorTheme.textFaint))
        }
        for (go in scene.rootObjects) addNode(go, 0)
    }

    private fun addNode(go: GameObject, depth: Int) {
        listContainer.addView(makeRow(go, depth))
        if (collapsed.contains(go.name)) return
        for (child in go.children) addNode(child, depth + 1)
    }

    private fun makeRow(go: GameObject, depth: Int): View {
        val row = LinearLayout(context)
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(EditorTheme.dp(context, 4 + depth * 12), EditorTheme.dp(context, 7), EditorTheme.dp(context, 4), EditorTheme.dp(context, 7))
        val selected = state.selection === go
        row.setBackgroundColor(if (selected) EditorTheme.surface else android.graphics.Color.TRANSPARENT)

        val expander = EditorTheme.label(
            context,
            if (go.children.isEmpty()) "  " else if (collapsed.contains(go.name)) "\u25B8 " else "\u25BE ",
            10f,
            EditorTheme.textDim
        )
        expander.setPadding(0, 0, 0, 0)
        if (go.children.isNotEmpty()) {
            expander.setOnClickListener {
                if (collapsed.contains(go.name)) collapsed.remove(go.name) else collapsed.add(go.name)
                refresh()
            }
        }
        row.addView(expander, LayoutParams(EditorTheme.dp(context, 16), LayoutParams.WRAP_CONTENT))

        val icon = EditorTheme.label(context, iconFor(go), 11f, EditorTheme.accentAlt)
        icon.setPadding(0, 0, EditorTheme.dp(context, 4), 0)
        row.addView(icon)

        val name = EditorTheme.label(context, go.name, 11.5f, if (go.active) EditorTheme.text else EditorTheme.textFaint, selected)
        name.setTypeface(Typeface.DEFAULT, if (selected) Typeface.BOLD else Typeface.NORMAL)
        row.addView(name, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        val eye = EditorTheme.label(context, if (go.active) "\u25C9" else "\u25CB", 12f, if (go.active) EditorTheme.success else EditorTheme.textFaint)
        eye.setOnClickListener {
            val previous = go.active
            go.active = !go.active
            state.isDirty = true
            state.undoStack.push("Toggle ${go.name}", { go.active = previous }, { go.active = !previous })
            refresh()
        }
        row.addView(eye)

        row.setOnClickListener { state.select(go) }
        row.setOnLongClickListener { showContextMenu(go, row); true }
        return row
    }

    private fun iconFor(go: GameObject): String = when {
        go.getComponent<com.sura.engine.rendering.Light2D>() != null -> "\u2600"
        go.getComponent<com.sura.engine.rendering.CameraComponent>() != null -> "\u25A3"
        go.getComponent<com.sura.engine.particles.ParticleSystem>() != null -> "\u2733"
        go.getComponent<com.sura.engine.tilemap.TilemapComponent>() != null -> "\u25A6"
        go.getComponent<com.sura.engine.scripting.ScriptBehaviour>() != null -> "\u2318"
        go.getComponent<com.sura.engine.rendering.SpriteRenderer>() != null -> "\u25A0"
        else -> "\u25CB"
    }

    private fun showContextMenu(go: GameObject, anchor: View) {
        val scene = state.scene ?: return
        val menu = PopupMenu(context, anchor)
        menu.menu.add("Rename").setOnMenuItemClickListener {
            Dialogs.prompt(context, "Rename object", go.name) { text ->
                val previous = go.name
                if (text.isNotBlank()) {
                    go.name = text.trim()
                    state.isDirty = true
                    state.undoStack.push("Rename", { go.name = previous }, { go.name = text.trim() })
                    state.notifyChanged()
                }
            }
            true
        }
        menu.menu.add("Duplicate").setOnMenuItemClickListener {
            val copy = state.scene?.let { com.sura.engine.serialization.SceneSerializer().deserializeObject(
                com.sura.engine.serialization.SceneSerializer().serializeObject(go), it) }
            if (copy != null) {
                copy.name = state.uniqueName(go.name)
                copy.transform.position.x += state.snapSize
                scene.adopt(copy)
                state.isDirty = true
                state.select(copy)
            }
            true
        }
        menu.menu.add("Create child").setOnMenuItemClickListener {
            val child = scene.createGameObject(state.uniqueName("Child"), go)
            state.isDirty = true
            state.select(child)
            true
        }
        menu.menu.add("Move to root").setOnMenuItemClickListener {
            go.setParent(null)
            state.isDirty = true
            state.notifyChanged()
            true
        }
        menu.menu.add("Delete").setOnMenuItemClickListener {
            state.select(go)
            state.deleteSelected()
            true
        }
        menu.show()
    }
}
