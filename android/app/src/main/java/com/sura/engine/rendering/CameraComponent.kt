package com.sura.engine.rendering

import com.sura.engine.scene.Component

/** Marks the GameObject that drives the active camera. */
class CameraComponent : Component() {
    var isMain = true
    var zoom = 1f
    var followTargetName: String = ""
    var followSmoothing = 6f
    var boundsEnabled = false
    var minX = -10000f
    var maxX = 10000f
    var minY = -10000f
    var maxY = 10000f

    fun applyTo(camera: Camera) {
        val t = transform ?: return
        camera.position.set(t.position.x, t.position.y)
        camera.zoom = zoom
    }

    fun follow(target: com.sura.engine.utils.Vector2, camera: Camera, dt: Float) {
        val t = transform ?: return
        val smoothing = (followSmoothing * dt).coerceIn(0f, 1f)
        var targetX = target.x
        var targetY = target.y
        if (boundsEnabled) {
            targetX = targetX.coerceIn(minX, maxX)
            targetY = targetY.coerceIn(minY, maxY)
        }
        t.position.x += (targetX - t.position.x) * smoothing
        t.position.y += (targetY - t.position.y) * smoothing
        applyTo(camera)
    }
}
