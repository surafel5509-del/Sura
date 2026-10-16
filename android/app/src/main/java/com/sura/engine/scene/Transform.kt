package com.sura.engine.scene

import com.sura.engine.utils.Vector2
import com.sura.engine.utils.Vector3

class Transform {
    var position = Vector2(0f, 0f)
    var rotation = 0f
    var scale = Vector2(1f, 1f)
    var zOrder = 0
    var parent: Transform? = null
    val children = mutableListOf<Transform>()

    fun worldPosition(): Vector2 {
        var p = position.copy()
        var cur = parent
        while (cur != null) {
            // simple: rotate+scale not fully; approximate
            p.x = cur.position.x + p.x * cur.scale.x
            p.y = cur.position.y + p.y * cur.scale.y
            cur = cur.parent
        }
        return p
    }
    fun addChild(t: Transform) { t.parent = this; children.add(t) }
    fun removeChild(t: Transform) { children.remove(t); t.parent = null }
}
