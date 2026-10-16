package com.sura.engine.rendering

import com.sura.engine.particles.ParticleSystem
import com.sura.engine.scene.GameObject
import com.sura.engine.scene.Scene
import com.sura.engine.tilemap.TilemapComponent
import com.sura.engine.utils.Color

/**
 * Draws a scene with any [Renderer] backend (Canvas or OpenGL).
 * Keeps batching friendly ordering: tilemaps -> sprites -> particles.
 */
class SceneRenderer(private val renderer: Renderer) {
    var drawTiles = true
    var drawSprites = true
    var drawParticles = true
    var sortByLayer = true

    fun render(scene: Scene?, camera: Camera) {
        if (scene == null) return
        val objects = if (sortByLayer) scene.allObjects.sortedBy { it.transform.zOrder } else scene.allObjects
        if (drawTiles) {
            for (go in objects) {
                if (!go.active) continue
                val tilemap = go.getComponent<TilemapComponent>() ?: continue
                tilemap.render(renderer, go.transform.position)
            }
        }
        if (drawSprites) {
            for (go in objects) {
                if (!go.active) continue
                val sr = go.getComponent<SpriteRenderer>() ?: continue
                if (!sr.enabled) continue
                if (sr.color.a <= 0.001f) continue
                renderer.drawSprite(
                    sr.sprite ?: Sprite(),
                    go.transform.position,
                    go.transform.rotation,
                    com.sura.engine.utils.Vector2(sr.size.x * go.transform.scale.x, sr.size.y * go.transform.scale.y),
                    sr.color
                )
            }
        }
        if (drawParticles) {
            for (go in objects) {
                if (!go.active) continue
                val ps = go.getComponent<ParticleSystem>() ?: continue
                for (p in ps.getParticles()) {
                    val size = p.size
                    renderer.drawRect(
                        com.sura.engine.utils.Rect(p.position.x - size / 2f, p.position.y - size / 2f, size, size),
                        p.color, true
                    )
                }
            }
        }
    }

    fun renderObject(go: GameObject, color: Color) {
        renderer.drawRect(
            com.sura.engine.utils.Rect(go.transform.position.x - 16f, go.transform.position.y - 16f, 32f, 32f),
            color, true
        )
    }
}
