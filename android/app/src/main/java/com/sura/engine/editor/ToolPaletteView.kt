package com.sura.engine.editor

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/** Tool strip under the menu bar: transform tools, paint tools, snapping and playback controls. */
class ToolPaletteView(
    context: Context,
    private val state: EditorState,
    private val callbacks: EditorMenuBar.Callbacks,
    private val onToolChanged: () -> Unit
) : LinearLayout(context) {

    private val toolButtons = mutableMapOf<EditorTool, TextView>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(EditorTheme.panel)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, EditorTheme.dp(context, EditorTheme.TOOLBAR_HEIGHT_DP))
        setPadding(EditorTheme.dp(context, 6), 0, EditorTheme.dp(context, 6), 0)

        addView(EditorTheme.label(context, "TOOLS ", 10f, EditorTheme.textFaint))
        addTool(EditorTool.SELECT, "\u271B Select")
        addTool(EditorTool.MOVE, "\u2725 Move")
        addTool(EditorTool.ROTATE, "\u21BB Rotate")
        addTool(EditorTool.SCALE, "\u2197 Scale")
        addTool(EditorTool.TILE_PAINT, "\u25A6 Paint")
        addTool(EditorTool.TILE_ERASE, "\u25A1 Erase")
        addTool(EditorTool.PAN, "\u270B Pan")

        addView(space())
        addView(EditorTheme.button(context, "\u25B6 Play") { callbacks.onPlay() })
        addView(EditorTheme.button(context, "\u23F8 Pause") { callbacks.onPause() })
        addView(EditorTheme.button(context, "\u23ED Step") { callbacks.onStep() })
        addView(EditorTheme.button(context, "\u23F9 Stop") { callbacks.onStop() })

        addView(space())
        addView(toggle("Grid", state.showGrid) { callbacks.onToggleGrid() })
        addView(toggle("Stats", state.showStats) { callbacks.onToggleStats() })
        addView(toggle("Colliders", state.showColliders) { callbacks.onToggleColliders() })
        addView(toggle("Snap", state.snapToGrid) {
            state.snapToGrid = !state.snapToGrid
            state.notifyChanged()
        })
        addView(EditorTheme.button(context, "Focus") { callbacks.onFocusSelection() })
        addView(space())
        addView(EditorTheme.button(context, "Build \u2692") { callbacks.onBuild() })

        refresh()
    }

    private fun space(): LinearLayout = LinearLayout(context).apply {
        layoutParams = LayoutParams(0, 1, 1f)
    }

    private fun addTool(tool: EditorTool, label: String) {
        val view = EditorTheme.label(context, label, 11.5f, EditorTheme.text)
        view.background = EditorTheme.toolbarButtonBackground(context)
        view.setPadding(EditorTheme.dp(context, 10), EditorTheme.dp(context, 6), EditorTheme.dp(context, 10), EditorTheme.dp(context, 6))
        view.setOnClickListener {
            state.tool = tool
            state.log("tool: ${tool.name.lowercase()}")
            refresh()
            onToolChanged()
        }
        toolButtons[tool] = view
        addView(view)
        addView(LinearLayout(context).apply { layoutParams = LayoutParams(EditorTheme.dp(context, 4), 1) })
    }

    private fun toggle(label: String, initial: Boolean, onTap: () -> Unit): TextView {
        val view = EditorTheme.label(context, label, 11.5f, if (initial) EditorTheme.success else EditorTheme.textDim)
        view.background = EditorTheme.toolbarButtonBackground(context)
        view.setPadding(EditorTheme.dp(context, 10), EditorTheme.dp(context, 6), EditorTheme.dp(context, 10), EditorTheme.dp(context, 6))
        view.setOnClickListener {
            onTap()
            view.setTextColor(
                when (label) {
                    "Grid" -> if (state.showGrid) EditorTheme.success else EditorTheme.textDim
                    "Stats" -> if (state.showStats) EditorTheme.success else EditorTheme.textDim
                    "Colliders" -> if (state.showColliders) EditorTheme.success else EditorTheme.textDim
                    else -> if (state.snapToGrid) EditorTheme.success else EditorTheme.textDim
                }
            )
        }
        return view
    }

    fun refresh() {
        for ((tool, view) in toolButtons) {
            val selected = state.tool == tool
            view.setTextColor(if (selected) EditorTheme.accent else EditorTheme.text)
            view.setTypeface(android.graphics.Typeface.DEFAULT, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (selected) 12f else 11.5f)
        }
    }
}
