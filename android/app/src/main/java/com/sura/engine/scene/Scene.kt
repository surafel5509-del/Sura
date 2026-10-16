package com.sura.engine.scene

import com.sura.engine.core.Logger

class Scene(val name: String = "Untitled") {
    val rootObjects = mutableListOf<GameObject>()
    val allObjects = mutableListOf<GameObject>()
    var isLoaded = false

    fun createGameObject(name: String = "GameObject", parent: GameObject? = null): GameObject {
        val go = GameObject(name)
        go.scene = this
        allObjects.add(go)
        if (parent == null) {
            rootObjects.add(go)
        } else {
            go.parent = parent
            parent.children.add(go)
            parent.transform.addChild(go.transform)
        }
        return go
    }
    /** Adds an already built GameObject (used when loading scenes from disk). */
    fun adopt(go: GameObject): GameObject {
        go.scene = this
        if (!allObjects.contains(go)) allObjects.add(go)
        if (go.parent == null && !rootObjects.contains(go)) rootObjects.add(go)
        return go
    }

    fun destroyGameObject(go: GameObject) {
        for (child in go.children.toList()) destroyGameObject(child)
        go.components.forEach { it.onDestroy() }
        go.parent?.children?.remove(go)
        go.parent?.transform?.removeChild(go.transform)
        allObjects.remove(go)
        rootObjects.remove(go)
        go.scene = null
    }

    fun objectCount(): Int = allObjects.size
    fun componentCount(): Int = allObjects.sumOf { it.components.size }
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
