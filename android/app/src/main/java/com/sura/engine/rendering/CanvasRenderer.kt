package com.sura.engine.rendering

import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.RectF
import com.sura.engine.utils.Color
import com.sura.engine.utils.Rect
import com.sura.engine.utils.Vector2

class CanvasRenderer : Renderer {
    var canvas: Canvas? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var camera: Camera? = null

    override fun begin(camera: Camera) { this.camera = camera }
    override fun end() { camera = null }

    override fun drawSprite(sprite: Sprite, position: Vector2, rotation: Float, scale: Vector2, color: Color) {
        val c = canvas ?: return
        val cam = camera
        val sx = if (cam != null) (position.x - cam.position.x) * cam.zoom + cam.viewportWidth/2f else position.x
        val sy = if (cam != null) (position.y - cam.position.y) * cam.zoom + cam.viewportHeight/2f else position.y
        paint.color = color.toInt()
        paint.style = Paint.Style.FILL
        val size = 32f * scale.x * (cam?.zoom ?: 1f)
        c.save()
        c.translate(sx, sy)
        c.rotate(rotation)
        c.drawRect(-size/2f, -size/2f, size/2f, size/2f, paint)
        c.restore()
    }
    override fun drawRect(rect: Rect, color: Color, filled: Boolean) {
        val c = canvas ?: return
        paint.color = color.toInt()
        paint.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
        c.drawRect(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, paint)
    }
    override fun drawCircle(center: Vector2, radius: Float, color: Color, filled: Boolean) {
        val c = canvas ?: return
        paint.color = color.toInt()
        paint.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
        c.drawCircle(center.x, center.y, radius, paint)
    }
    override fun drawLine(a: Vector2, b: Vector2, color: Color, thickness: Float) {
        val c = canvas ?: return
        paint.color = color.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = thickness
        c.drawLine(a.x, a.y, b.x, b.y, paint)
    }
    override fun drawText(text: String, position: Vector2, size: Float, color: Color) {
        val c = canvas ?: return
        paint.color = color.toInt()
        paint.textSize = size
        paint.style = Paint.Style.FILL
        c.drawText(text, position.x, position.y, paint)
    }
    override fun clear(color: Color) {
        canvas?.drawColor(color.toInt())
    }
}
