package com.sura.engine.scene

import com.sura.engine.utils.Logger

class Scene(val name: String = "Untitled") {
    val rootObjects = mutableListOf<GameObject>()
    val allObjects = mutableListOf<GameObject>()
    var isLoaded = false

    fun createGameObject(name: String = "GameObject"): GameObject {
        val go = GameObject(name)
        go.scene = this
        allObjects.add(go)
        rootObjects.add(go)
        return go
    }
    fun destroyGameObject(go: GameObject) {
        go.components.forEach { it.onDestroy() }
        allObjects.remove(go)
        rootObjects.remove(go)
        go.scene = null
    }
    fun findByName(name: String): GameObject? = allObjects.firstOrNull { it.name == name }
    fun findByTag(tag: String): List<GameObject> = allObjects.filter { it.tag == tag }
    fun update(dt: Float) {
        for (go in allObjects.toList()) go.update(dt)
        for (go in allObjects.toList()) go.lateUpdate(dt)
    }
    fun fixedUpdate(dt: Float) {
        for (go in allObjects.toList()) go.fixedUpdate(dt)
    }
    fun clear() {
        for (go in allObjects.toList()) go.components.forEach { it.onDestroy() }
        allObjects.clear(); rootObjects.clear()
    }
    fun load() { isLoaded = true; Logger.i("Scene loaded: $name") }
    fun unload() { isLoaded = false; clear() }
}
