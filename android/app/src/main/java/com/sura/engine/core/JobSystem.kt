package com.sura.engine.core

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Tiny worker pool used for background asset loading / build tasks (keeps the render thread free). */
class JobSystem(threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)) : EngineService {
    private val executor = Executors.newFixedThreadPool(threads)
    private val completed = ConcurrentLinkedQueue<() -> Unit>()
    private val pending = java.util.concurrent.atomic.AtomicInteger(0)
    val isBusy: Boolean get() = pending.get() > 0

    fun run(job: () -> Unit, onComplete: (() -> Unit)? = null) {
        pending.incrementAndGet()
        executor.execute {
            try {
                job()
            } catch (t: Throwable) {
                Logger.e("Job failed: ${t.message}", t = t)
            } finally {
                pending.decrementAndGet()
                if (onComplete != null) completed.add(onComplete)
            }
        }
    }

    fun runBlocking(jobs: List<() -> Unit>) {
        val latch = java.util.concurrent.CountDownLatch(jobs.size)
        for (job in jobs) {
            executor.execute {
                try { job() } finally { latch.countDown() }
            }
        }
        try { latch.await() } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
    }

    override fun update(dt: Float) {
        while (true) {
            val cb = completed.poll() ?: break
            try { cb() } catch (t: Throwable) { Logger.e("Job callback failed", t = t) }
        }
    }

    override fun stop() {
        executor.shutdown()
        try { executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS) } catch (e: InterruptedException) { }
    }

    val pendingCount: Int get() = pending.get()
}

/** Simple timed scheduler for gameplay callbacks (delays, repeats, cooldowns). */
class Scheduler : EngineService {
    private class Task(var delay: Float, var interval: Float, var repeat: Boolean, var action: () -> Unit, var cancelled: Boolean = false)

    private val tasks = mutableListOf<Task>()

    fun schedule(delay: Float, action: () -> Unit): Task = Task(delay, 0f, false, action).also { tasks.add(it) }
    fun repeat(interval: Float, action: () -> Unit): Task = Task(interval, interval, true, action).also { tasks.add(it) }
    fun cancel(task: Task) { task.cancelled = true; tasks.remove(task) }
    fun clearAll() { tasks.clear() }

    override fun update(dt: Float) {
        val it = tasks.iterator()
        val finished = mutableListOf<Task>()
        while (it.hasNext()) {
            val task = it.next()
            if (task.cancelled) { it.remove(); continue }
            task.delay -= dt
            if (task.delay <= 0f) {
                task.action()
                if (task.repeat) task.delay = task.interval else finished.add(task)
            }
        }
        tasks.removeAll(finished)
    }
}
