package com.sura.engine.editor

import android.content.Context
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Console with a command line - the fastest way to drive the editor from a tablet keyboard. */
class ConsoleView(context: Context, private val state: EditorState) : LinearLayout(context) {

    private val logContainer = LinearLayout(context).apply { orientation = VERTICAL }
    private val scroll = ScrollView(context)
    private val input = EditText(context)
    private var renderedCount = 0

    init {
        orientation = VERTICAL
        setBackgroundColor(EditorTheme.panelAlt)
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(EditorTheme.label(context, "  CONSOLE", 11.5f, EditorTheme.accent, true), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(EditorTheme.button(context, "Clear") {
            state.consoleLines.clear()
            renderedCount = 0
            refresh()
        })
        addView(header)

        scroll.addView(logContainer)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        input.apply {
            hint = "type a command (help)"
            setTextColor(EditorTheme.text)
            setHintTextColor(EditorTheme.textFaint)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setBackgroundColor(EditorTheme.panel)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(EditorTheme.dp(context, 8), EditorTheme.dp(context, 6), EditorTheme.dp(context, 8), EditorTheme.dp(context, 6))
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                    runCommand(text.toString())
                    setText("")
                    true
                } else false
            }
        }
        addView(input)

        state.subscribe { post { refresh() } }
        refresh()
    }

    fun runCommand(command: String) {
        if (command.isBlank()) return
        state.logCommand(command)
        val result = EditorCommands.execute(command, state)
        state.log(if (result.ok) result.message else "error: ${result.message}", if (result.ok) EditorState.ConsoleLine.Level.INFO else EditorState.ConsoleLine.Level.ERROR)
    }

    fun log(message: String, level: EditorState.ConsoleLine.Level = EditorState.ConsoleLine.Level.INFO) {
        state.log(message, level)
        post { refresh() }
    }

    fun refresh() {
        if (renderedCount > state.consoleLines.size) { renderedCount = 0; logContainer.removeAllViews() }
        while (renderedCount < state.consoleLines.size) {
            val line = state.consoleLines[renderedCount]
            logContainer.addView(makeLine(line))
            renderedCount++
        }
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun makeLine(line: EditorState.ConsoleLine): TextView {
        val color = when (line.level) {
            EditorState.ConsoleLine.Level.ERROR -> EditorTheme.error
            EditorState.ConsoleLine.Level.WARN -> EditorTheme.warning
            EditorState.ConsoleLine.Level.COMMAND -> EditorTheme.accentAlt
            EditorState.ConsoleLine.Level.DEBUG -> EditorTheme.textFaint
            else -> EditorTheme.text
        }
        val prefix = when (line.level) {
            EditorState.ConsoleLine.Level.ERROR -> "[error] "
            EditorState.ConsoleLine.Level.WARN -> "[warn] "
            EditorState.ConsoleLine.Level.COMMAND -> ""
            else -> "[info] "
        }
        return TextView(context).apply {
            text = prefix + line.message
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(EditorTheme.dp(context, 6), EditorTheme.dp(context, 1), EditorTheme.dp(context, 6), EditorTheme.dp(context, 1))
        }
    }
}
