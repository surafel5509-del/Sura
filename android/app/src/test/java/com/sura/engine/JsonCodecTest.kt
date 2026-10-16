package com.sura.engine

import com.sura.engine.serialization.Json
import com.sura.engine.serialization.JsonValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonCodecTest {

    @Test fun roundTripNestedDocument() {
        val source = """
            {
              "name": "MainScene",
              "version": 1,
              "objects": [
                { "name": "Player", "x": 12.5, "active": true, "tag": null },
                { "name": "Enemy", "x": -3, "active": false }
              ]
            }
        """.trimIndent()
        val parsed = Json.parse(source)
        assertEquals("MainScene", parsed.str("name"))
        assertEquals(1, parsed.int("version"))
        val objects = parsed.array("objects")!!
        assertEquals(2, objects.size)
        assertEquals("Player", objects[0]?.str("name"))
        assertEquals(12.5, objects[0]?.num("x") ?: 0.0, 0.0001)
        assertTrue(objects[0]?.bool("active") == true)
        assertEquals(-3.0, objects[1]?.num("x") ?: 0.0, 0.0001)

        val text = Json.stringify(parsed)
        val reparsed = Json.parse(text)
        assertEquals(2, reparsed.array("objects")!!.size)
    }

    @Test fun escapesAreHandled() {
        val value = JsonValue.JObject()
        value["text"] = Json.of("line1\nline2\t\"quoted\"")
        val text = Json.stringify(value)
        assertEquals("line1\nline2\t\"quoted\"", Json.parse(text).str("text"))
    }

    @Test fun builderProducesArray() {
        val array = Json.array(listOf(Json.of(1), Json.of("two"), Json.of(true)))
        assertEquals(3, array.size)
        assertEquals("two", array[1]?.asString())
    }
}
