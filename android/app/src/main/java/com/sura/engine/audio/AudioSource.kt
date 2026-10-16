package com.sura.engine.audio

import com.sura.engine.scene.Component

class AudioSource : Component() {
    var clipPath: String = ""
    var volume = 1f
    var pitch = 1f
    var loop = false
    var playOnAwake = false
    var is3D = false
    private var streamId = -1
    fun play() {}
    fun stop() {}
    fun pause() {}
    override fun start() { if (playOnAwake) play() }
}
