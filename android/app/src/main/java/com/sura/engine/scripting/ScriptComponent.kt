package com.sura.engine.scripting

import com.sura.engine.core.Logger
import com.sura.engine.scene.Component
import com.sura.engine.scene.GameObject
import com.sura.engine.utils.Vector2

/**
 * Attaches a SuraScript source to a GameObject. Engine values (position, rotation, scale, dt)
 * are bound as script variables and the script may declare any of the lifecycle functions:
 *
 *   function start() { }
 *   function update(dt) { }
 *   function fixedUpdate(dt) { }
 *   function onCollision(other) { }
 *   function onDestroy() { }
 */
open class ScriptBehaviour : Component() {
    var source: String = ""
    var scriptName: String = "Script"
    var autoReloadOnError = false

    private var instance: ScriptInstance? = null
    private var started = false

    val lastError: String? get() = instance?.lastError

    override fun awake() {
        compile()
    }

    fun compile(): Boolean {
        if (source.isBlank()) return false
        val script = ScriptInstance(scriptName, source)
        bindEngineValues(script.interpreter)
        instance = script
        val ok = script.load()
        if (!ok) Logger.w("$scriptName failed to load: ${script.lastError}")
        return ok
    }

    private inline fun withTransform(block: (com.sura.engine.scene.Transform) -> Unit) {
        val obj = gameObject ?: return
        block(obj.transform)
    }

    private fun bindEngineValues(interpreter: Interpreter) {
        interpreter.printHandler = { message -> Logger.i("[$scriptName] $message") }
        interpreter.bindVariable("x", { gameObject?.transform?.position?.x?.toDouble() ?: 0.0 },
            { v -> withTransform { it.position.x = (v as? Number)?.toFloat() ?: 0f } })
        interpreter.bindVariable("y", { gameObject?.transform?.position?.y?.toDouble() ?: 0.0 },
            { v -> withTransform { it.position.y = (v as? Number)?.toFloat() ?: 0f } })
        interpreter.bindVariable("rotation", { gameObject?.transform?.rotation?.toDouble() ?: 0.0 },
            { v -> withTransform { it.rotation = (v as? Number)?.toFloat() ?: 0f } })
        interpreter.bindVariable("scaleX", { gameObject?.transform?.scale?.x?.toDouble() ?: 1.0 },
            { v -> withTransform { it.scale.x = (v as? Number)?.toFloat() ?: 1f } })
        interpreter.bindVariable("scaleY", { gameObject?.transform?.scale?.y?.toDouble() ?: 1.0 },
            { v -> withTransform { it.scale.y = (v as? Number)?.toFloat() ?: 1f } })
        interpreter.bindVariable("active", { gameObject?.active ?: false },
            { v -> gameObject?.let { go -> go.active = v == true || (v as? Number)?.toInt() == 1 } })
        interpreter.bindVariable("time", { com.sura.engine.core.Engine.get().time.totalTime.toDouble() })
    }

    /** Registers an engine function callable from the script, e.g. `spawn("Enemy", x, y)`. */
    fun registerFunction(name: String, fn: (List<Any?>) -> Any?) {
        instance?.interpreter?.registerNative(name, fn)
    }

    override fun start() {
        if (instance == null) compile()
        if (!started) {
            instance?.callSafe("start")
            started = true
        }
    }

    override fun update(dt: Float) {
        instance?.callSafe("update", listOf(dt.toDouble()))
    }

    override fun fixedUpdate(dt: Float) {
        instance?.callSafe("fixedUpdate", listOf(dt.toDouble()))
    }

    override fun onDestroy() {
        instance?.callSafe("onDestroy")
    }

    fun onCollisionEnter(other: GameObject) {
        instance?.callSafe("onCollision", listOf<Any?>(other.name))
    }

    fun onTriggerEnter(other: GameObject) {
        instance?.callSafe("onTriggerEnter", listOf<Any?>(other.name))
    }

    fun stringValue(name: String): String = instance?.interpreter?.valueToString(instance?.interpreter?.getGlobal(name)) ?: ""

    override fun toString(): String = "ScriptBehaviour($scriptName)"
}

/** Coroutine-ish scheduler used by gameplay scripts for timers. */
class CoroutineRunner {
    private class Routine(val id: Int, val delay: Float, val interval: Float, var repeat: Boolean, val action: () -> Unit)

    private val routines = mutableListOf<Routine>()
    private var nextId = 1

    fun start(delay: Float, action: () -> Unit): Int {
        val id = nextId++
        routines.add(Routine(id, delay, delay, false, action))
        return id
    }

    fun repeatEvery(interval: Float, action: () -> Unit): Int {
        val id = nextId++
        routines.add(Routine(id, interval, interval, true, action))
        return id
    }

    fun stop(id: Int) { routines.removeAll { it.id == id } }
    fun stopAll() { routines.clear() }
    val count: Int get() = routines.size

    fun update(dt: Float) {
        val finished = mutableListOf<Routine>()
        for (routine in routines) {
            routine.delay -= dt
            if (routine.delay <= 0f) {
                routine.action()
                if (routine.repeat) routine.delay = routine.interval else finished.add(routine)
            }
        }
        routines.removeAll(finished)
    }
}

/** Global script registry: hosts gameplay scripts loaded from the project asset folder. */
class ScriptEngine {
    val globals = mutableMapOf<String, Any?>()
    val scripts = mutableMapOf<String, ScriptInstance>()
    val runner = CoroutineRunner()
    var lastError: String? = null

    fun loadScript(name: String, source: String): Boolean {
        val instance = ScriptInstance(name, source)
        val ok = instance.load()
        scripts[name] = instance
        if (!ok) lastError = instance.lastError
        return ok
    }

    fun runSnippet(code: String): Any? {
        val interpreter = Interpreter()
        return try {
            interpreter.execute(code)
            lastError = null
            interpreter.getGlobal("result")
        } catch (e: ScriptException) {
            lastError = e.message
            null
        }
    }

    fun callFunction(name: String, vararg args: Any?): Any? {
        for (script in scripts.values) {
            if (script.interpreter.hasFunction(name)) return script.call(name, args.toList())
        }
        return null
    }

    fun registerFunction(name: String, fn: (Array<Any?>) -> Any?) { globals[name] = fn }
    fun update(dt: Float) { runner.update(dt) }
    fun hasErrors(): Boolean = scripts.values.any { it.hasError }
}

/** Small helper for building Vector2 values inside scripts. */
object ScriptMath {
    fun toVector(x: Any?, y: Any?): Vector2 =
        Vector2((x as? Number)?.toFloat() ?: 0f, (y as? Number)?.toFloat() ?: 0f)
}
