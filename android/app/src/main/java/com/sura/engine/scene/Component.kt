package com.sura.engine.scene

abstract class Component {
    var gameObject: GameObject? = null
    var enabled = true
    open fun awake() {}
    open fun start() {}
    open fun update(dt: Float) {}
    open fun fixedUpdate(dt: Float) {}
    open fun lateUpdate(dt: Float) {}
    open fun onDestroy() {}
    open fun onEnable() {}
    open fun onDisable() {}
    val transform: Transform? get() = gameObject?.transform
}
