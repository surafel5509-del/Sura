package com.sura.engine.rendering.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

/** Runtime generated bitmap font atlas (ASCII 32..126) so text rendering works with zero assets. */
class FontAtlas(val baseSize: Float = 32f) {

    data class Glyph(
        val u0: Float, val v0: Float, val u1: Float, val v1: Float,
        val width: Float, val height: Float, val advance: Float
    )

    var textureId = 0
        private set
    var atlasWidth = 1024
    var atlasHeight = 1024
    private val glyphs = mutableMapOf<Char, Glyph>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var built = false

    fun build(renderer: GLRenderer): Boolean {
        if (built) return true
        paint.color = android.graphics.Color.WHITE
        paint.textSize = baseSize
        paint.typeface = Typeface.MONOSPACE
        val bitmap = Bitmap.createBitmap(atlasWidth, atlasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cellHeight = baseSize * 1.45f
        var x = 4f
        var y = 4f
        val metrics = paint.fontMetrics
        for (code in 32..126) {
            val ch = code.toChar()
            val advance = if (ch == ' ') baseSize * 0.5f else paint.measureText(ch.toString())
            if (x + advance + 4f > atlasWidth) { x = 4f; y += cellHeight }
            if (y + cellHeight > atlasHeight) break
            canvas.drawText(ch.toString(), x, y + baseSize, paint)
            val u0 = x / atlasWidth
            val v0 = y / atlasHeight
            val u1 = (x + advance) / atlasWidth
            val v1 = (y + cellHeight) / atlasHeight
            val ascent = metrics.ascent
            val height = (metrics.descent - ascent)
            glyphs[ch] = Glyph(u0, v0, u1, v1, advance, height, advance)
            x += advance + 4f
        }
        textureId = renderer.createTexture(bitmap)
        bitmap.recycle()
        built = true
        return true
    }

    fun glyph(ch: Char): Glyph? = glyphs[ch]
    fun glyphCount(): Int = glyphs.size
    fun isBuilt(): Boolean = built

    /** Width in pixels of [text] rendered at [size]. */
    fun measure(text: String, size: Float): Float {
        val scale = size / baseSize
        var width = 0f
        for (ch in text) width += (glyphs[ch]?.advance ?: (baseSize * 0.5f)) * scale
        return width
    }
}
