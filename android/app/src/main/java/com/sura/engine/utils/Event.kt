package com.sura.engine.utils

class Event<T> {
    private val listeners = mutableListOf<(T) -> Unit>()
    fun subscribe(l: (T) -> Unit) { listeners.add(l) }
    fun unsubscribe(l: (T) -> Unit) { listeners.remove(l) }
    fun invoke(data: T) { for (l in listeners.toList()) l(data) }
    fun clear() { listeners.clear() }
    val count: Int get() = listeners.size
}

class Signal {
    private val listeners = mutableListOf<() -> Unit>()
    fun subscribe(l: () -> Unit) { listeners.add(l) }
    fun unsubscribe(l: () -> Unit) { listeners.remove(l) }
    fun invoke() { for (l in listeners.toList()) l() }
    fun clear() { listeners.clear() }
}
