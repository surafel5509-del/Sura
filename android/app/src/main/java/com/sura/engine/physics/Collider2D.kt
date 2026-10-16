package com.sura.engine.physics

import com.sura.engine.scene.Component
import com.sura.engine.utils.Vector2

abstract class Collider2D : Component() {
    var isTrigger = false
    var offset = Vector2(0f, 0f)
    var friction = 0.5f
    var restitution = 0f
    var density = 1f
    abstract fun getBounds(): com.sura.engine.utils.Rect
}

class BoxCollider2D : Collider2D() {
    var size = Vector2(1f, 1f)
    override fun getBounds(): com.sura.engine.utils.Rect {
        val t = transform ?: return com.sura.engine.utils.Rect()
        val pos = t.position
        return com.sura.engine.utils.Rect(pos.x + offset.x - size.x/2f, pos.y + offset.y - size.y/2f, size.x, size.y)
    }
}

class CircleCollider2D : Collider2D() {
    var radius = 0.5f
    override fun getBounds(): com.sura.engine.utils.Rect {
        val t = transform ?: return com.sura.engine.utils.Rect()
        val pos = t.position
        return com.sura.engine.utils.Rect(pos.x + offset.x - radius, pos.y + offset.y - radius, radius*2f, radius*2f)
    }
    fun center(): Vector2 {
        val t = transform ?: return Vector2()
        return Vector2(t.position.x + offset.x, t.position.y + offset.y)
    }
}
