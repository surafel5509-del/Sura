package com.sura.engine.rendering

import com.sura.engine.utils.Color
import com.sura.engine.utils.Vector2
import com.sura.engine.utils.Rect

interface Renderable {
    fun render(renderer: Renderer)
}

interface Renderer {
    fun begin(camera: Camera)
    fun end()
    fun drawSprite(sprite: Sprite, position: Vector2, rotation: Float, scale: Vector2, color: Color = Color.WHITE)
    fun drawRect(rect: Rect, color: Color, filled: Boolean = true)
    fun drawCircle(center: Vector2, radius: Float, color: Color, filled: Boolean = true)
    fun drawLine(a: Vector2, b: Vector2, color: Color, thickness: Float = 1f)
    fun drawText(text: String, position: Vector2, size: Float, color: Color)
    fun clear(color: Color)
}
