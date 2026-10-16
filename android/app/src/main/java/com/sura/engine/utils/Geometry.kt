package com.sura.engine.utils

object Geometry {
    fun pointInPolygon(point: Vector2, polygon: List<Vector2>): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if ((pi.y > point.y) != (pj.y > point.y) &&
                (point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x)) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    fun circleCircle(c1: Vector2, r1: Float, c2: Vector2, r2: Float): Boolean {
        val dx = c1.x - c2.x
        val dy = c1.y - c2.y
        val rr = r1 + r2
        return dx * dx + dy * dy <= rr * rr
    }

    fun circleRect(center: Vector2, radius: Float, rect: Rect): Boolean {
        val cx = center.x.coerceIn(rect.x, rect.x + rect.w)
        val cy = center.y.coerceIn(rect.y, rect.y + rect.h)
        val dx = center.x - cx
        val dy = center.y - cy
        return dx * dx + dy * dy <= radius * radius
    }

    fun pointInRect(p: Vector2, r: Rect): Boolean = p.x >= r.x && p.x <= r.x + r.w && p.y >= r.y && p.y <= r.y + r.h

    fun clamp(value: Float, min: Float, max: Float): Float = if (value < min) min else if (value > max) max else value

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * clamp(t, 0f, 1f)

    fun angleBetween(a: Vector2, b: Vector2): Float = kotlin.math.atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble()).toFloat()

    fun distance(a: Vector2, b: Vector2): Float = a.distance(b)

    fun reflect(velocity: Vector2, normal: Vector2): Vector2 {
        val d = velocity.dot(normal)
        return Vector2(velocity.x - 2f * d * normal.x, velocity.y - 2f * d * normal.y)
    }
}
