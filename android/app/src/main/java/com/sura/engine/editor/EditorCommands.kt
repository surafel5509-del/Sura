package com.sura.engine.editor

import com.sura.engine.core.Engine
import com.sura.engine.scene.Scene

/** Parses and executes console commands. Everything works offline on the device. */
object EditorCommands {

    data class Result(val ok: Boolean, val message: String)

    fun execute(raw: String, state: EditorState): Result {
        val parts = raw.trim().split(Regex("\\s+"))
        if (parts.isEmpty() || parts[0].isEmpty()) return Result(true, "")
        val command = parts[0].lowercase()
        val args = parts.drop(1)
        return try {
            when (command) {
                "help", "?" -> Result(true, HELP)
                "clear" -> { state.consoleLines.clear(); Result(true, "console cleared") }
                "list", "objects" -> {
                    val scene = state.scene ?: return Result(false, "no scene loaded")
                    Result(true, scene.allObjects.joinToString(", ") { it.name })
                }
                "spawn", "new" -> {
                    val name = args.firstOrNull() ?: "GameObject"
                    val go = state.createObject(name)
                    Result(go != null, if (go == null) "no scene loaded" else "created ${go.name}")
                }
                "delete", "destroy" -> {
                    if (state.selection == null) return Result(false, "nothing selected")
                    val name = state.selection?.name
                    state.deleteSelected()
                    Result(true, "deleted $name")
                }
                "select" -> {
                    val name = args.firstOrNull() ?: return Result(false, "usage: select <name>")
                    val go = state.scene?.findByName(name) ?: return Result(false, "object '$name' not found")
                    state.select(go)
                    Result(true, "selected $name")
                }
                "move" -> {
                    val go = state.selection ?: return Result(false, "nothing selected")
                    val x = args.getOrNull(0)?.toFloatOrNull() ?: return Result(false, "usage: move <x> <y>")
                    val y = args.getOrNull(1)?.toFloatOrNull() ?: 0f
                    val px = go.transform.position.x
                    val py = go.transform.position.y
                    go.transform.position.set(x, y)
                    state.undoStack.push("Console move", { go.transform.position.set(px, py) }, { go.transform.position.set(x, y) })
                    state.notifyChanged()
                    Result(true, "moved ${go.name} to ($x, $y)")
                }
                "timescale", "time" -> {
                    val value = args.firstOrNull()?.toFloatOrNull() ?: return Result(false, "usage: timescale <value>")
                    Engine.get().time.deltaTime = value
                    Result(true, "time scale set to $value")
                }
                "grid" -> {
                    val value = args.firstOrNull()?.toIntOrNull()
                    state.showGrid = if (value != null) value != 0 else !state.showGrid
                    state.notifyChanged()
                    Result(true, "grid ${if (state.showGrid) "on" else "off"}")
                }
                "snap" -> {
                    val value = args.firstOrNull()?.toFloatOrNull()
                    if (value != null) { state.snapSize = value; state.snapToGrid = true }
                    else state.snapToGrid = !state.snapToGrid
                    state.notifyChanged()
                    Result(true, "snap ${if (state.snapToGrid) "on (${state.snapSize}px)" else "off"}")
                }
                "stats" -> {
                    val scene = state.scene
                    Result(true, "objects=${scene?.allObjects?.size ?: 0} fps=${state.profiler.fps.toInt()} frame=${"%.2f".format(state.profiler.frameMs)}ms mem=${"%.1f".format(state.profiler.memoryMb)}MB")
                }
                "scene" -> {
                    val sub = args.firstOrNull()?.lowercase() ?: "info"
                    when (sub) {
                        "new" -> {
                            val name = args.getOrNull(1) ?: "Scene"
                            val scene = Scene(name)
                            state.scene = scene
                            state.select(null)
                            state.log("scene '$name' created")
                            Result(true, "new scene $name")
                        }
                        "save" -> {
                            val project = state.project ?: return Result(false, "no project open")
                            val scene = state.scene ?: return Result(false, "no scene loaded")
                            val ok = com.sura.engine.project.ProjectManager(com.sura.engine.platform.Platform(Engine.get().context)).saveScene(project, scene)
                            Result(ok, if (ok) "saved ${scene.name}" else "save failed")
                        }
                        else -> Result(true, "scene '${state.scene?.name ?: "-"}' has ${state.scene?.allObjects?.size ?: 0} objects")
                    }
                }
                "script" -> {
                    val code = raw.substringAfter("script").trim()
                    if (code.isEmpty()) return Result(false, "usage: script <expression>")
                    val interpreter = com.sura.engine.scripting.Interpreter()
                    val result = interpreter.execute(code)
                    Result(true, "executed: " + interpreter.globalsSnapshot().entries.joinToString(", ") { "${it.key}=${interpreter.valueToString(it.value)}" })
                }
                "undo" -> Result(state.undoStack.undo(), if (state.undoStack.canUndo) "undone" else "nothing to undo")
                "redo" -> Result(state.undoStack.redo(), "redo")
                "play" -> { state.isPlaying = true; state.isPaused = false; state.notifyChanged(); Result(true, "play mode") }
                "pause" -> { state.isPaused = !state.isPaused; state.notifyChanged(); Result(true, if (state.isPaused) "paused" else "resumed") }
                "stop" -> { state.isPlaying = false; state.isPaused = false; state.notifyChanged(); Result(true, "stopped") }
                "demo" -> {
                    val name = args.firstOrNull() ?: "platformer"
                    val scene = com.sura.engine.games.DemoScenes.build(name)
                    state.scene = scene
                    state.project?.let { project ->
                        com.sura.engine.project.ProjectManager(com.sura.engine.platform.Platform(Engine.get().context)).saveScene(project, scene)
                    }
                    state.notifyChanged()
                    Result(true, "loaded demo '${scene.name}' with ${scene.allObjects.size} objects")
                }
                else -> Result(false, "unknown command '$command' - type help")
            }
        } catch (t: Throwable) {
            Result(false, t.message ?: t.toString())
        }
    }

    val HELP = """
        Commands: help | list | spawn <name> | delete | select <name> | move <x> <y>
        grid [0/1] | snap [px] | stats | scene info|new <name>|save | script <code>
        demo [platformer|shooter|pong|topdown|particles] | undo | redo | play | pause | stop | clear
    """.trimIndent()
}
