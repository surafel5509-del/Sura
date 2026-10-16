package com.sura.engine.animation

import com.sura.engine.scene.Component
import com.sura.engine.utils.Easing

class AnimationClip(val name: String) {
    data class Keyframe(val time: Float, val value: Float)
    val tracks = mutableMapOf<String, MutableList<Keyframe>>()
    var duration = 0f
    var loop = true
    fun addKey(track: String, time: Float, value: Float) {
        tracks.getOrPut(track) { mutableListOf() }.add(Keyframe(time, value))
        if (time > duration) duration = time
    }
}

class Animator : Component() {
    val clips = mutableMapOf<String, AnimationClip>()
    var currentClip: String? = null
    var time = 0f
    var speed = 1f
    var isPlaying = false
    var onFinished: (() -> Unit)? = null

    fun addClip(clip: AnimationClip) { clips[clip.name] = clip }
    fun play(name: String) { if (clips.containsKey(name)) { currentClip = name; time = 0f; isPlaying = true } }
    fun stop() { isPlaying = false; time = 0f }
    fun pause() { isPlaying = false }
    fun resume() { isPlaying = true }

    override fun update(dt: Float) {
        if (!isPlaying) return
        val clip = clips[currentClip] ?: return
        time += dt * speed
        if (time >= clip.duration) {
            if (clip.loop) time %= clip.duration.coerceAtLeast(0.0001f)
            else { time = clip.duration; isPlaying = false; onFinished?.invoke() }
        }
    }
    fun getValue(track: String): Float {
        val clip = clips[currentClip] ?: return 0f
        val keys = clip.tracks[track] ?: return 0f
        if (keys.isEmpty()) return 0f
        // find
        var i = 0
        while (i < keys.size - 1 && keys[i+1].time <= time) i++
        if (i >= keys.size - 1) return keys.last().value
        val a = keys[i]; val b = keys[i+1]
        val t = ((time - a.time) / (b.time - a.time)).coerceIn(0f,1f)
        return a.value + (b.value - a.value) * Easing.linear(t)
    }
}

class Tween(var from: Float, var to: Float, var duration: Float, val onUpdate: (Float) -> Unit, val onComplete: (() -> Unit)? = null) {
    var elapsed = 0f
    var easing: (Float) -> Float = { t -> Easing.easeInOutQuad(t) }
    var isComplete = false
    fun update(dt: Float) {
        if (isComplete) return
        elapsed += dt
        val t = (elapsed / duration).coerceIn(0f,1f)
        onUpdate(from + (to - from) * easing(t))
        if (t >= 1f) { isComplete = true; onComplete?.invoke() }
    }
}

class TweenManager {
    val tweens = mutableListOf<Tween>()
    fun add(t: Tween) { tweens.add(t) }
    fun update(dt: Float) {
        for (t in tweens.toList()) { t.update(dt); if (t.isComplete) tweens.remove(t) }
    }
}
