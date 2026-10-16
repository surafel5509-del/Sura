package com.sura.engine.rendering

import com.sura.engine.scene.Component
import com.sura.engine.utils.Color
import com.sura.engine.utils.Vector2

class SpriteRenderer : Component() {
    var sprite: Sprite? = null
    var texturePath: String = ""
    var color: Color = Color.WHITE
    var size = Vector2(1f, 1f)
    var sortingLayer = 0
    var orderInLayer = 0
    var loadedTexture: com.sura.engine.rendering.Texture? = null

    /** Pixel size used by the runtime when no explicit size is set. */
    fun pixelSize(): Vector2 {
        val tex = loadedTexture ?: sprite?.texture
        return if (tex != null && tex.width > 1) Vector2(tex.width.toFloat(), tex.height.toFloat()) else Vector2(32f, 32f)
    }

    override fun update(dt: Float) {}
}
