package com.sura.engine.save

import com.sura.engine.platform.Platform
import com.sura.engine.serialization.Json
import com.sura.engine.serialization.JsonValue

/** Slot based save/load system backed by the platform file store (fully offline). */
class SaveSystem(private val platform: Platform) {
    private val slots = 8

    fun save(slot: Int, data: JsonValue): Boolean {
        if (slot !in 0 until slots) return false
        return try {
            platform.writeFile("save_$slot.json", Json.stringify(data))
            platform.saveString("save_${slot}_time", System.currentTimeMillis().toString())
            true
        } catch (t: Throwable) { false }
    }

    fun load(slot: Int): JsonValue? {
        val raw = platform.readFile("save_$slot.json") ?: return null
        return try { Json.parse(raw) } catch (t: Throwable) { null }
    }

    fun delete(slot: Int) { platform.deleteFile("save_$slot.json") }

    fun exists(slot: Int): Boolean = platform.readFile("save_$slot.json") != null

    fun slotInfo(): List<SlotInfo> = (0 until slots).map { i ->
        SlotInfo(i, exists(i), platform.loadString("save_${i}_time", ""))
    }

    data class SlotInfo(val slot: Int, val used: Boolean, val timestamp: String)

    fun quickSave(data: JsonValue) { save(0, data) }
    fun quickLoad(): JsonValue? = load(0)

    val slotCount: Int get() = slots
}
