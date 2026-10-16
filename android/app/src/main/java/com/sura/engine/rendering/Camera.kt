package com.sura.engine.rendering

import com.sura.engine.utils.Vector2
import com.sura.engine.utils.Matrix3

class Camera {
    var position = Vector2(0f, 0f)
    var zoom = 1f
    var rotation = 0f
    var viewportWidth = 1920f
    var viewportHeight = 1080f
    var orthographic = true
    var near = 0.1f
    var far = 1000f

    fun worldToScreen(world: Vector2): Vector2 {
        val x = (world.x - position.x) * zoom + viewportWidth / 2f
        val y = (world.y - position.y) * zoom + viewportHeight / 2f
        return Vector2(x, y)
    }
    fun screenToWorld(screen: Vector2): Vector2 {
        val x = (screen.x - viewportWidth / 2f) / zoom + position.x
        val y = (screen.y - viewportHeight / 2f) / zoom + position.y
        return Vector2(x, y)
    }
    fun viewMatrix(): Matrix3 {
        val m = Matrix3()
        m.identity()
        return m
    }
}
