package com.sura.engine.core

class Time {
    var deltaTime: Float = 0f
    var totalTime: Float = 0f
    var frameCount: Long = 0
    var fps: Float = 0f
    private var lastFrame: Long = 0
    private var fpsAccum: Float = 0f
    private var fpsFrames: Int = 0

    fun update(now: Long) {
        if (lastFrame == 0L) lastFrame = now
        deltaTime = (now - lastFrame) / 1000f
        if (deltaTime > 0.25f) deltaTime = 0.25f
        lastFrame = now
        totalTime += deltaTime
        frameCount++
        fpsAccum += deltaTime
        fpsFrames++
        if (fpsAccum >= 0.5f) {
            fps = fpsFrames / fpsAccum
            fpsAccum = 0f
            fpsFrames = 0
        }
    }
    fun reset() { lastFrame = 0; totalTime = 0f; frameCount = 0; deltaTime = 0f }
}
