package com.sura.engine.core

import android.content.Context
import com.sura.engine.core.Logger

class Engine private constructor(val context: Context) {
    val time = Time()
    val services = ServiceLocator()
    var isRunning = false
        private set
    var isPaused = false
        private set

    companion object {
        private const val MAX_FIXED_STEPS = 5
        @Volatile private var instance: Engine? = null
        fun init(context: Context): Engine {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) instance = Engine(context.applicationContext)
                }
            }
            return instance!!
        }
        fun get(): Engine = instance ?: throw IllegalStateException("Engine not initialized")
        fun isInitialized(): Boolean = instance != null
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        isPaused = false
        Logger.i("Engine started")
        services.startAll()
    }

    fun pause() { if (isRunning && !isPaused) { isPaused = true; services.pauseAll(); Logger.i("Engine paused") } }
    fun resume() { if (isRunning && isPaused) { isPaused = false; services.resumeAll(); Logger.i("Engine resumed") } }
    fun stop() {
        if (!isRunning) return
        isRunning = false
        isPaused = false
        services.stopAll()
        Logger.i("Engine stopped")
    }

    private var fixedAccumulator = 0f
    var fixedTimeStep: Float = 1f / 60f

    fun update() {
        if (!isRunning || isPaused) return
        time.update(System.currentTimeMillis())
        val dt = time.deltaTime
        services.updateAll(dt)
        fixedAccumulator += dt
        var steps = 0
        while (fixedAccumulator >= fixedTimeStep && steps < MAX_FIXED_STEPS) {
            services.fixedUpdateAll(fixedTimeStep)
            fixedAccumulator -= fixedTimeStep
            steps++
        }
        if (steps >= MAX_FIXED_STEPS) fixedAccumulator = 0f
    }
}
