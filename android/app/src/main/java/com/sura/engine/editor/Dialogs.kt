package com.sura.engine.editor

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import android.util.TypedValue

/** Reusable retro-dark dialogs used across the editor. */
object Dialogs {

    fun prompt(
        context: Context,
        title: String,
        initial: String = "",
        numeric: Boolean = false,
        onSubmit: (String) -> Unit
    ) {
        val input = EditText(context)
        input.setText(initial)
        input.setTextColor(EditorTheme.text)
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        input.setBackgroundColor(EditorTheme.panelAlt)
        if (numeric) input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        val container = LinearLayout(context)
        container.setPadding(EditorTheme.dp(context, 16), EditorTheme.dp(context, 8), EditorTheme.dp(context, 16), 0)
        container.addView(input)
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("OK") { _, _ -> onSubmit(input.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun promptMultiline(context: Context, title: String, initial: String, onSubmit: (String) -> Unit) {
        val input = EditText(context)
        input.setText(initial)
        input.setTextColor(EditorTheme.text)
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        input.setBackgroundColor(EditorTheme.panelAlt)
        input.setPadding(EditorTheme.dp(context, 8), EditorTheme.dp(context, 8), EditorTheme.dp(context, 8), EditorTheme.dp(context, 8))
        input.minLines = 8
        input.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        val container = LinearLayout(context)
        container.setPadding(EditorTheme.dp(context, 14), EditorTheme.dp(context, 8), EditorTheme.dp(context, 14), 0)
        container.addView(input, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, EditorTheme.dp(context, 260)))
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Apply") { _, _ -> onSubmit(input.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun confirm(context: Context, title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun alert(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    fun choose(context: Context, title: String, options: List<String>, onChoose: (Int) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(options.toTypedArray()) { _, index -> onChoose(index) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun custom(context: Context, title: String, view: android.view.View, positive: String = "Create", onPositive: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(positive) { _, _ -> onPositive() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
