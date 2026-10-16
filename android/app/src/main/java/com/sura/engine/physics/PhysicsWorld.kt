package com.sura.engine.physics

import com.sura.engine.core.EngineService
import com.sura.engine.scene.Scene
import com.sura.engine.scene.GameObject
import com.sura.engine.utils.Vector2
import kotlin.math.max
import kotlin.math.min

class PhysicsWorld : EngineService {
    var gravity = Vector2(0f, -9.81f)
    var fixedDeltaTime = 1f / 60f
    private var accumulator = 0f
    val bodies = mutableListOf<RigidBody2D>()
    val colliders = mutableListOf<Collider2D>()

    fun registerBody(b: RigidBody2D) { if (b !in bodies) bodies.add(b) }
    fun registerCollider(c: Collider2D) { if (c !in colliders) colliders.add(c) }
    fun unregisterBody(b: RigidBody2D) { bodies.remove(b) }
    fun unregisterCollider(c: Collider2D) { colliders.remove(c) }

    override fun update(dt: Float) {
        accumulator += dt
        while (accumulator >= fixedDeltaTime) {
            fixedUpdate(fixedDeltaTime)
            accumulator -= fixedDeltaTime
        }
    }

    fun fixedUpdate(dt: Float) {
        // integrate
        for (b in bodies) {
            if (b.bodyType != BodyType.DYNAMIC) continue
            if (b.useGravity) {
                b.velocity.x += gravity.x * b.gravityScale * dt
                b.velocity.y += gravity.y * b.gravityScale * dt
            }
            // drag
            b.velocity.x *= (1f - b.linearDrag * dt).coerceAtLeast(0f)
            b.velocity.y *= (1f - b.linearDrag * dt).coerceAtLeast(0f)
            val t = b.transform ?: continue
            if ((b.constraints and 1) == 0) t.position.x += b.velocity.x * dt
            if ((b.constraints and 2) == 0) t.position.y += b.velocity.y * dt
            if ((b.constraints and 4) == 0) t.rotation += b.angularVelocity * dt
        }
        // collisions
        resolveCollisions()
    }

    private fun resolveCollisions() {
        for (i in colliders.indices) {
            for (j in i+1 until colliders.size) {
                val a = colliders[i]; val b = colliders[j]
                if (a.isTrigger || b.isTrigger) continue
                if (a.gameObject?.active != true || b.gameObject?.active != true) continue
                val ra = a.getBounds(); val rb = b.getBounds()
                if (ra.intersects(rb)) {
                    // simple push apart along minimal axis
                    val overlapX = min(ra.right(), rb.right()) - max(ra.left(), rb.left())
                    val overlapY = min(ra.bottom(), rb.bottom()) - max(ra.top(), rb.top())
                    val rbA = a.gameObject?.getComponent<RigidBody2D>()
                    val rbB = b.gameObject?.getComponent<RigidBody2D>()
                    if (overlapX < overlapY) {
                        val dir = if (ra.center().x < rb.center().x) -1f else 1f
                        val push = overlapX / 2f
                        if (rbA != null && rbA.bodyType == BodyType.DYNAMIC) {
                            rbA.transform?.position?.x = (rbA.transform?.position?.x ?: 0f) + dir * push
                            rbA.velocity.x = 0f
                        }
                        if (rbB != null && rbB.bodyType == BodyType.DYNAMIC) {
                            rbB.transform?.position?.x = (rbB.transform?.position?.x ?: 0f) - dir * push
                            rbB.velocity.x = 0f
                        }
                    } else {
                        val dir = if (ra.center().y < rb.center().y) -1f else 1f
                        val push = overlapY / 2f
                        if (rbA != null && rbA.bodyType == BodyType.DYNAMIC) {
                            rbA.transform?.position?.y = (rbA.transform?.position?.y ?: 0f) + dir * push
                            if (dir > 0) rbA.velocity.y = 0f
                        }
                        if (rbB != null && rbB.bodyType == BodyType.DYNAMIC) {
                            rbB.transform?.position?.y = (rbB.transform?.position?.y ?: 0f) - dir * push
                            if (dir < 0) rbB.velocity.y = 0f
                        }
                    }
                }
            }
        }
    }

    fun raycast(origin: Vector2, dir: Vector2, maxDist: Float): RaycastHit? {
        // simple: check colliders bounding
        var best: RaycastHit? = null
        var bestT = maxDist
        for (c in colliders) {
            val b = c.getBounds()
            // slab method approximate
            // skip
        }
        return best
    }

    data class RaycastHit(val collider: Collider2D, val point: Vector2, val normal: Vector2, val distance: Float)
}
