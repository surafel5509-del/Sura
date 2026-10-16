package com.sura.engine.editor

import com.sura.engine.debug.Profiler
import com.sura.engine.project.Project
import com.sura.engine.scene.GameObject
import com.sura.engine.scene.Scene

/** Which transform handle the user is dragging in the scene view. */
enum class EditorTool { SELECT, MOVE, ROTATE, SCALE, TILE_PAINT, TILE_ERASE, PAN, RECT }

/** Single source of truth shared by every editor panel. */
class EditorState {
    var project: Project? = null
    var scene: Scene? = null
    var selection: GameObject? = null
    var tool: EditorTool = EditorTool.SELECT
    var isPlaying = false
    var isPaused = false
    var isDirty = false
    var snapToGrid = true
    var snapSize = 16f
    var showGrid = true
    var showGizmos = true
    var showStats = true
    var showColliders = false

    val undoStack = UndoStack()
    val profiler = Profiler()
    val consoleLines = mutableListOf<ConsoleLine>()
    var brushTileId = 0

    data class ConsoleLine(val level: Level, val message: String, val time: Long = System.currentTimeMillis()) {
        enum class Level { INFO, WARN, ERROR, COMMAND, DEBUG }
    }

    val listeners = mutableListOf<() -> Unit>()

    fun notifyChanged() { for (listener in listeners.toList()) listener() }
    fun subscribe(listener: () -> Unit) { listeners.add(listener) }
    fun unsubscribe(listener: () -> Unit) { listeners.remove(listener) }

    fun select(gameObject: GameObject?) {
        selection = gameObject
        notifyChanged()
    }

    fun log(message: String, level: EditorState.ConsoleLine.Level = EditorState.ConsoleLine.Level.INFO) {
        consoleLines.add(EditorState.ConsoleLine(level, message))
        if (consoleLines.size > 500) consoleLines.removeAt(0)
        notifyChanged()
    }

    fun logCommand(message: String) = log("> $message", EditorState.ConsoleLine.Level.COMMAND)

    fun createObject(name: String = "GameObject"): GameObject? {
        val s = scene ?: return null
        val go = s.createGameObject(uniqueName(name))
        isDirty = true
        undoStack.push("Create $name", { s.destroyGameObject(go) }, { s.adopt(go) })
        select(go)
        return go
    }

    fun deleteSelected() {
        val go = selection ?: return
        val s = scene ?: return
        s.destroyGameObject(go)
        isDirty = true
        selection = null
        undoStack.push("Delete ${go.name}", { s.adopt(go) }, { s.destroyGameObject(go) })
        notifyChanged()
    }

    fun uniqueName(base: String): String {
        val s = scene ?: return base
        if (s.findByName(base) == null) return base
        var index = 1
        while (s.findByName("$base $index") != null) index++
        return "$base $index"
    }

    fun updateStats() {
        profiler.drawCalls = 0
    }

    fun reset() {
        project = null
        scene = null
        selection = null
        isPlaying = false
        isPaused = false
        isDirty = false
        undoStack.clear()
        consoleLines.clear()
    }
}
