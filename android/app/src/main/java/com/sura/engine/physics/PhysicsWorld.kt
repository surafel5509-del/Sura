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

    var simulationPaused = false
    var pendingStep = false

    override fun update(dt: Float) {
        if (simulationPaused && !pendingStep) return
        if (pendingStep) {
            pendingStep = false
            accumulator = 0f
            fixedUpdate(fixedDeltaTime)
            return
        }
        accumulator += dt
        var steps = 0
        while (accumulator >= fixedDeltaTime && steps < 6) {
            fixedUpdate(fixedDeltaTime)
            accumulator -= fixedDeltaTime
            steps++
        }
        if (steps >= 6) accumulator = 0f
    }

    override fun fixedUpdate(dt: Float) {
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

    /** Collision events dispatched to ScriptBehaviour components on both objects. */
    private val activePairs = mutableSetOf<Long>()

    private fun pairKey(a: Collider2D, b: Collider2D): Long {
        val goA = a.gameObject ?: return 0L
        val goB = b.gameObject ?: return 0L
        val hA = System.identityHashCode(goA).toLong() and 0xFFFFFFFFL
        val hB = System.identityHashCode(goB).toLong() and 0xFFFFFFFFL
        return if (hA < hB) (hA shl 32) or hB else (hB shl 32) or hA
    }

    private fun notifyCollisions(a: Collider2D, b: Collider2D, trigger: Boolean, current: MutableSet<Long>) {
        val goA = a.gameObject ?: return
        val goB = b.gameObject ?: return
        val key = pairKey(a, b)
        current.add(key)
        if (activePairs.contains(key)) return
        for (component in goA.components) {
            if (component is com.sura.engine.scripting.ScriptBehaviour) {
                if (trigger) component.onTriggerEnter(goB) else component.onCollisionEnter(goB)
            }
        }
        for (component in goB.components) {
            if (component is com.sura.engine.scripting.ScriptBehaviour) {
                if (trigger) component.onTriggerEnter(goA) else component.onCollisionEnter(goA)
            }
        }
    }

    private fun resolveCollisions() {
        val current = mutableSetOf<Long>()
        for (i in colliders.indices) {
            for (j in i + 1 until colliders.size) {
                val a = colliders[i]
                val b = colliders[j]
                if (a.gameObject?.active != true || b.gameObject?.active != true) continue
                val trigger = a.isTrigger || b.isTrigger
                val ra = a.getBounds()
                val rb = b.getBounds()
                if (!ra.intersects(rb)) continue
                notifyCollisions(a, b, trigger, current)
                if (trigger) continue
                separate(a, b, ra, rb)
            }
        }
        activePairs.clear()
        activePairs.addAll(current)
    }

    /** Pushes two overlapping colliders apart along the axis with the smallest overlap. */
    private fun separate(a: Collider2D, b: Collider2D, ra: com.sura.engine.utils.Rect, rb: com.sura.engine.utils.Rect) {
        val bodyA = a.gameObject?.getComponent<RigidBody2D>()
        val bodyB = b.gameObject?.getComponent<RigidBody2D>()
        val staticA = bodyA == null || bodyA.bodyType == BodyType.STATIC
        val staticB = bodyB == null || bodyB.bodyType == BodyType.STATIC
        if (staticA && staticB) return
        val totalMass = (if (bodyA != null && !staticA) bodyA.mass else 0f) + (if (bodyB != null && !staticB) bodyB.mass else 0f)
        if (totalMass <= 0f) return
        val shareA = if (bodyA != null && !staticA) bodyA.mass / totalMass else 0f
        val shareB = if (bodyB != null && !staticB) bodyB.mass / totalMass else 0f

        val overlapX = min(ra.right(), rb.right()) - max(ra.left(), rb.left())
        val overlapY = min(ra.bottom(), rb.bottom()) - max(ra.top(), rb.top())
        val restitution = max(a.restitution, b.restitution)

        if (overlapX < overlapY) {
            val direction = if (ra.center().x < rb.center().x) -1f else 1f
            if (bodyA != null && !staticA) {
                bodyA.transform?.let { it.position.x += direction * overlapX * shareA }
                bodyA.velocity.x = -bodyA.velocity.x * restitution
            }
            if (bodyB != null && !staticB) {
                bodyB.transform?.let { it.position.x -= direction * overlapX * shareB }
                bodyB.velocity.x = -bodyB.velocity.x * restitution
            }
        } else {
            val direction = if (ra.center().y < rb.center().y) -1f else 1f
            if (bodyA != null && !staticA) {
                bodyA.transform?.let { it.position.y += direction * overlapY * shareA }
                bodyA.velocity.y = -bodyA.velocity.y * restitution
            }
            if (bodyB != null && !staticB) {
                bodyB.transform?.let { it.position.y -= direction * overlapY * shareB }
                bodyB.velocity.y = -bodyB.velocity.y * restitution
            }
        }
    }

    // ------------------------------------------------------------------ queries

    /** Ray against every collider bounding box, returns the closest hit. */
    fun raycast(origin: Vector2, direction: Vector2, maxDistance: Float): RaycastHit? {
        val length = direction.length()
        if (length <= 0.0001f) return null
        val dirX = direction.x / length
        val dirY = direction.y / length
        var best: RaycastHit? = null
        var bestDistance = maxDistance
        for (collider in colliders) {
            if (collider.gameObject?.active != true) continue
            val bounds = collider.getBounds()
            val hit = rayVsRect(origin, dirX, dirY, bounds)
            if (hit != null && hit.distance in 0f..bestDistance) {
                bestDistance = hit.distance
                best = RaycastHit(collider, hit.point, hit.normal, hit.distance)
            }
        }
        return best
    }

    private class RectHit(val point: Vector2, val normal: Vector2, val distance: Float)

    private fun rayVsRect(origin: Vector2, dirX: Float, dirY: Float, rect: com.sura.engine.utils.Rect): RectHit? {
        var tMin = 0f
        var tMax = Float.MAX_VALUE
        var normalX = 0f
        var normalY = 0f

        if (kotlin.math.abs(dirX) < 0.00001f) {
            if (origin.x < rect.x || origin.x > rect.x + rect.w) return null
        } else {
            var t1 = (rect.x - origin.x) / dirX
            var t2 = (rect.x + rect.w - origin.x) / dirX
            var sign = -1f
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp; sign = 1f }
            if (t1 > tMin) { normalX = sign; normalY = 0f }
            tMin = max(tMin, t1)
            tMax = min(tMax, t2)
            if (tMin > tMax) return null
        }
        if (kotlin.math.abs(dirY) < 0.00001f) {
            if (origin.y < rect.y || origin.y > rect.y + rect.h) return null
        } else {
            var t1 = (rect.y - origin.y) / dirY
            var t2 = (rect.y + rect.h - origin.y) / dirY
            var sign = -1f
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp; sign = 1f }
            if (t1 > tMin) { normalX = 0f; normalY = sign }
            tMin = max(tMin, t1)
            tMax = min(tMax, t2)
            if (tMin > tMax) return null
        }
        val point = Vector2(origin.x + dirX * tMin, origin.y + dirY * tMin)
        return RectHit(point, Vector2(normalX, normalY), tMin)
    }

    /** All colliders whose bounding box contains the point. */
    fun queryPoint(point: Vector2): List<Collider2D> = colliders.filter { it.getBounds().contains(point.x, point.y) }

    fun queryRect(rect: com.sura.engine.utils.Rect): List<Collider2D> = colliders.filter { it.getBounds().intersects(rect) }

    fun bodyCount(): Int = bodies.size
    fun colliderCount(): Int = colliders.size

    data class RaycastHit(val collider: Collider2D, val point: Vector2, val normal: Vector2, val distance: Float)
}
