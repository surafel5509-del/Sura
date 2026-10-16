package com.sura.engine.animation

import com.sura.engine.core.Engine
import com.sura.engine.rendering.SpriteRenderer
import com.sura.engine.scene.Component
import com.sura.engine.utils.Rect

/** Describes how a tile sheet is laid out (used for frame animation). */
class SpriteSheet(val columns: Int = 4, val rows: Int = 1) {
    var texturePath: String = ""
    val frameCount: Int get() = columns * rows

    fun regionFor(frame: Int): Rect {
        val index = frame.coerceAtLeast(0)
        val column = index % columns
        val row = (index / columns) % rows
        return Rect(
            column.toFloat() / columns,
            row.toFloat() / rows,
            1f / columns,
            1f / rows
        )
    }
}

/**
 * Frame based sprite animation. Frames are indices into a [SpriteSheet] applied to the
 * SpriteRenderer on the same GameObject.
 */
class SpriteAnimator : Component() {
    data class FrameClip(
        val name: String,
        val frames: List<Int>,
        val fps: Float = 12f,
        val loop: Boolean = true,
        val onComplete: (() -> Unit)? = null
    )

    var sheet = SpriteSheet()
    val clips = mutableMapOf<String, FrameClip>()
    var currentClip: String? = null
        private set
    var frameIndex = 0
        private set
    var speed = 1f
    var isPlaying = false
        private set
    private var accumulator = 0f
    private var sprite: SpriteRenderer? = null

    override fun awake() {
        sprite = gameObject?.getComponent<SpriteRenderer>()
    }

    fun addClip(clip: FrameClip) { clips[clip.name] = clip }

    fun addClip(name: String, frames: List<Int>, fps: Float = 12f, loop: Boolean = true) {
        clips[name] = FrameClip(name, frames, fps, loop)
    }

    fun play(name: String, restart: Boolean = true) {
        val clip = clips[name] ?: return
        if (currentClip == name && !restart) { isPlaying = true; return }
        currentClip = name
        frameIndex = 0
        accumulator = 0f
        isPlaying = true
        applyFrame()
    }

    fun stop() { isPlaying = false }

    fun pause() { isPlaying = false }

    fun resume() { if (currentClip != null) isPlaying = true }

    override fun update(dt: Float) {
        if (!isPlaying) return
        val clip = clips[currentClip] ?: return
        if (clip.frames.isEmpty()) return
        if (sprite == null) sprite = gameObject?.getComponent<SpriteRenderer>()
        accumulator += dt * speed
        val frameDuration = 1f / clip.fps.coerceAtLeast(0.001f)
        while (accumulator >= frameDuration) {
            accumulator -= frameDuration
            frameIndex++
            if (frameIndex >= clip.frames.size) {
                if (clip.loop) {
                    frameIndex = 0
                } else {
                    frameIndex = clip.frames.size - 1
                    isPlaying = false
                    clip.onComplete?.invoke()
                    break
                }
            }
        }
        applyFrame()
    }

    private fun applyFrame() {
        val clip = clips[currentClip] ?: return
        val spriteRenderer = sprite ?: return
        val frame = clip.frames.getOrNull(frameIndex) ?: return
        val sprite = spriteRenderer.sprite ?: com.sura.engine.rendering.Sprite().also { spriteRenderer.sprite = it }
        sprite.region = sheet.regionFor(frame)
    }

    fun currentFrameNumber(): Int {
        val clip = clips[currentClip] ?: return 0
        return clip.frames.getOrNull(frameIndex) ?: 0
    }

    fun clipNames(): List<String> = clips.keys.toList()

    /** Frames per second actually achieved for the running clip. */
    fun effectiveFps(): Float {
        val clip = clips[currentClip] ?: return 0f
        return clip.fps * speed
    }
}

/** Shared registry so texture paths on sprites are resolved once per scene load. */
object TextureResolver {
    fun applyTo(scene: com.sura.engine.scene.Scene?, assets: com.sura.engine.assets.AssetManager) {
        val activeScene = scene ?: return
        for (go in activeScene.allObjects) {
            val sprite = go.getComponent<SpriteRenderer>() ?: continue
            if (sprite.texturePath.isEmpty()) continue
            val texture = assets.loadTexture(sprite.texturePath)
            sprite.loadedTexture = texture
            if (!texture.isLoaded) {
                com.sura.engine.core.Logger.w("Texture not found: ${sprite.texturePath}")
                continue
            }
            sprite.sprite = com.sura.engine.rendering.Sprite().apply { this.texture = texture }
        }
    }

    fun engineReady(): Boolean = Engine.isInitialized()
}
