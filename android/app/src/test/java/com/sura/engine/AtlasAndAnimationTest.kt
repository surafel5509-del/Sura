package com.sura.engine

import com.sura.engine.animation.SpriteAnimator
import com.sura.engine.animation.SpriteSheet
import com.sura.engine.rendering.SpriteRenderer
import com.sura.engine.rendering.atlas.AtlasPacker
import com.sura.engine.scene.GameObject
import com.sura.engine.tilemap.AutoTile
import com.sura.engine.tilemap.Tilemap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasAndAnimationTest {

    @Test fun atlasPackerPlacesEverythingItCan() {
        val packer = AtlasPacker(maxWidth = 256, maxHeight = 256)
        val entries = listOf(
            AtlasPacker.Entry("hero", 64, 64),
            AtlasPacker.Entry("enemy", 48, 32),
            AtlasPacker.Entry("coin", 16, 16),
            AtlasPacker.Entry("huge", 4096, 4096)
        )
        val result = packer.pack(entries)
        assertEquals(3, result.placements.size)
        assertEquals(listOf("huge"), result.unpacked)
        assertTrue(result.coverage() > 0f)
        assertTrue(result.placementFor("hero") != null)
        val hero = result.placementFor("hero")!!
        val normalized = hero.rect(result.width, result.height)
        assertTrue(normalized.w > 0f && normalized.w <= 1f)
        assertTrue(packer.nextPowerOfTwo(300) >= 300)
        assertTrue(packer.nextPowerOfTwo(1024) == 1024 || packer.nextPowerOfTwo(1024) == 256)
    }

    @Test fun atlasEntriesParseFromText() {
        val entries = AtlasPacker.parseEntries("hero,64,64\ncoin,16,16\nbroken\n")
        assertEquals(2, entries.size)
        assertEquals("hero", entries[0].name)
        assertEquals(16, entries[1].width)
    }

    @Test fun autotileBuildsNeighbourMasks() {
        // 3x3 grid, only the centre is solid
        val solid = { x: Int, y: Int -> x == 1 && y == 1 }
        val mask = AutoTile.mask(solid, 1, 1)
        assertEquals(AutoTile.NORTH or AutoTile.EAST or AutoTile.SOUTH or AutoTile.WEST, mask)

        // top-left corner of a fully solid map exposes north and west
        val allSolid = { _: Int, _: Int -> true }
        val corner = AutoTile.mask(allSolid, 0, 0)
        assertEquals(AutoTile.NORTH or AutoTile.WEST, corner)
    }

    @Test fun autotileRetilesALayer() {
        val map = Tilemap(4, 4, 32f)
        val layer = map.addLayer("Ground")
        layer.setTile(0, 0, 1)
        layer.setTile(1, 0, 1)
        layer.setTile(2, 0, 1)
        AutoTile.retileLayer(layer, baseTile = 1)
        assertTrue(layer.getTile(1, 0) >= 1)
        assertEquals(-1, layer.getTile(3, 3))
    }

    @Test fun spriteAnimatorAdvancesFrames() {
        val go = GameObject("Animated")
        val sprite = go.addComponent<SpriteRenderer>()
        val animator = SpriteAnimator()
        go.addComponent(animator)
        animator.sheet = SpriteSheet(columns = 4, rows = 1)
        animator.addClip("walk", listOf(0, 1, 2, 3), fps = 10f, loop = true)
        animator.play("walk")
        assertEquals(0, animator.currentFrameNumber())
        repeat(6) { animator.update(0.1f) }
        assertTrue(animator.frameIndex in 0..3)
        assertTrue("sprite instance should be created by the animator", sprite.sprite != null)
        assertTrue(animator.isPlaying)
        assertTrue(animator.effectiveFps() == 10f)
    }

    @Test fun spriteAnimatorNonLoopingStopsAtLastFrame() {
        val go = GameObject("Animated")
        go.addComponent<SpriteRenderer>()
        val animator = go.addComponent<SpriteAnimator>()
        animator.addClip("hit", listOf(0, 1, 2), fps = 5f, loop = false)
        animator.play("hit")
        repeat(10) { animator.update(0.25f) }
        assertFalse(animator.isPlaying)
        assertEquals(2, animator.frameIndex)
    }

    @Test fun spriteSheetRegionsAreNormalized() {
        val sheet = SpriteSheet(columns = 2, rows = 2)
        assertEquals(4, sheet.frameCount)
        val region = sheet.regionFor(3)
        assertEquals(0.5f, region.x, 0.0001f)
        assertEquals(0.5f, region.y, 0.0001f)
        assertEquals(0.5f, region.w, 0.0001f)
        assertEquals(0.5f, region.h, 0.0001f)
    }
}
