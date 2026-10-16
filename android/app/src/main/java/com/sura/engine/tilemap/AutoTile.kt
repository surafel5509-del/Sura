package com.sura.engine.tilemap

/**
 * Rule based autotiling: converts a boolean map of "solid" cells into 16 tile ids using the
 * classic 4-bit bitmask (up/right/down/left neighbours).
 */
object AutoTile {

    /** Bit indices used by the 47-tile / 16-tile autotile tables. */
    const val NORTH = 1
    const val EAST = 2
    const val SOUTH = 4
    const val WEST = 8

    fun mask(solid: (x: Int, y: Int) -> Boolean, x: Int, y: Int): Int {
        var result = 0
        if (!solid(x, y - 1)) result = result or NORTH
        if (!solid(x + 1, y)) result = result or EAST
        if (!solid(x, y + 1)) result = result or SOUTH
        if (!solid(x - 1, y)) result = result or WEST
        return result
    }

    /** Maps a neighbour mask to a tile id from the given table (index 0..15). */
    fun tileForMask(mask: Int, table: IntArray? = null): Int {
        if (table != null && mask in table.indices) return table[mask]
        return mask
    }

    /** Convenience: recompute every tile of a layer from its own solidity. */
    fun retileLayer(layer: TileLayer, table: IntArray? = null, baseTile: Int = 1) {
        val snapshot = IntArray(layer.tiles.size) { layer.tiles[it] }
        val solid: (Int, Int) -> Boolean = { x, y ->
            if (x < 0 || y < 0 || x >= layer.width || y >= layer.height) false
            else snapshot[y * layer.width + x] >= 0
        }
        for (y in 0 until layer.height) {
            for (x in 0 until layer.width) {
                if (snapshot[y * layer.width + x] < 0) {
                    layer.setTile(x, y, -1)
                    continue
                }
                val mask = mask(solid, x, y)
                layer.setTile(x, y, tileForMask(mask, table).let { baseTile + it })
            }
        }
    }

    /** Default 16-entry table (bit order: N, E, S, W) mapping to a tileset layout. */
    val DEFAULT_TABLE = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    )
}
