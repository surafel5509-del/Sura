package com.sura.engine.scene

import com.sura.engine.utils.Logger

class GameObject(var name: String = "GameObject") {
    val transform = Transform()
    val components = mutableListOf<Component>()
    var tag = "Untagged"
    var layer = 0
    var active = true
    var scene: Scene? = null

    inline fun <reified T : Component> addComponent(): T {
        val c = T::class.java.getDeclaredConstructor().newInstance()
        c.gameObject = this
        components.add(c)
        c.awake()
        return c
    }
    fun addComponent(c: Component): Component { c.gameObject = this; components.add(c); c.awake(); return c }
    inline fun <reified T : Component> getComponent(): T? = components.filterIsInstance<T>().firstOrNull()
    inline fun <reified T : Component> getComponents(): List<T> = components.filterIsInstance<T>()
    inline fun <reified T : Component> hasComponent(): Boolean = components.any { it is T }
    fun removeComponent(c: Component) { c.onDestroy(); components.remove(c); c.gameObject = null }
    fun update(dt: Float) {
        if (!active) return
        for (c in components.toList()) if (c.enabled) c.update(dt)
    }
    fun fixedUpdate(dt: Float) {
        if (!active) return
        for (c in components.toList()) if (c.enabled) c.fixedUpdate(dt)
    }
    fun lateUpdate(dt: Float) {
        if (!active) return
        for (c in components.toList()) if (c.enabled) c.lateUpdate(dt)
    }
    override fun toString(): String = "GameObject($name)"
}
