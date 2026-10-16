package com.sura.engine.rendering

import com.sura.engine.scene.Component
import com.sura.engine.utils.Color

/** Simple 2D point light: no shadow casting, but cheap and perfect for mobile. */
class Light2D : Component() {
    var color = Color(1f, 0.95f, 0.8f, 1f)
    var radius = 240f
    var intensity = 1f
    var flicker = false
    var flickerSpeed = 6f
    var flickerAmount = 0.15f
    private var time = 0f

    override fun update(dt: Float) {
        if (flicker) time += dt * flickerSpeed
    }

    /** Intensity including flicker, evaluated per frame by the renderer. */
    fun currentIntensity(): Float {
        if (!flicker) return intensity
        return (intensity + kotlin.math.sin(time.toDouble()).toFloat() * flickerAmount).coerceIn(0f, 2f)
    }
}
