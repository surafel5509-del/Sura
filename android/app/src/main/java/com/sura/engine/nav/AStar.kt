package com.sura.engine.nav

import com.sura.engine.utils.Vector2

/** Uniform cost grid used for pathfinding (buildings, enemies, RTS units). */
class NavGrid(val width: Int, val height: Int, val cellSize: Float = 32f) {
    private val blocked = BooleanArray(width * height)
    val weights = FloatArray(width * height) { 1f }
    var allowDiagonal = true

    fun index(x: Int, y: Int): Int = y * width + x
    fun inBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height
    fun isBlocked(x: Int, y: Int): Boolean = !inBounds(x, y) || blocked[index(x, y)]
    fun setBlocked(x: Int, y: Int, value: Boolean) { if (inBounds(x, y)) blocked[index(x, y)] = value }
    fun setWeight(x: Int, y: Int, value: Float) { if (inBounds(x, y)) weights[index(x, y)] = value.coerceAtLeast(0.01f) }
    fun clear() { blocked.fill(false) }

    fun setBlockedRect(x0: Int, y0: Int, w: Int, h: Int, value: Boolean = true) {
        for (y in y0 until y0 + h) for (x in x0 until x0 + w) setBlocked(x, y, value)
    }

    fun worldToCell(world: Vector2): Pair<Int, Int> = Pair((world.x / cellSize).toInt(), (world.y / cellSize).toInt())
    fun cellToWorld(x: Int, y: Int): Vector2 = Vector2(x * cellSize + cellSize / 2f, y * cellSize + cellSize / 2f)

    fun worldBlocked(world: Vector2): Boolean {
        val (x, y) = worldToCell(world)
        return isBlocked(x, y)
    }

    /** A* search returning world-space waypoints (empty when no path exists). */
    fun findPath(start: Vector2, goal: Vector2): List<Vector2> {
        val (sx, sy) = worldToCell(start)
        val (gx, gy) = worldToCell(goal)
        return findPathCells(sx, sy, gx, gy).map { cellToWorld(it.first, it.second) }
    }

    fun findPathCells(sx: Int, sy: Int, gx: Int, gy: Int): List<Pair<Int, Int>> {
        if (!inBounds(sx, sy) || !inBounds(gx, gy)) return emptyList()
        if (isBlocked(gx, gy)) return emptyList()
        val size = width * height
        val gScore = FloatArray(size) { Float.MAX_VALUE }
        val fScore = FloatArray(size) { Float.MAX_VALUE }
        val cameFrom = IntArray(size) { -1 }
        val closed = BooleanArray(size)
        val startIndex = index(sx, sy)
        val goalIndex = index(gx, gy)
        gScore[startIndex] = 0f
        fScore[startIndex] = heuristic(sx, sy, gx, gy)
        val open = BinaryHeap(size)
        open.push(startIndex, fScore[startIndex])

        while (open.isNotEmpty()) {
            val current = open.pop()
            if (current == goalIndex) return reconstruct(cameFrom, current)
            if (closed[current]) continue
            closed[current] = true
            val cx = current % width
            val cy = current / width
            for (neighbour in neighbours(cx, cy)) {
                val nx = neighbour.first
                val ny = neighbour.second
                if (isBlocked(nx, ny)) continue
                val nIndex = index(nx, ny)
                if (closed[nIndex]) continue
                val step = if (nx != cx && ny != cy) 1.4142f else 1f
                val tentative = gScore[current] + step * weights[nIndex]
                if (tentative < gScore[nIndex]) {
                    cameFrom[nIndex] = current
                    gScore[nIndex] = tentative
                    fScore[nIndex] = tentative + heuristic(nx, ny, gx, gy)
                    open.push(nIndex, fScore[nIndex])
                }
            }
        }
        return emptyList()
    }

    private fun neighbours(x: Int, y: Int): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        result.add(Pair(x + 1, y)); result.add(Pair(x - 1, y))
        result.add(Pair(x, y + 1)); result.add(Pair(x, y - 1))
        if (allowDiagonal) {
            result.add(Pair(x + 1, y + 1)); result.add(Pair(x - 1, y - 1))
            result.add(Pair(x + 1, y - 1)); result.add(Pair(x - 1, y + 1))
        }
        return result
    }

    private fun heuristic(x: Int, y: Int, gx: Int, gy: Int): Float {
        val dx = kotlin.math.abs(x - gx)
        val dy = kotlin.math.abs(y - gy)
        return if (allowDiagonal) (dx + dy) + (-2 * minOf(dx, dy)) + 1.4142f * minOf(dx, dy) else (dx + dy).toFloat()
    }

    private fun reconstruct(cameFrom: IntArray, currentIndex: Int): List<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        var node = currentIndex
        while (node != -1) {
            path.add(Pair(node % width, node / width))
            node = cameFrom[node]
        }
        path.reverse()
        return path
    }
}

/** Minimal binary heap with decrease-key emulation through duplicate pushes. */
private class BinaryHeap(capacity: Int) {
    private var keys = IntArray(capacity.coerceAtLeast(16))
    private var priorities = FloatArray(capacity.coerceAtLeast(16))
    private var size = 0

    fun isNotEmpty(): Boolean = size > 0

    fun push(key: Int, priority: Float) {
        if (size >= keys.size) grow()
        keys[size] = key
        priorities[size] = priority
        size++
        siftUp(size - 1)
    }

    fun pop(): Int {
        val top = keys[0]
        size--
        if (size > 0) {
            keys[0] = keys[size]
            priorities[0] = priorities[size]
            siftDown(0)
        }
        return top
    }

    private fun grow() {
        keys = keys.copyOf(keys.size * 2)
        priorities = priorities.copyOf(priorities.size * 2)
    }

    private fun siftUp(start: Int) {
        var index = start
        while (index > 0) {
            val parent = (index - 1) / 2
            if (priorities[parent] <= priorities[index]) break
            swap(parent, index)
            index = parent
        }
    }

    private fun siftDown(start: Int) {
        var index = start
        while (true) {
            val left = index * 2 + 1
            val right = left + 1
            var smallest = index
            if (left < size && priorities[left] < priorities[smallest]) smallest = left
            if (right < size && priorities[right] < priorities[smallest]) smallest = right
            if (smallest == index) break
            swap(index, smallest)
            index = smallest
        }
    }

    private fun swap(a: Int, b: Int) {
        val k = keys[a]; keys[a] = keys[b]; keys[b] = k
        val p = priorities[a]; priorities[a] = priorities[b]; priorities[b] = p
    }
}
