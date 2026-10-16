package com.sura.engine.project

import com.sura.engine.core.Logger
import com.sura.engine.platform.Platform
import com.sura.engine.scene.Scene
import com.sura.engine.serialization.SceneSerializer
import java.io.File

/** Creates, opens, saves and imports Sura projects - all offline, on the device file system. */
class ProjectManager(private val platform: Platform) {
    val serializer = SceneSerializer()
    var current: Project? = null
        private set

    val projectsDir: File get() = File(platform.filesDir, "Projects")

    init { if (!projectsDir.exists()) projectsDir.mkdirs() }

    fun createProject(name: String): Project {
        val dir = File(projectsDir, sanitize(name))
        dir.mkdirs()
        val project = Project(name, dir)
        project.ensureStructure()
        writeSettings(project)
        current = project
        Logger.i("Project created: ${project.name} at ${dir.absolutePath}")
        return project
    }

    fun openProject(name: String): Project? {
        val dir = File(projectsDir, sanitize(name))
        if (!dir.exists()) return null
        val project = Project(name, dir)
        project.ensureStructure()
        readSettings(project)
        current = project
        return project
    }

    fun saveProject(): Boolean {
        val project = current ?: return false
        return saveProject(project)
    }

    fun saveProject(project: Project): Boolean {
        writeSettings(project)
        platform.saveString("last_project", project.name)
        return true
    }

    fun deleteProject(name: String): Boolean {
        val dir = File(projectsDir, sanitize(name))
        val deleted = dir.deleteRecursively()
        if (current?.name == name) current = null
        return deleted
    }

    fun listProjects(): List<String> = projectsDir.list()?.sorted() ?: emptyList()

    fun lastProjectName(): String = platform.loadString("last_project", "")

    // ------------------------------------------------------------------ scenes

    fun saveScene(project: Project, scene: Scene): Boolean {
        val json = serializer.serialize(scene)
        val ok = project.writeText("Scenes/${sanitize(scene.name)}.scene", json)
        if (ok) Logger.i("Scene saved: ${scene.name}")
        return ok
    }

    fun loadScene(project: Project, name: String): Scene? {
        val json = project.readText("Scenes/${sanitize(name)}.scene") ?: return null
        return serializer.deserialize(json, name)
    }

    fun listScenes(project: Project): List<String> = project.scenes().map { it.removeSuffix(".scene") }

    // ------------------------------------------------------------------ scripts

    fun saveScript(project: Project, name: String, source: String): Boolean =
        project.writeText("Scripts/${sanitize(name)}.sura", source)

    fun loadScript(project: Project, name: String): String? =
        project.readText("Scripts/${sanitize(name)}.sura")

    // ------------------------------------------------------------------ prefabs

    fun savePrefab(project: Project, name: String, json: String): Boolean =
        project.writeText("Prefabs/${sanitize(name)}.prefab", json)

    fun loadPrefab(project: Project, name: String): String? =
        project.readText("Prefabs/${sanitize(name)}.prefab")

    // ------------------------------------------------------------------ settings IO

    private fun writeSettings(project: Project) {
        val map = project.settings.toMap()
        val sb = StringBuilder()
        sb.append("{\n")
        val entries = map.entries.toList()
        for ((index, entry) in entries.withIndex()) {
            val value = entry.value
            val encoded = when (value) {
                is String -> "\"" + value.replace("\"", "\\\"") + "\""
                else -> value.toString()
            }
            sb.append("  \"").append(entry.key).append("\": ").append(encoded)
            if (index < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("}\n")
        project.writeText("project.sura.json", sb.toString())
    }

    private fun readSettings(project: Project) {
        val raw = project.readText("project.sura.json") ?: return
        try {
            val settings = project.settings
            fun value(key: String): String? {
                val marker = "\"$key\""
                val start = raw.indexOf(marker)
                if (start < 0) return null
                val colon = raw.indexOf(':', start)
                if (colon < 0) return null
                var end = raw.indexOf('\n', colon)
                if (end < 0) end = raw.length
                var text = raw.substring(colon + 1, end).trim().trimEnd(',')
                if (text.startsWith("\"") && text.endsWith("\"") && text.length >= 2) {
                    text = text.substring(1, text.length - 1)
                }
                return text
            }
            value("appName")?.let { settings.appName = it }
            value("packageName")?.let { settings.packageName = it }
            value("versionName")?.let { settings.versionName = it }
            value("versionCode")?.let { settings.versionCode = it.toIntOrNull() ?: 1 }
            value("orientation")?.let { settings.orientation = it }
            value("companyName")?.let { settings.companyName = it }
            value("startScene")?.let { settings.startScene = it }
            value("gravity")?.let { settings.physicsGravityY = it.toFloatOrNull() ?: -9.81f }
            value("rendererBackend")?.let { settings.rendererBackend = it }
            value("targetFps")?.let { settings.targetFps = it.toIntOrNull() ?: 60 }
            value("autosaveSeconds")?.let { settings.autosaveSeconds = it.toIntOrNull() ?: 60 }
            value("gridSize")?.let { settings.gridSize = it.toFloatOrNull() ?: 32f }
        } catch (t: Throwable) {
            Logger.w("Could not parse project settings: ${t.message}")
        }
    }

    private fun sanitize(name: String): String =
        name.trim().replace(Regex("[^A-Za-z0-9_\\-]"), "_").ifEmpty { "Project" }
}
