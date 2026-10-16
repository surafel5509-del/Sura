package com.sura.engine.tilemap

import com.sura.engine.rendering.Renderer
import com.sura.engine.scene.Component
import com.sura.engine.utils.Color
import com.sura.engine.utils.Rect
import com.sura.engine.utils.Vector2

/** A tileset describes how tile ids map onto a texture atlas. */
class TileSet(
    val name: String = "TileSet",
    val columns: Int = 8,
    val rows: Int = 8,
    val tileWidth: Int = 32,
    val tileHeight: Int = 32
) {
    var texturePath: String = ""
    val tileCount: Int get() = columns * rows

    /** UV rect for a tile id inside the atlas (normalized 0..1). */
    fun regionOf(tileId: Int): Rect {
        val index = tileId.coerceAtLeast(0)
        val col = index % columns
        val row = index / columns
        return Rect(
            col.toFloat() / columns,
            row.toFloat() / rows,
            1f / columns,
            1f / rows
        )
    }
}

/** One layer of tiles. Tile id -1 means "empty". */
class TileLayer(val name: String, val width: Int, val height: Int) {
    val tiles = IntArray(width * height) { -1 }
    var visible = true
    var solid = false
    var opacity = 1f
    var color = Color.WHITE

    fun index(x: Int, y: Int): Int = y * width + x
    fun inBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    fun getTile(x: Int, y: Int): Int = if (inBounds(x, y)) tiles[index(x, y)] else -1

    fun setTile(x: Int, y: Int, tileId: Int) {
        if (inBounds(x, y)) tiles[index(x, y)] = tileId
    }

    fun fill(tileId: Int) { for (i in tiles.indices) tiles[i] = tileId }

    fun isSolidAt(x: Int, y: Int): Boolean = solid && getTile(x, y) >= 0

    fun clear() { fill(-1) }
}

/** Grid of layers - the level data for 2D games. */
class Tilemap(val width: Int, val height: Int, val tileSize: Float = 32f) {
    val layers = mutableListOf<TileLayer>()
    var tileSet: TileSet = TileSet()

    fun addLayer(name: String): TileLayer {
        val layer = TileLayer(name, width, height)
        layers.add(layer)
        return layer
    }

    fun layer(name: String): TileLayer? = layers.firstOrNull { it.name == name }

    fun worldToTile(world: Vector2): Vector2 = Vector2(
        kotlin.math.floor((world.x / tileSize).toDouble()).toFloat(),
        kotlin.math.floor((world.y / tileSize).toDouble()).toFloat()
    )

    fun tileToWorld(tx: Int, ty: Int): Vector2 = Vector2(tx * tileSize + tileSize / 2f, ty * tileSize + tileSize / 2f)

    fun isSolidAt(world: Vector2): Boolean {
        val t = worldToTile(world)
        val tx = t.x.toInt()
        val ty = t.y.toInt()
        for (layer in layers) if (layer.isSolidAt(tx, ty)) return true
        return false
    }

    fun isSolidTile(tx: Int, ty: Int): Boolean {
        for (layer in layers) if (layer.isSolidAt(tx, ty)) return true
        return false
    }

    fun bounds(): Rect = Rect(0f, 0f, width * tileSize, height * tileSize)
}

/** Renders a [Tilemap] using any renderer backend. */
class TilemapRenderer(private val tilemap: Tilemap) {
    var cullingEnabled = true
    var tilesDrawn = 0

    fun render(renderer: Renderer, origin: Vector2, viewport: Rect? = null) {
        tilesDrawn = 0
        for (layer in tilemap.layers) {
            if (!layer.visible) continue
            for (y in 0 until layer.height) {
                for (x in 0 until layer.width) {
                    val tileId = layer.getTile(x, y)
                    if (tileId < 0) continue
                    val worldX = origin.x + x * tilemap.tileSize
                    val worldY = origin.y + y * tilemap.tileSize
                    if (cullingEnabled && viewport != null) {
                        if (worldX + tilemap.tileSize < viewport.x || worldX > viewport.x + viewport.w) continue
                        if (worldY + tilemap.tileSize < viewport.y || worldY > viewport.y + viewport.h) continue
                    }
                    renderer.drawRect(Rect(worldX, worldY, tilemap.tileSize, tilemap.tileSize), layer.color, true)
                    tilesDrawn++
                }
            }
        }
    }
}

/** Component wrapper so a Tilemap can live in the scene graph. */
class TilemapComponent : Component() {
    var tilemap: Tilemap? = null
    var tileSize: Float = 32f
    val rendererCache = HashMap<String, TilemapRenderer>()

    override fun awake() {
        tilemap?.let { map ->
            if (map.layers.isEmpty()) map.addLayer("Ground")
            if (map.layers.size == 1) map.layers[0].solid = true
        }
    }

    fun painter(): TilemapRenderer {
        val key = "main"
        return rendererCache.getOrPut(key) { TilemapRenderer(tilemap ?: Tilemap(8, 8, tileSize)) }
    }

    fun render(renderer: Renderer, origin: Vector2) {
        val map = tilemap ?: return
        for (layer in map.layers) {
            if (!layer.visible) continue
            for (y in 0 until layer.height) {
                for (x in 0 until layer.width) {
                    val tileId = layer.getTile(x, y)
                    if (tileId < 0) continue
                    renderer.drawRect(
                        Rect(origin.x + x * tileSize, origin.y + y * tileSize, tileSize, tileSize),
                        layer.color, true
                    )
                }
            }
        }
    }

    fun paintAt(worldX: Float, worldY: Float, tileId: Int, layerIndex: Int = 0) {
        val map = tilemap ?: return
        val layer = map.layers.getOrNull(layerIndex) ?: return
        val tx = kotlin.math.floor((worldX - (transform?.position?.x ?: 0f)) / tileSize).toInt()
        val ty = kotlin.math.floor((worldY - (transform?.position?.y ?: 0f)) / tileSize).toInt()
        layer.setTile(tx, ty, tileId)
    }

    fun eraseAt(worldX: Float, worldY: Float, layerIndex: Int = 0) = paintAt(worldX, worldY, -1, layerIndex)
}

/** Collision helper that lets platformer scripts resolve movement against solid tiles. */
class TilemapCollider {
    fun resolve(tilemap: Tilemap, previous: Vector2, current: Vector2, halfSize: Float): Vector2 {
        var result = current.copy()
        val stepping = 4
        val dx = (current.x - previous.x) / stepping
        val dy = (current.y - previous.y) / stepping
        for (i in 0 until stepping) {
            val probeX = result.x + dx
            if (!tilemap.isSolidAt(Vector2(probeX + if (dx > 0) halfSize else -halfSize, result.y))) result.x = probeX
            val probeY = result.y + dy
            if (!tilemap.isSolidAt(Vector2(result.x, probeY + if (dy > 0) halfSize else -halfSize))) result.y = probeY
        }
        return result
    }

    fun onGround(tilemap: Tilemap, position: Vector2, halfSize: Float): Boolean =
        tilemap.isSolidAt(Vector2(position.x, position.y - halfSize - 2f))
}
