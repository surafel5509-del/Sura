package com.sura.engine.debug

import com.sura.engine.core.EngineService
import java.util.ArrayDeque
import kotlin.math.max

/** Lightweight frame profiler: tracks frame time, sections, draw calls and memory. */
class Profiler : EngineService {
    private val frameTimes = ArrayDeque<Float>()
    private val historySize = 120
    private val sectionTimes = linkedMapOf<String, Float>()
    private val sectionStack = ArrayDeque<Pair<String, Long>>()

    var fps: Float = 0f
        private set
    var frameMs: Float = 0f
        private set
    var minFps: Float = 0f
        private set
    var maxFps: Float = 0f
        private set
    var drawCalls: Int = 0
    var triangles: Int = 0
    var objectsRendered: Int = 0
    var activeParticles: Int = 0
    var activeBodies: Int = 0
    var memoryMb: Float = 0f
        private set
    var isRecording = false
    val samples = mutableListOf<Float>()

    fun beginFrame() {
        if (isRecording) { /* sampling handled in endFrame */ }
    }

    fun endFrame(dt: Float) {
        frameMs = dt * 1000f
        if (dt > 0f) fps = 1f / dt
        if (fps > maxFps) maxFps = fps
        if (minFps == 0f || fps < minFps) minFps = fps
        frameTimes.addLast(dt)
        while (frameTimes.size > historySize) frameTimes.removeFirst()
        if (isRecording) {
            samples.add(dt)
            if (samples.size > 1200) samples.removeAt(0)
        }
        val rt = Runtime.getRuntime()
        memoryMb = (rt.totalMemory() - rt.freeMemory()) / (1024f * 1024f)
    }

    fun beginSection(name: String) { sectionStack.addLast(name to System.nanoTime()) }
    fun endSection() {
        val name = sectionStack.pollLast() ?: return
        val elapsedMs = (System.nanoTime() - name.second) / 1_000_000f
        sectionTimes[name.first] = elapsedMs
    }

    fun averageFrameMs(): Float {
        if (frameTimes.isEmpty()) return 0f
        var total = 0f
        for (f in frameTimes) total += f
        return (total / frameTimes.size) * 1000f
    }

    fun peakFrameMs(): Float {
        var best = 0f
        for (f in frameTimes) best = max(best, f)
        return best * 1000f
    }

    fun sectionMs(name: String): Float = sectionTimes[name] ?: 0f
    fun sections(): Map<String, Float> = sectionTimes.toMap()

    fun frameStatsLine(): String =
        "FPS ${fps.toInt()} | frame ${"%.2f".format(frameMs)}ms | avg ${"%.2f".format(averageFrameMs())}ms | draw ${drawCalls} | obj ${objectsRendered} | mem ${"%.1f".format(memoryMb)}MB"

    fun reset() {
        frameTimes.clear(); sectionTimes.clear(); samples.clear()
        minFps = 0f; maxFps = 0f
    }
}
