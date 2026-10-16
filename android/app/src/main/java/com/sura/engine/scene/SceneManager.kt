package com.sura.engine.scene

import com.sura.engine.core.EngineService
import com.sura.engine.utils.Logger

class SceneManager : EngineService {
    val scenes = linkedMapOf<String, Scene>()
    var activeScene: Scene? = null
    private set
    val onSceneChanged = com.sura.engine.utils.Event<Scene?>()

    fun createScene(name: String): Scene {
        val s = Scene(name)
        scenes[name] = s
        return s
    }
    fun loadScene(name: String): Scene? {
        val s = scenes[name] ?: createScene(name)
        activeScene?.unload()
        activeScene = s
        s.load()
        onSceneChanged.invoke(s)
        return s
    }
    fun loadScene(scene: Scene) {
        activeScene?.unload()
        activeScene = scene
        if (scene.name !in scenes) scenes[scene.name] = scene
        scene.load()
        onSceneChanged.invoke(scene)
    }
    fun unloadScene(name: String) {
        scenes[name]?.unload()
        if (activeScene?.name == name) { activeScene = null; onSceneChanged.invoke(null) }
        scenes.remove(name)
    }
    override fun update(dt: Float) { activeScene?.update(dt) }
    override fun fixedUpdate(dt: Float) { activeScene?.fixedUpdate(dt) }
}
