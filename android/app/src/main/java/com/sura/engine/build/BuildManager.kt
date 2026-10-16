package com.sura.engine.build

class BuildManager {
    data class BuildConfig(
        val appName: String = "SuraGame",
        val packageName: String = "com.sura.game",
        val versionCode: Int = 1,
        val versionName: String = "1.0.0",
        val minSdk: Int = 21,
        val targetSdk: Int = 33,
        val orientation: String = "landscape",
        val iconPath: String = "",
        val splashPath: String = "",
        val keystorePath: String = "",
        val signingEnabled: Boolean = false
    )
    var config = BuildConfig()
    var isBuilding = false
    var buildProgress = 0f
    var lastBuildLog = StringBuilder()

    fun buildDebug(onComplete: (Boolean, String) -> Unit) {
        isBuilding = true; buildProgress = 0f; lastBuildLog.clear()
        // Offline build pipeline stub: package assets, compile scripts, export
        lastBuildLog.appendLine("Starting debug build...")
        buildProgress = 1f
        isBuilding = false
        onComplete(true, "Build successful (offline)")
    }
    fun buildRelease(onComplete: (Boolean, String) -> Unit) {
        isBuilding = true; buildProgress = 0f; lastBuildLog.clear()
        lastBuildLog.appendLine("Starting release build...")
        buildProgress = 1f
        isBuilding = false
        onComplete(true, "Release build successful (offline)")
    }
    fun exportProject(path: String): Boolean { return true }
    fun importProject(path: String): Boolean { return true }
}
