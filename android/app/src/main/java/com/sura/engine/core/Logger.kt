package com.sura.engine.core

import android.util.Log

object Logger {
    const val TAG = "SuraEngine"
    var enabled = true
    var level = Level.DEBUG
    enum class Level { VERBOSE, DEBUG, INFO, WARN, ERROR, NONE }

    fun v(msg: String, tag: String = TAG) { if (enabled && level <= Level.VERBOSE) Log.v(tag, msg) }
    fun d(msg: String, tag: String = TAG) { if (enabled && level <= Level.DEBUG) Log.d(tag, msg) }
    fun i(msg: String, tag: String = TAG) { if (enabled && level <= Level.INFO) Log.i(tag, msg) }
    fun w(msg: String, tag: String = TAG) { if (enabled && level <= Level.WARN) Log.w(tag, msg) }
    fun e(msg: String, tag: String = TAG, t: Throwable? = null) { if (enabled && level <= Level.ERROR) { if (t != null) Log.e(tag, msg, t) else Log.e(tag, msg) } }

    private val listeners = mutableListOf<(Level, String, String) -> Unit>()
    fun addListener(l: (Level, String, String) -> Unit) { listeners.add(l) }
    fun removeListener(l: (Level, String, String) -> Unit) { listeners.remove(l) }
}
