package com.sura.engine

import com.sura.engine.utils.Color
import com.sura.engine.utils.Easing
import com.sura.engine.utils.Rect
import com.sura.engine.utils.Vector2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineUnitTest {
    @Test fun vectorMathWorks() {
        val a = Vector2(3f, 4f)
        assertEquals(5f, a.length(), 0.0001f)
        val b = Vector2(1f, 0f)
        assertEquals(3f, a.dot(b), 0.0001f)
        assertEquals(1f, Vector2(0f, 2f).normalize().y, 0.0001f)
    }

    @Test fun rectIntersectionWorks() {
        val a = Rect(0f, 0f, 10f, 10f)
        val b = Rect(5f, 5f, 10f, 10f)
        val c = Rect(20f, 20f, 5f, 5f)
        assertTrue(a.intersects(b))
        assertTrue(!a.intersects(c))
    }

    @Test fun colorPackingWorks() {
        val c = Color(1f, 0f, 0f, 1f)
        assertEquals(-65536, c.toInt())
    }

    @Test fun easingBoundsAreSane() {
        assertEquals(0f, Easing.easeInOutQuad(0f), 0.0001f)
        assertEquals(1f, Easing.easeInOutQuad(1f), 0.0001f)
        assertTrue(Easing.easeOutCubic(0.5f) > 0.5f)
    }
}
