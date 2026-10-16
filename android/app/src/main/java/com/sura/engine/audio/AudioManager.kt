package com.sura.engine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.sura.engine.core.EngineService

class AudioManager(private val context: Context) : EngineService {
    var masterVolume = 1f
    var musicVolume = 1f
    var sfxVolume = 1f
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<String, Int>()
    private val streamIds = mutableMapOf<Int, Int>()

    override fun start() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(16).setAudioAttributes(attrs).build()
    }
    override fun stop() { soundPool?.release(); soundPool = null; soundIds.clear() }

    fun loadSound(path: String): Int {
        soundIds[path]?.let { return it }
        // assets from res/raw not implemented; return 0
        return 0
    }
    fun playSound(id: Int, volume: Float = 1f, loop: Boolean = false): Int {
        val sp = soundPool ?: return -1
        return sp.play(id, volume * sfxVolume * masterVolume, volume * sfxVolume * masterVolume, 1, if (loop) -1 else 0, 1f)
    }
    fun stopSound(streamId: Int) { soundPool?.stop(streamId) }
    fun setMasterVolume(v: Float) { masterVolume = v.coerceIn(0f,1f) }
}
