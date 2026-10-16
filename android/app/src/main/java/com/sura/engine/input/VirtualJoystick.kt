package com.sura.engine.input

import com.sura.engine.utils.Vector2

class VirtualJoystick {
    var center = Vector2()
    var radius = 80f
    var knob = Vector2()
    var isDragging = false
    var pointerId = -1
    val direction: Vector2 get() {
        val d = Vector2(knob.x - center.x, knob.y - center.y)
        val l = d.length()
        if (l > radius) { d.scale(radius / l) }
        return if (radius > 0) Vector2(d.x / radius, d.y / radius) else Vector2.ZERO
    }
    var magnitude: Float = 0f
        get() = (Vector2(knob.x-center.x, knob.y-center.y).length() / radius).coerceIn(0f,1f)

    fun onTouchDown(x: Float, y: Float, id: Int) {
        if (Vector2(x,y).distance(center) <= radius * 1.5f) {
            isDragging = true; pointerId = id; knob.set(x,y)
        }
    }
    fun onTouchMove(x: Float, y: Float, id: Int) {
        if (isDragging && id == pointerId) knob.set(x,y)
    }
    fun onTouchUp(id: Int) {
        if (id == pointerId) { isDragging = false; pointerId = -1; knob = center.copy() }
    }
}
