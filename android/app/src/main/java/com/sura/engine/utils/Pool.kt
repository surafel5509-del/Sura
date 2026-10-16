package com.sura.engine.utils

class ObjectPool<T>(private val factory: () -> T, private val reset: (T) -> Unit = {}) {
    private val pool = ArrayDeque<T>()
    private var created = 0
    var maxSize = 256
    fun obtain(): T {
        return if (pool.isNotEmpty()) pool.removeLast() else { created++; factory() }
    }
    fun free(obj: T) {
        reset(obj)
        if (pool.size < maxSize) pool.addLast(obj)
    }
    fun clear() { pool.clear() }
    val size: Int get() = pool.size
}
