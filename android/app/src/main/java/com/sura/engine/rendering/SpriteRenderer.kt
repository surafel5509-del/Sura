package com.sura.engine.rendering

import com.sura.engine.scene.Component
import com.sura.engine.utils.Color
import com.sura.engine.utils.Vector2

class SpriteRenderer : Component() {
    var sprite: Sprite? = null
    var color: Color = Color.WHITE
    var size = Vector2(1f, 1f)
    var sortingLayer = 0
    var orderInLayer = 0
    override fun update(dt: Float) {}
}
