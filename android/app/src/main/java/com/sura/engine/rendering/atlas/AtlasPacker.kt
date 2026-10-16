package com.sura.engine.rendering.atlas

import com.sura.engine.utils.Rect

/**
 * Shelf/skyline texture atlas packer. Pure Kotlin so it can be unit tested and reused by the
 * editor, the runtime and any export tool. Produces normalized UV regions per sprite name.
 */
class AtlasPacker(val maxWidth: Int = 2048, val maxHeight: Int = 2048, val padding: Int = 2) {

    data class Entry(val name: String, val width: Int, val height: Int)

    data class Placement(val name: String, val x: Int, val y: Int, val width: Int, val height: Int, val rotated: Boolean = false) {
        fun rect(atlasWidth: Int, atlasHeight: Int): Rect = Rect(
            x.toFloat() / atlasWidth,
            y.toFloat() / atlasHeight,
            width.toFloat() / atlasWidth,
            height.toFloat() / atlasHeight
        )
    }

    data class Result(
        val width: Int,
        val height: Int,
        val placements: List<Placement>,
        val unpacked: List<String>
    ) {
        val usedArea: Int get() = placements.sumOf { it.width * it.height }
        fun coverage(): Float = if (width * height == 0) 0f else usedArea.toFloat() / (width * height)
        fun placementFor(name: String): Placement? = placements.firstOrNull { it.name == name }
    }

    /** Packs entries using a simple growing shelf algorithm (stable, fast, no dependencies). */
    fun pack(entries: List<Entry>): Result {
        val sorted = entries.sortedWith(compareByDescending<Entry> { it.height }.thenByDescending { it.width })
        val placements = mutableListOf<Placement>()
        val unpacked = mutableListOf<String>()
        var shelfY = 0
        var shelfHeight = 0
        var cursorX = 0
        var atlasWidth = 0

        for (entry in sorted) {
            if (entry.width > maxWidth || entry.height > maxHeight) {
                unpacked.add(entry.name)
                continue
            }
            if (cursorX + entry.width + padding > maxWidth) {
                shelfY += shelfHeight + padding
                shelfHeight = 0
                cursorX = 0
            }
            if (shelfY + entry.height + padding > maxHeight) {
                unpacked.add(entry.name)
                continue
            }
            placements.add(Placement(entry.name, cursorX, shelfY, entry.width, entry.height))
            cursorX += entry.width + padding
            shelfHeight = maxOf(shelfHeight, entry.height)
            atlasWidth = maxOf(atlasWidth, cursorX)
        }

        val atlasHeight = (shelfY + shelfHeight).coerceAtMost(maxHeight)
        return Result(
            width = nextPowerOfTwo(atlasWidth.coerceAtLeast(1)),
            height = nextPowerOfTwo(atlasHeight.coerceAtLeast(1)),
            placements = placements,
            unpacked = unpacked
        )
    }

    fun nextPowerOfTwo(value: Int): Int {
        var result = 1
        while (result < value) result = result shl 1
        return result.coerceAtMost(maxWidth)
    }

    companion object {
        /** Builds an entries list from "name,width,height" lines (used by the asset browser). */
        fun parseEntries(text: String): List<Entry> = text.split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(',')
                if (parts.size < 3) return@mapNotNull null
                val width = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
                val height = parts[2].trim().toIntOrNull() ?: return@mapNotNull null
                Entry(parts[0].trim(), width, height)
            }
    }
}
