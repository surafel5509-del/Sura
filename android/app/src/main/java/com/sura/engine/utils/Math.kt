package com.sura.engine.utils

import kotlin.math.*

data class Vector2(var x: Float = 0f, var y: Float = 0f) {
    fun set(x: Float, y: Float): Vector2 { this.x = x; this.y = y; return this }
    fun add(v: Vector2): Vector2 { x += v.x; y += v.y; return this }
    fun sub(v: Vector2): Vector2 { x -= v.x; y -= v.y; return this }
    fun scale(s: Float): Vector2 { x *= s; y *= s; return this }
    fun length(): Float = sqrt(x * x + y * y)
    fun normalize(): Vector2 {
        val l = length()
        if (l > 0f) { x /= l; y /= l }
        return this
    }
    fun dot(v: Vector2): Float = x * v.x + y * v.y
    fun distance(v: Vector2): Float { val dx = x - v.x; val dy = y - v.y; return sqrt(dx * dx + dy * dy) }
    fun copy(): Vector2 = Vector2(x, y)
    companion object {
        val ZERO = Vector2(0f, 0f)
        val ONE = Vector2(1f, 1f)
        val UP = Vector2(0f, 1f)
        val DOWN = Vector2(0f, -1f)
        val LEFT = Vector2(-1f, 0f)
        val RIGHT = Vector2(1f, 0f)
    }
}

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun normalize(): Vector3 {
        val l = length()
        if (l > 0f) { x /= l; y /= l; z /= l }
        return this
    }
    fun copy(): Vector3 = Vector3(x, y, z)
    companion object { val ZERO = Vector3(0f,0f,0f); val ONE = Vector3(1f,1f,1f) }
}

data class Color(var r: Float = 1f, var g: Float = 1f, var b: Float = 1f, var a: Float = 1f) {
    fun toInt(): Int {
        val ri = (r.coerceIn(0f,1f)*255).toInt()
        val gi = (g.coerceIn(0f,1f)*255).toInt()
        val bi = (b.coerceIn(0f,1f)*255).toInt()
        val ai = (a.coerceIn(0f,1f)*255).toInt()
        return (ai shl 24) or (ri shl 16) or (gi shl 8) or bi
    }
    companion object {
        val WHITE = Color(1f,1f,1f,1f)
        val BLACK = Color(0f,0f,0f,1f)
        val RED = Color(1f,0f,0f,1f)
        val GREEN = Color(0f,1f,0f,1f)
        val BLUE = Color(0f,0f,1f,1f)
        val TRANSPARENT = Color(0f,0f,0f,0f)
        fun fromInt(c: Int): Color = Color(
            ((c shr 16) and 0xFF)/255f,
            ((c shr 8) and 0xFF)/255f,
            (c and 0xFF)/255f,
            ((c shr 24) and 0xFF)/255f
        )
    }
}

data class Rect(var x: Float = 0f, var y: Float = 0f, var w: Float = 0f, var h: Float = 0f) {
    fun left() = x
    fun right() = x + w
    fun top() = y
    fun bottom() = y + h
    fun center() = Vector2(x + w/2f, y + h/2f)
    fun contains(px: Float, py: Float): Boolean = px in x..(x+w) && py in y..(y+h)
    fun intersects(o: Rect): Boolean = !(o.x > x + w || o.x + o.w < x || o.y > y + h || o.y + o.h < y)
    fun copy(): Rect = Rect(x,y,w,h)
}

data class Matrix3(
    var m00: Float = 1f, var m01: Float = 0f, var m02: Float = 0f,
    var m10: Float = 0f, var m11: Float = 1f, var m12: Float = 0f,
    var m20: Float = 0f, var m21: Float = 0f, var m22: Float = 1f
) {
    fun identity(): Matrix3 { m00=1f;m01=0f;m02=0f;m10=0f;m11=1f;m12=0f;m20=0f;m21=0f;m22=1f; return this }
    fun translate(tx: Float, ty: Float): Matrix3 {
        m02 += m00*tx + m01*ty
        m12 += m10*tx + m11*ty
        return this
    }
    fun scale(sx: Float, sy: Float): Matrix3 {
        m00 *= sx; m01 *= sy; m02 *= 0f  // simplified
        return this
    }
}

object Easing {
    fun linear(t: Float): Float = t
    fun easeInQuad(t: Float): Float = t * t
    fun easeOutQuad(t: Float): Float = t * (2f - t)
    fun easeInOutQuad(t: Float): Float = if (t < 0.5f) 2f*t*t else -1f + (4f-2f*t)*t
    fun easeInCubic(t: Float): Float = t * t * t
    fun easeOutCubic(t: Float): Float = (t-1f)*(t-1f)*(t-1f) + 1f
    fun easeInOutCubic(t: Float): Float = if (t < 0.5f) 4f*t*t*t else (t-1f)*(2f*t-2f)*(2f*t-2f)+1f
    fun easeOutBounce(t: Float): Float {
        var tt = t
        if (tt < 1f/2.75f) return 7.5625f*tt*tt
        if (tt < 2f/2.75f) { tt -= 1.5f/2.75f; return 7.5625f*tt*tt + 0.75f }
        if (tt < 2.5f/2.75f) { tt -= 2.25f/2.75f; return 7.5625f*tt*tt + 0.9375f }
        tt -= 2.625f/2.75f; return 7.5625f*tt*tt + 0.984375f
    }
}
