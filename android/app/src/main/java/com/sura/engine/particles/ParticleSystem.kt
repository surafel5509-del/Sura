package com.sura.engine.particles

import com.sura.engine.scene.Component
import com.sura.engine.utils.Color
import com.sura.engine.utils.Vector2
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin

class Particle {
    var position = Vector2()
    var velocity = Vector2()
    var acceleration = Vector2()
    var color = Color.WHITE
    var size = 4f
    var life = 1f
    var maxLife = 1f
    var rotation = 0f
    var angularVelocity = 0f
    var active = false
}

class ParticleSystem : Component() {
    var maxParticles = 200
    var emissionRate = 20f
    var lifetime = 1f
    var speed = 100f
    var speedVariance = 50f
    var sizeStart = 8f
    var sizeEnd = 0f
    var colorStart = Color.WHITE
    var colorEnd = Color(1f,1f,1f,0f)
    var gravity = Vector2(0f, 0f)
    var shape: EmitShape = EmitShape.POINT
    var emitRadius = 10f
    var isPlaying = true
    var loop = true
    var duration = 5f
    private val particles = Array(maxParticles) { Particle() }
    private var emitAccum = 0f
    private var elapsed = 0f

    enum class EmitShape { POINT, CIRCLE, CONE, BOX }

    fun emit(count: Int) {
        var emitted = 0
        for (p in particles) {
            if (emitted >= count) break
            if (p.active) continue
            resetParticle(p)
            p.active = true
            emitted++
        }
    }

    private fun resetParticle(p: Particle) {
        val t = transform
        val base = t?.position ?: Vector2()
        p.position = when (shape) {
            EmitShape.POINT -> base.copy()
            EmitShape.CIRCLE -> {
                val a = Random.nextFloat() * Math.PI.toFloat() * 2f
                val r = Random.nextFloat() * emitRadius
                Vector2(base.x + cos(a)*r, base.y + sin(a)*r)
            }
            else -> base.copy()
        }
        val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
        val sp = speed + (Random.nextFloat()-0.5f)*speedVariance
        p.velocity = Vector2(cos(angle)*sp, sin(angle)*sp)
        p.acceleration = gravity.copy()
        p.color = colorStart
        p.size = sizeStart
        p.life = lifetime * (0.8f + Random.nextFloat()*0.4f)
        p.maxLife = p.life
        p.rotation = Random.nextFloat()*360f
        p.angularVelocity = (Random.nextFloat()-0.5f)*180f
    }

    override fun update(dt: Float) {
        if (!isPlaying) return
        elapsed += dt
        if (loop || elapsed < duration) {
            emitAccum += emissionRate * dt
            while (emitAccum >= 1f) { emit(1); emitAccum -= 1f }
        }
        for (p in particles) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) { p.active = false; continue }
            p.velocity.x += p.acceleration.x * dt
            p.velocity.y += p.acceleration.y * dt
            p.position.x += p.velocity.x * dt
            p.position.y += p.velocity.y * dt
            p.rotation += p.angularVelocity * dt
            val t = 1f - (p.life / p.maxLife)
            p.size = sizeStart + (sizeEnd - sizeStart) * t
            // color lerp
            p.color.r = colorStart.r + (colorEnd.r - colorStart.r)*t
            p.color.g = colorStart.g + (colorEnd.g - colorStart.g)*t
            p.color.b = colorStart.b + (colorEnd.b - colorStart.b)*t
            p.color.a = colorStart.a + (colorEnd.a - colorStart.a)*t
        }
    }

    fun getParticles(): List<Particle> = particles.filter { it.active }
}
