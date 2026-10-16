package com.sura.engine.utils

import kotlin.math.cos
import kotlin.math.sin

/** Column-major 4x4 matrix, same layout OpenGL expects. */
class Matrix4 {
    val m = FloatArray(16)

    init { identity() }

    fun identity(): Matrix4 {
        for (i in 0 until 16) m[i] = 0f
        m[0] = 1f; m[5] = 1f; m[10] = 1f; m[15] = 1f
        return this
    }

    fun set(other: Matrix4): Matrix4 { System.arraycopy(other.m, 0, m, 0, 16); return this }

    fun copy(): Matrix4 = Matrix4().set(this)

    fun multiply(other: Matrix4): Matrix4 {
        val a = m
        val b = other.m
        val r = FloatArray(16)
        for (c in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) sum += a[k * 4 + row] * b[c * 4 + k]
                r[c * 4 + row] = sum
            }
        }
        System.arraycopy(r, 0, m, 0, 16)
        return this
    }

    fun ortho(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4 {
        val lr = 1f / (right - left)
        val bt = 1f / (top - bottom)
        val nf = 1f / (far - near)
        identity()
        m[0] = 2f * lr
        m[5] = 2f * bt
        m[10] = -2f * nf
        m[12] = -(right + left) * lr
        m[13] = -(top + bottom) * bt
        m[14] = -(far + near) * nf
        return this
    }

    fun translate(x: Float, y: Float, z: Float): Matrix4 {
        val t = Matrix4()
        t.m[12] = x; t.m[13] = y; t.m[14] = z
        return multiply(t)
    }

    fun rotate(zRadians: Float): Matrix4 {
        val c = cos(zRadians)
        val s = sin(zRadians)
        val r = Matrix4()
        r.m[0] = c; r.m[1] = s
        r.m[4] = -s; r.m[5] = c
        return multiply(r)
    }

    fun scale(x: Float, y: Float): Matrix4 {
        val s = Matrix4()
        s.m[0] = x
        s.m[5] = y
        return multiply(s)
    }

    companion object {
        fun projection(width: Float, height: Float, camera: com.sura.engine.rendering.Camera): Matrix4 {
            val halfW = width / 2f / camera.zoom
            val halfH = height / 2f / camera.zoom
            val mvp = Matrix4()
            mvp.ortho(
                camera.position.x - halfW, camera.position.x + halfW,
                camera.position.y - halfH, camera.position.y + halfH,
                -1f, 1f
            )
            return mvp
        }
    }
}
