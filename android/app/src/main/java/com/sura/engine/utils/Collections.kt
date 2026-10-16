package com.sura.engine.utils

/** Small helpers used across the engine. */
object Collections {
    inline fun <T> List<T>.firstOrNullMatching(predicate: (T) -> Boolean): T? {
        for (item in this) if (predicate(item)) return item
        return null
    }

    fun <T> MutableList<T>.removeFirstWhere(predicate: (T) -> Boolean): Boolean {
        val it = iterator()
        while (it.hasNext()) {
            if (predicate(it.next())) { it.remove(); return true }
        }
        return false
    }
}

class OrderedSet<T> {
    private val list = mutableListOf<T>()
    private val set = mutableSetOf<T>()
    val size: Int get() = list.size
    fun add(value: T): Boolean {
        if (set.add(value)) { list.add(value); return true }
        return false
    }
    fun remove(value: T): Boolean {
        if (set.remove(value)) { list.remove(value); return true }
        return false
    }
    fun contains(value: T): Boolean = set.contains(value)
    fun toList(): List<T> = list.toList()
    fun clear() { list.clear(); set.clear() }
    operator fun iterator(): Iterator<T> = list.iterator()
}
