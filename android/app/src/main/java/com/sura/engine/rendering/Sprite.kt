package com.sura.engine.rendering

import com.sura.engine.utils.Color
import com.sura.engine.utils.Rect
import com.sura.engine.utils.Vector2

class Sprite {
    var texture: Texture? = null
    var region = Rect(0f,0f,1f,1f)
    var color = Color.WHITE
    var pivot = Vector2(0.5f, 0.5f)
    var flipX = false
    var flipY = false
    var pixelsPerUnit = 100f
}
