package com.sura.engine.platform

import android.content.Context
import android.content.SharedPreferences
import java.io.File

class Platform(private val context: Context) {
    val filesDir: File get() = context.filesDir
    val cacheDir: File get() = context.cacheDir
    val externalDir: File? get() = context.getExternalFilesDir(null)
    val prefs: SharedPreferences get() = context.getSharedPreferences("sura_engine", Context.MODE_PRIVATE)

    fun saveString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    fun loadString(key: String, default: String = ""): String = prefs.getString(key, default) ?: default
    fun saveInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    fun loadInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)
    fun saveFloat(key: String, value: Float) { prefs.edit().putFloat(key, value).apply() }
    fun loadFloat(key: String, default: Float = 0f): Float = prefs.getFloat(key, default)
    fun saveBool(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun loadBool(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun writeFile(name: String, content: String) { File(filesDir, name).writeText(content) }
    fun readFile(name: String): String? { val f = File(filesDir, name); return if (f.exists()) f.readText() else null }
    fun deleteFile(name: String) { File(filesDir, name).delete() }
    fun listFiles(dir: String = ""): Array<String> {
        val f = if (dir.isEmpty()) filesDir else File(filesDir, dir)
        return f.list() ?: emptyArray()
    }
}
