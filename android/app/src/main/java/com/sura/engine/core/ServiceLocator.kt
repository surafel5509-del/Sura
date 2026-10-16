package com.sura.engine.core

interface EngineService {
    fun start() {}
    fun stop() {}
    fun pause() {}
    fun resume() {}
    fun update(dt: Float) {}
}

class ServiceLocator {
    private val services = linkedMapOf<Class<*>, EngineService>()
    private val order = mutableListOf<EngineService>()

    fun <T : EngineService> register(clazz: Class<T>, service: T) {
        services[clazz] = service
        if (service !in order) order.add(service)
    }
    inline fun <reified T : EngineService> register(service: T) = register(T::class.java, service)

    @Suppress("UNCHECKED_CAST")
    fun <T : EngineService> get(clazz: Class<T>): T? = services[clazz] as? T
    inline fun <reified T : EngineService> get(): T? = get(T::class.java)

    fun startAll() { order.forEach { it.start() } }
    fun stopAll() { order.reversed().forEach { it.stop() } }
    fun pauseAll() { order.forEach { it.pause() } }
    fun resumeAll() { order.forEach { it.resume() } }
    fun updateAll(dt: Float) { order.forEach { it.update(dt) } }
    fun clear() { services.clear(); order.clear() }
}
