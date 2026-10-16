package com.sura.engine.physics

import com.sura.engine.scene.Component
import com.sura.engine.utils.Vector2

enum class BodyType { STATIC, KINEMATIC, DYNAMIC }

class RigidBody2D : Component() {
    override fun awake() { world()?.registerBody(this) }
    override fun onDestroy() { world()?.unregisterBody(this) }

    private fun world(): PhysicsWorld? =
        if (com.sura.engine.core.Engine.isInitialized())
            com.sura.engine.core.Engine.get().services.get(PhysicsWorld::class.java) else null

    var bodyType = BodyType.DYNAMIC
    var mass = 1f
    var gravityScale = 1f
    var linearDrag = 0f
    var angularDrag = 0f
    var velocity = Vector2(0f, 0f)
    var angularVelocity = 0f
    var useGravity = true
    var isKinematic = false
    var constraints = 0 // bitmask: 1=freezeX,2=freezeY,4=freezeRot

    fun addForce(f: Vector2) {
        if (bodyType != BodyType.DYNAMIC) return
        velocity.x += f.x / mass
        velocity.y += f.y / mass
    }
    fun addImpulse(f: Vector2) { addForce(f) }
    fun applyVelocity(v: Vector2) { velocity = v.copy() }
}
