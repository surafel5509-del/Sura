package com.sura.engine.editor

import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.sura.engine.scripting.Interpreter
import com.sura.engine.scripting.ScriptException
import com.sura.engine.scripting.ScriptBehaviour

/** Full screen-ish SuraScript editor with syntax check + live test-run. */
object ScriptEditorDialog {

    fun show(context: Context, behaviour: ScriptBehaviour, state: EditorState) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(EditorTheme.panel)
            setPadding(EditorTheme.dp(context, 10), EditorTheme.dp(context, 8), EditorTheme.dp(context, 10), EditorTheme.dp(context, 8))
        }
        val nameRow = EditorTheme.row(context, 36)
        nameRow.addView(EditorTheme.label(context, "Script name", 11f, EditorTheme.textDim), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val nameField = EditText(context).apply {
            setText(behaviour.scriptName)
            setTextColor(EditorTheme.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setBackgroundColor(EditorTheme.panelAlt)
            setSingleLine()
        }
        nameRow.addView(nameField, LinearLayout.LayoutParams(EditorTheme.dp(context, 180), LinearLayout.LayoutParams.WRAP_CONTENT))
        container.addView(nameRow)

        val editor = EditText(context).apply {
            setText(behaviour.source)
            setTextColor(EditorTheme.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setBackgroundColor(EditorTheme.panelAlt)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(EditorTheme.dp(context, 10), EditorTheme.dp(context, 10), EditorTheme.dp(context, 10), EditorTheme.dp(context, 10))
        }
        container.addView(editor, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, EditorTheme.dp(context, 240)))

        val status = TextView(context).apply {
            setTextColor(EditorTheme.textDim)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            text = "Ready - functions: update(dt), start(), fixedUpdate(dt), onCollision(name)"
            setPadding(0, EditorTheme.dp(context, 6), 0, 0)
        }
        container.addView(status)

        val buttonRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        buttonRow.addView(EditorTheme.button(context, "Check") {
            try {
                val program = Interpreter().parse(editor.text.toString())
                status.setTextColor(EditorTheme.success)
                status.text = "No syntax errors. Functions: " + program.functionNames.joinToString(", ")
            } catch (e: ScriptException) {
                status.setTextColor(EditorTheme.error)
                status.text = "Syntax error -> ${e.message}"
            }
        })
        buttonRow.addView(EditorTheme.button(context, "Test run") {
            val interpreter = Interpreter()
            try {
                interpreter.execute(editor.text.toString())
                if (interpreter.hasFunction("start")) interpreter.callFunction("start")
                status.setTextColor(EditorTheme.success)
                status.text = "Test run OK. Globals: " + interpreter.globalsSnapshot().keys.joinToString(", ")
            } catch (e: ScriptException) {
                status.setTextColor(EditorTheme.error)
                status.text = "Runtime error -> ${e.message}"
            }
        })
        buttonRow.addView(EditorTheme.button(context, "Insert template") {
            editor.setText(InspectorView.DEFAULT_SCRIPT)
        })
        container.addView(buttonRow)

        AlertDialog.Builder(context)
            .setTitle("SuraScript editor")
            .setView(container)
            .setPositiveButton("Apply") { _, _ ->
                val previousName = behaviour.scriptName
                val previousSource = behaviour.source
                val newName = nameField.text.toString().ifBlank { "Script" }
                val newSource = editor.text.toString()
                behaviour.scriptName = newName
                behaviour.source = newSource
                behaviour.compile()
                state.isDirty = true
                state.undoStack.push("Edit script", {
                    behaviour.scriptName = previousName
                    behaviour.source = previousSource
                    behaviour.compile()
                }, {
                    behaviour.scriptName = newName
                    behaviour.source = newSource
                    behaviour.compile()
                })
                state.log("Script '${behaviour.scriptName}' updated (${newSource.lines().size} lines)")
                state.notifyChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
