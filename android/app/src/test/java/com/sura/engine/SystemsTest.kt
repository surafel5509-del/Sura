package com.sura.engine

import com.sura.engine.editor.UndoStack
import com.sura.engine.nav.NavGrid
import com.sura.engine.physics.BodyType
import com.sura.engine.physics.PhysicsWorld
import com.sura.engine.physics.RigidBody2D
import com.sura.engine.physics.BoxCollider2D
import com.sura.engine.scene.Scene
import com.sura.engine.tilemap.Tilemap
import com.sura.engine.utils.Geometry
import com.sura.engine.utils.Noise
import com.sura.engine.utils.Random
import com.sura.engine.utils.Vector2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemsTest {

    @Test fun deterministicRandomIsReproducible() {
        val a = Random(1234L)
        val b = Random(1234L)
        for (i in 0 until 50) {
            assertEquals(a.nextInt(1000), b.nextInt(1000))
        }
        val c = Random(999L)
        assertTrue(c.nextFloat() in 0f..1f)
        assertTrue(c.range(5, 10) in 5..10)
    }

    @Test fun noiseIsDeterministicAndBounded() {
        val first = Noise.perlin2D(1.5f, 2.5f)
        val second = Noise.perlin2D(1.5f, 2.5f)
        assertEquals(first, second, 0.000001f)
        for (i in 0 until 40) {
            val value = Noise.perlin2D(i * 0.37f, i * 0.11f)
            assertTrue("perlin value out of range: $value", value >= -1.6f && value <= 1.6f)
        }
        val smooth = Noise.fbm(0.4f, 0.9f, octaves = 3)
        assertTrue("fbm out of range: $smooth", smooth >= -1.6f && smooth <= 1.6f)
    }

    @Test fun geometryHelpers() {
        assertTrue(Geometry.circleRect(Vector2(0f, 0f), 5f, com.sura.engine.utils.Rect(-1f, -1f, 2f, 2f)))
        assertFalse(Geometry.circleRect(Vector2(50f, 0f), 5f, com.sura.engine.utils.Rect(-1f, -1f, 2f, 2f)))
        val square = listOf(Vector2(0f, 0f), Vector2(10f, 0f), Vector2(10f, 10f), Vector2(0f, 10f))
        assertTrue(Geometry.pointInPolygon(Vector2(5f, 5f), square))
        assertFalse(Geometry.pointInPolygon(Vector2(15f, 5f), square))
        assertEquals(5f, Geometry.lerp(0f, 10f, 0.5f), 0.0001f)
        assertEquals(1f, Geometry.clamp(5f, 0f, 1f), 0.0001f)
    }

    @Test fun tilemapPaintAndSolidQuery() {
        val map = Tilemap(8, 8, 32f)
        val layer = map.addLayer("Ground")
        layer.solid = true
        layer.setTile(2, 2, 1)
        assertEquals(1, layer.getTile(2, 2))
        assertTrue(map.isSolidTile(2, 2))
        assertFalse(map.isSolidTile(3, 3))
        // world -> tile conversion
        val tile = map.worldToTile(Vector2(80f, 80f))
        assertEquals(2f, tile.x, 0.0001f)
        assertEquals(2f, tile.y, 0.0001f)
        val world = map.tileToWorld(2, 2)
        assertEquals(80f, world.x, 0.0001f)
    }

    @Test fun pathfindingFindsRouteAroundObstacles() {
        val grid = NavGrid(12, 12, 16f)
        grid.setBlockedRect(5, 0, 1, 9)
        val path = grid.findPathCells(1, 1, 10, 1)
        assertTrue("path should exist", path.isNotEmpty())
        assertTrue(path.contains(Pair(10, 1)))
        // a blocked target cannot be reached
        grid.setBlocked(11, 11, true)
        assertTrue(grid.findPathCells(1, 1, 11, 11).isEmpty())
        // blocked start or fully blocked grid also returns no path
        grid.setBlockedRect(0, 0, 12, 12, true)
        assertTrue(grid.findPathCells(1, 1, 5, 5).isEmpty())
        grid.clear()
        assertTrue(grid.findPathCells(1, 1, 5, 5).isNotEmpty())
    }

    @Test fun physicsAppliesGravityAndSeparatesBodies() {
        val scene = Scene("Physics")
        val ground = scene.createGameObject("Ground")
        ground.transform.position.set(0f, 0f)
        ground.addComponent(BoxCollider2D().apply { size.set(200f, 20f) })
        val dynamic = scene.createGameObject("Crate")
        dynamic.transform.position.set(0f, 60f)
        val body = RigidBody2D()
        body.bodyType = BodyType.DYNAMIC
        body.mass = 1f
        dynamic.addComponent(body)
        dynamic.addComponent(BoxCollider2D().apply { size.set(20f, 20f) })

        val world = PhysicsWorld()
        world.registerBody(body)
        world.registerCollider(dynamic.getComponent<BoxCollider2D>()!!)
        world.registerCollider(ground.getComponent<BoxCollider2D>()!!)

        repeat(30) { world.fixedUpdate(1f / 60f) }
        assertTrue("gravity should pull the crate down", dynamic.transform.position.y < 60f)
        assertTrue("crate should rest above the ground", dynamic.transform.position.y > 0f)
        assertEquals(2, world.colliderCount())
    }

    @Test fun raycastHitsColliders() {
        val scene = Scene("Ray")
        val wall = scene.createGameObject("Wall")
        wall.transform.position.set(100f, 0f)
        wall.addComponent(BoxCollider2D().apply { size.set(20f, 100f) })
        val world = PhysicsWorld()
        world.registerCollider(wall.getComponent<BoxCollider2D>()!!)
        val hit = world.raycast(Vector2(0f, 0f), Vector2(1f, 0f), 500f)
        assertTrue("ray should hit the wall", hit != null)
        assertTrue(hit!!.distance in 80f..100f)
    }

    @Test fun undoStackSupportsUndoAndRedo() {
        val stack = UndoStack()
        var value = 0
        stack.push("increment", { value -= 1 }, { value += 1 })
        value = 1
        assertTrue(stack.canUndo)
        stack.undo()
        assertEquals(0, value)
        assertTrue(stack.canRedo)
        stack.redo()
        assertEquals(1, value)
    }
}
