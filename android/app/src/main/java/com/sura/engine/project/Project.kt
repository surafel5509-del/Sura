package com.sura.engine.project

import com.sura.engine.rendering.Camera
import com.sura.engine.scene.Scene
import java.io.File

/** Project level settings that end up in the generated game manifest. */
class ProjectSettings {
    var appName: String = "My Game"
    var packageName: String = "com.sura.game"
    var versionName: String = "1.0.0"
    var versionCode: Int = 1
    var minSdk: Int = 21
    var targetSdk: Int = 33
    var orientation: String = "landscape"
    var companyName: String = "Sura Games"
    var iconPath: String = ""
    var primaryColor: String = "#89B4FA"
    var physicsGravityY: Float = -9.81f
    var fixedTimestep: Float = 1f / 60f
    var startScene: String = "MainScene"
    var showStats: Boolean = true

    fun toMap(): Map<String, Any> = mapOf(
        "appName" to appName,
        "packageName" to packageName,
        "versionName" to versionName,
        "versionCode" to versionCode,
        "minSdk" to minSdk,
        "targetSdk" to targetSdk,
        "orientation" to orientation,
        "companyName" to companyName,
        "primaryColor" to primaryColor,
        "gravity" to physicsGravityY,
        "fixedTimestep" to fixedTimestep,
        "startScene" to startScene,
        "showStats" to showStats
    )
}

/** An asset reference inside the project folder. */
class ProjectAsset(val path: String, val name: String, val type: AssetType) {
    enum class AssetType { SCENE, SCRIPT, PREFAB, TEXTURE, AUDIO, TILEMAP, FONT, DATA, FOLDER, UNKNOWN }

    companion object {
        fun typeOf(name: String): AssetType = when {
            name.endsWith(".scene") -> AssetType.SCENE
            name.endsWith(".sura") || name.endsWith(".script") -> AssetType.SCRIPT
            name.endsWith(".prefab") -> AssetType.PREFAB
            name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".webp") -> AssetType.TEXTURE
            name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".mp3") -> AssetType.AUDIO
            name.endsWith(".tilemap") -> AssetType.TILEMAP
            name.endsWith(".ttf") -> AssetType.FONT
            name.endsWith(".json") || name.endsWith(".txt") || name.endsWith(".csv") -> AssetType.DATA
            else -> AssetType.UNKNOWN
        }
    }
}

/**
 * A Sura project: a folder on disk holding scenes, scripts, prefabs and build settings.
 * Everything is stored locally so projects work fully offline.
 */
class Project(val name: String, val rootDir: File) {
    val settings = ProjectSettings().also { it.appName = name }
    val sceneFiles = mutableMapOf<String, String>()
    var lastOpened: Long = System.currentTimeMillis()

    val assetsDir: File get() = File(rootDir, "Assets")
    val scenesDir: File get() = File(rootDir, "Scenes")
    val scriptsDir: File get() = File(rootDir, "Scripts")
    val prefabsDir: File get() = File(rootDir, "Prefabs")
    val buildDir: File get() = File(rootDir, "Build")

    fun ensureStructure(): Boolean {
        var ok = true
        for (dir in listOf(rootDir, assetsDir, scenesDir, scriptsDir, prefabsDir, buildDir)) {
            if (!dir.exists()) ok = ok && dir.mkdirs()
        }
        return ok
    }

    fun writeText(relative: String, content: String): Boolean {
        return try {
            val file = File(rootDir, relative)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (t: Throwable) { false }
    }

    fun readText(relative: String): String? {
        val file = File(rootDir, relative)
        return if (file.exists()) file.readText() else null
    }

    fun delete(relative: String): Boolean = File(rootDir, relative).delete()

    fun exists(relative: String): Boolean = File(rootDir, relative).exists()

    fun list(relative: String = ""): List<String> {
        val dir = if (relative.isEmpty()) rootDir else File(rootDir, relative)
        return dir.list()?.sorted() ?: emptyList()
    }

    fun listAssets(relative: String = ""): List<ProjectAsset> {
        val dir = if (relative.isEmpty()) rootDir else File(rootDir, relative)
        val files = dir.listFiles() ?: return emptyList()
        return files.sortedBy { it.name }.map { f ->
            val rel = f.absolutePath.removePrefix(rootDir.absolutePath).removePrefix("/")
            ProjectAsset(rel, f.name, if (f.isDirectory) ProjectAsset.AssetType.FOLDER else ProjectAsset.AssetType.typeOf(f.name))
        }
    }

    fun scenes(): List<String> = list("Scenes").filter { it.endsWith(".scene") }
    fun scripts(): List<String> = list("Scripts").filter { it.endsWith(".sura") }
    fun prefabs(): List<String> = list("Prefabs").filter { it.endsWith(".prefab") }

    fun defaultCamera(): Camera = Camera()
}
