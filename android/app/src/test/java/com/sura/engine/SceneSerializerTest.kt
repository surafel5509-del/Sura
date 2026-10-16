package com.sura.engine

import com.sura.engine.physics.BoxCollider2D
import com.sura.engine.physics.RigidBody2D
import com.sura.engine.rendering.Light2D
import com.sura.engine.rendering.SpriteRenderer
import com.sura.engine.scene.Scene
import com.sura.engine.scripting.ScriptBehaviour
import com.sura.engine.serialization.SceneSerializer
import com.sura.engine.utils.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneSerializerTest {

    private fun buildScene(): Scene {
        val scene = Scene("Demo")
        val player = scene.createGameObject("Player")
        player.tag = "Player"
        player.transform.position.set(120f, 240f)
        player.transform.rotation = 45f
        player.addComponent(SpriteRenderer().apply {
            size.set(32f, 48f)
            color = Color(1f, 0f, 0f, 1f)
        })
        player.addComponent(RigidBody2D().apply { mass = 2.5f })
        player.addComponent(BoxCollider2D().apply { size.set(32f, 48f) })
        player.addComponent(ScriptBehaviour().apply {
            scriptName = "PlayerController"
            source = "function update(dt) { move(10, 0) }"
        })
        val light = scene.createGameObject("Sun")
        light.addComponent(Light2D().apply { radius = 300f })
        return scene
    }

    @Test fun sceneRoundTripKeepsObjectsAndComponents() {
        val serializer = SceneSerializer()
        val json = serializer.serialize(buildScene())
        assertTrue(json.contains("Player"))
        val restored = serializer.deserialize(json, "Restored")
        assertEquals("Demo", restored.name)
        assertEquals(2, restored.allObjects.size)

        val player = restored.findByName("Player")
        assertNotNull(player)
        assertEquals(120f, player!!.transform.position.x, 0.001f)
        assertEquals(240f, player.transform.position.y, 0.001f)
        assertEquals(45f, player.transform.rotation, 0.001f)
        assertEquals("Player", player.tag)

        val sprite = player.getComponent<SpriteRenderer>()
        assertNotNull(sprite)
        assertEquals(32f, sprite!!.size.x, 0.001f)

        val body = player.getComponent<RigidBody2D>()
        assertNotNull(body)
        assertEquals(2.5f, body!!.mass, 0.001f)

        val collider = player.getComponent<BoxCollider2D>()
        assertNotNull(collider)
        assertEquals(48f, collider!!.size.y, 0.001f)

        val script = player.getComponent<ScriptBehaviour>()
        assertNotNull(script)
        assertEquals("PlayerController", script!!.scriptName)
        assertTrue(script.source.contains("function update"))

        val sun = restored.findByName("Sun")
        val light = sun?.getComponent<Light2D>()
        assertEquals(300f, light!!.radius, 0.001f)
    }

    @Test fun colorsRoundTrip() {
        val serializer = SceneSerializer()
        val hex = serializer.colorToHex(Color(0.54f, 0.71f, 0.98f, 0.5f))
        val parsed = serializer.hexToColor(hex)
        assertEquals(0.54f, parsed.r, 0.01f)
        assertEquals(0.71f, parsed.g, 0.01f)
        assertEquals(0.98f, parsed.b, 0.01f)
        assertEquals(0.5f, parsed.a, 0.01f)
    }
}
