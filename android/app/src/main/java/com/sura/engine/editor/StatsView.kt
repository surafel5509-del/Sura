package com.sura.engine.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Profiler panel: fps graph + counters, refreshed every frame from the render loop. */
class StatsView(context: Context, private val state: EditorState) : LinearLayout(context) {

    private val graph = FpsGraphView(context, state)
    private val values = TextView(context).apply {
        setTextColor(EditorTheme.text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        typeface = android.graphics.Typeface.MONOSPACE
        setPadding(EditorTheme.dp(context, 8), EditorTheme.dp(context, 6), EditorTheme.dp(context, 8), EditorTheme.dp(context, 6))
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(EditorTheme.panelAlt)
        addView(EditorTheme.header(context, "  PERFORMANCE"))
        addView(graph, LayoutParams(LayoutParams.MATCH_PARENT, EditorTheme.dp(context, 90)))
        addView(values)
    }

    fun update() {
        val p = state.profiler
        val scene = state.scene
        values.text = buildString {
            append("FPS          ").append(p.fps.toInt()).append("  (min ").append(p.minFps.toInt()).append(" / max ").append(p.maxFps.toInt()).append(")\n")
            append("Frame        ").append("%.2f".format(p.frameMs)).append(" ms   avg ").append("%.2f".format(p.averageFrameMs())).append(" ms\n")
            append("Draw calls   ").append(p.drawCalls).append("   quads ").append(p.triangles).append("\n")
            append("Objects      ").append(scene?.allObjects?.size ?: 0).append("   components ").append(scene?.componentCount() ?: 0).append("\n")
            append("Memory       ").append("%.1f".format(p.memoryMb)).append(" MB\n")
            append("Tool         ").append(state.tool.name).append("   selection ").append(state.selection?.name ?: "-")
        }
        graph.invalidate()
    }
}

private class FpsGraphView(context: Context, private val state: EditorState) : View(context) {
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = EditorTheme.success
        strokeWidth = EditorTheme.dpF(context, 1.5f)
    }
    private val gridPaint = Paint().apply { color = EditorTheme.border; strokeWidth = 1f }
    private val targetPaint = Paint().apply { color = EditorTheme.warning; strokeWidth = 1f }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(EditorTheme.panel)
        val w = width.toFloat()
        val h = height.toFloat()
        // horizontal guides at 30 and 60 fps
        for (fps in listOf(30, 60)) {
            val y = h - (fps / 70f) * h
            if (y in 0f..h) canvas.drawLine(0f, y, w, y, targetPaint)
        }
        for (i in 1..3) {
            val x = w * i / 4f
            canvas.drawLine(x, 0f, x, h, gridPaint)
        }
        val samples = state.profiler.samples
        if (samples.size < 2) return
        val step = w / (samples.size - 1)
        var previousX = 0f
        var previousY = h
        for (i in samples.indices) {
            val fps = if (samples[i] > 0f) 1f / samples[i] else 0f
            val normalized = (fps / 70f).coerceIn(0f, 1f)
            val x = i * step
            val y = h - normalized * h
            canvas.drawLine(previousX, previousY, x, y, linePaint)
            previousX = x
            previousY = y
        }
    }
}
