package com.sura.engine

import com.sura.engine.core.Scheduler
import com.sura.engine.debug.Profiler
import com.sura.engine.localization.Localization
import com.sura.engine.animation.Tween
import com.sura.engine.animation.TweenManager
import com.sura.engine.serialization.Serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilerSchedulerTest {

    @Test fun profilerAggregatesFrameStats() {
        val profiler = Profiler()
        repeat(60) { profiler.endFrame(1f / 60f) }
        assertTrue(profiler.fps > 55f && profiler.fps < 65f)
        assertTrue(profiler.averageFrameMs() in 10f..20f)
        assertTrue(profiler.frameStatsLine().contains("FPS"))
        profiler.beginSection("update")
        profiler.endSection()
        assertTrue(profiler.sections().containsKey("update"))
    }

    @Test fun schedulerRunsDelayedAndRepeatingTasks() {
        val scheduler = Scheduler()
        var delayed = 0
        var repeated = 0
        scheduler.schedule(0.1f) { delayed++ }
        scheduler.repeat(0.05f) { repeated++ }
        repeat(4) { scheduler.update(0.05f) }
        assertEquals(1, delayed)
        assertTrue(repeated >= 3)
    }

    @Test fun tweenInterpolatesAndCompletes() {
        val manager = TweenManager()
        var value = 0f
        var completed = false
        manager.add(Tween(0f, 10f, 1f, { value = it }, { completed = true }))
        repeat(60) { manager.update(1f / 60f) }
        assertEquals(10f, value, 0.5f)
        assertTrue(completed)
    }

    @Test fun serializerHandlesMapsAndLists() {
        val data = mapOf("score" to 42, "level" to "cave", "items" to listOf("sword", "shield"), "alive" to true)
        val json = Serializer.stringify(data)
        val parsed = Serializer.parse(json)
        assertEquals(42.0, parsed.num("score"), 0.0001)
        assertEquals("cave", parsed.str("level"))
        assertEquals(2, parsed.array("items")?.size)
        assertTrue(parsed.bool("alive"))
    }

    @Test fun localizationFallsBackToDefaultLanguage() {
        val localization = Localization("en")
        localization.set("hello", "Hello")
        localization.set("hello", "Selam", "am")
        assertEquals("Hello", localization.translate("hello"))
        localization.language = "am"
        assertEquals("Selam", localization.translate("hello"))
        localization.language = "fr"
        assertEquals("Hello", localization.translate("hello"))
        assertEquals("missing", localization.translate("missing"))
    }
}
