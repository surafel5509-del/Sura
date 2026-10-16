package com.sura.engine.scripting

import com.sura.engine.scene.Component
import com.sura.engine.scene.GameObject

abstract class Script : Component() {
    open fun onCollisionEnter(other: GameObject) {}
    open fun onTriggerEnter(other: GameObject) {}
    open fun onTriggerExit(other: GameObject) {}
}

class CoroutineRunner {
    data class Routine(val id: Int, val block: suspend () -> Unit)
    private val routines = mutableListOf<Routine>()
    fun start(block: suspend () -> Unit): Int { val id = routines.size; routines.add(Routine(id, block)); return id }
    fun stop(id: Int) { routines.removeAll { it.id == id } }
    fun update(dt: Float) {}
}

class ScriptEngine {
    val globals = mutableMapOf<String, Any?>()
    fun execute(code: String): Any? = null
    fun callFunction(name: String, vararg args: Any?): Any? = null
    fun registerFunction(name: String, fn: (Array<Any?>) -> Any?) { globals[name] = fn }
}
