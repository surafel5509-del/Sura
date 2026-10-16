package com.sura.engine.rendering.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import com.sura.engine.core.Logger
import com.sura.engine.rendering.Camera
import com.sura.engine.rendering.Renderer
import com.sura.engine.rendering.Sprite
import com.sura.engine.utils.Color
import com.sura.engine.utils.Matrix4
import com.sura.engine.utils.Rect
import com.sura.engine.utils.Vector2
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Hardware accelerated OpenGL ES 2.0 sprite renderer with batching.
 * Falls back gracefully: if shader compilation fails the renderer reports `isReady = false`.
 */
class GLRenderer : Renderer {
    private val maxQuads = 4096
    private val floatsPerVertex = 8
    private val verticesPerQuad = 6
    private val buffer = FloatArray(maxQuads * verticesPerQuad * floatsPerVertex)
    private var vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(buffer.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    private var vertexCount = 0
    private var program = 0
    private var aPosition = 0
    private var aTexCoord = 0
    private var aColor = 0
    private var uMvp = 0
    private var uTexture = 0
    private var currentTexture = 0
    private var whiteTexture = 0
    private var viewProj = Matrix4()
    private var model = Matrix4()
    private var mvp = Matrix4()
    private var drawing = false

    var isReady = false
        private set
    var drawCalls = 0
        private set
    var renderedQuads = 0
        private set
    var viewportWidth = 1
    var viewportHeight = 1
    var fontAtlas: FontAtlas? = null

    fun initialize(): Boolean {
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) {
            Logger.e("GLRenderer: shader program failed to build")
            isReady = false
            return false
        }
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        aColor = GLES20.glGetAttribLocation(program, "aColor")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
        uTexture = GLES20.glGetUniformLocation(program, "uTexture")
        whiteTexture = createWhiteTexture()
        fontAtlas = FontAtlas().also { it.build(this) }
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        isReady = true
        Logger.i("GLRenderer initialised (GLES20), max quads per batch: $maxQuads")
        return true
    }

    override fun begin(camera: Camera) {
        if (!isReady) return
        viewportWidth = camera.viewportWidth.toInt().coerceAtLeast(1)
        viewportHeight = camera.viewportHeight.toInt().coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        viewProj = Matrix4.projection(camera.viewportWidth, camera.viewportHeight, camera)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, viewProj.m, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(uTexture, 0)
        drawing = true
        vertexCount = 0
        drawCalls = 0
        renderedQuads = 0
        currentTexture = whiteTexture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentTexture)
    }

    override fun end() {
        if (!isReady) return
        flush()
        drawing = false
        vertexCount = 0
        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)
        GLES20.glDisableVertexAttribArray(aColor)
    }

    override fun clear(color: Color) {
        if (!isReady) return
        GLES20.glClearColor(color.r, color.g, color.b, color.a)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    override fun drawSprite(sprite: Sprite, position: Vector2, rotation: Float, scale: Vector2, color: Color) {
        if (!isReady || !drawing) return
        val tw = 32f
        val th = 32f
        drawQuad(
            position, tw * scale.x, th * scale.y, rotation, color,
            sprite.region.x, sprite.region.y,
            sprite.region.x + sprite.region.w, sprite.region.y + sprite.region.h,
            sprite.texture?.id ?: whiteTexture, sprite.flipX, sprite.flipY
        )
    }

    override fun drawRect(rect: Rect, color: Color, filled: Boolean) {
        if (!isReady || !drawing) return
        if (filled) {
            drawQuad(
                Vector2(rect.x + rect.w / 2f, rect.y + rect.h / 2f), rect.w, rect.h, 0f, color,
                0f, 0f, 1f, 1f, whiteTexture, false, false
            )
        } else {
            drawQuad(Vector2(rect.x + rect.w / 2f, rect.y + 1f), rect.w, 2f, 0f, color, 0f, 0f, 1f, 1f, whiteTexture, false, false)
            drawQuad(Vector2(rect.x + rect.w / 2f, rect.y + rect.h - 1f), rect.w, 2f, 0f, color, 0f, 0f, 1f, 1f, whiteTexture, false, false)
            drawQuad(Vector2(rect.x + 1f, rect.y + rect.h / 2f), 2f, rect.h, 0f, color, 0f, 0f, 1f, 1f, whiteTexture, false, false)
            drawQuad(Vector2(rect.x + rect.w - 1f, rect.y + rect.h / 2f), 2f, rect.h, 0f, color, 0f, 0f, 1f, 1f, whiteTexture, false, false)
        }
    }

    override fun drawCircle(center: Vector2, radius: Float, color: Color, filled: Boolean) {
        if (!isReady || !drawing) return
        val segments = 24
        val step = (Math.PI * 2 / segments).toFloat()
        if (filled) {
            for (i in 0 until segments) {
                val a0 = i * step
                val a1 = (i + 1) * step
                val p0 = Vector2(center.x + Math.cos(a0.toDouble()).toFloat() * radius, center.y + Math.sin(a0.toDouble()).toFloat() * radius)
                val p1 = Vector2(center.x + Math.cos(a1.toDouble()).toFloat() * radius, center.y + Math.sin(a1.toDouble()).toFloat() * radius)
                drawTriangle(center, p0, p1, color, whiteTexture)
            }
        } else {
            for (i in 0 until segments) {
                val a0 = i * step
                val a1 = (i + 1) * step
                val p0 = Vector2(center.x + Math.cos(a0.toDouble()).toFloat() * radius, center.y + Math.sin(a0.toDouble()).toFloat() * radius)
                val p1 = Vector2(center.x + Math.cos(a1.toDouble()).toFloat() * radius, center.y + Math.sin(a1.toDouble()).toFloat() * radius)
                drawLine(p0, p1, color, 2f)
            }
        }
    }

    override fun drawLine(a: Vector2, b: Vector2, color: Color, thickness: Float) {
        if (!isReady || !drawing) return
        val dx = b.x - a.x
        val dy = b.y - a.y
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        if (length <= 0.0001f) return
        val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
        val center = Vector2((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        drawQuad(center, length, thickness, angle, color, 0f, 0f, 1f, 1f, whiteTexture, false, false)
    }

    override fun drawText(text: String, position: Vector2, size: Float, color: Color) {
        if (!isReady || !drawing) return
        val atlas = fontAtlas ?: return
        val textureId = atlas.textureId
        var cursor = position.x
        val baseline = position.y
        val scale = size / atlas.baseSize
        for (ch in text) {
            val glyph = atlas.glyph(ch) ?: continue
            if (ch != ' ') {
                drawQuad(
                    Vector2(cursor + glyph.width * scale / 2f, baseline - glyph.height * scale / 2f),
                    glyph.width * scale, glyph.height * scale, 0f, color,
                    glyph.u0, glyph.v0, glyph.u1, glyph.v1, textureId, false, false
                )
            }
            cursor += glyph.advance * scale
        }
    }

    // ---------------------------------------------------------------- batch internals

    private fun drawQuad(
        center: Vector2, width: Float, height: Float, rotationRadians: Float, color: Color,
        u0: Float, v0: Float, u1: Float, v1: Float, textureId: Int, flipX: Boolean, flipY: Boolean
    ) {
        if (textureId != currentTexture) {
            flush()
            currentTexture = textureId
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        }
        model.identity()
            .translate(center.x, center.y, 0f)
            .rotate(rotationRadians)
            .scale(width, height)
        mvp.set(viewProj).multiply(model)
        val hw = 0.5f
        val hh = 0.5f
        val l = if (flipX) hw else -hw
        val r = if (flipX) -hw else hw
        val t = if (flipY) -hh else hh
        val b = if (flipY) hh else -hh
        val uL = if (flipX) u1 else u0
        val uR = if (flipX) u0 else u1
        val vT = if (flipY) v1 else v0
        val vB = if (flipY) v0 else v1
        addVertex(l, b, uL, vB, color)
        addVertex(r, b, uR, vB, color)
        addVertex(r, t, uR, vT, color)
        addVertex(l, b, uL, vB, color)
        addVertex(r, t, uR, vT, color)
        addVertex(l, t, uL, vT, color)
    }

    private fun drawTriangle(a: Vector2, b: Vector2, c: Vector2, color: Color, textureId: Int) {
        if (textureId != currentTexture) {
            flush()
            currentTexture = textureId
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        }
        mvp.set(viewProj)
        addTransformed(a.x, a.y, 0.5f, 0.5f, color)
        addTransformed(b.x, b.y, 0.5f, 0.5f, color)
        addTransformed(c.x, c.y, 0.5f, 0.5f, color)
    }

    private fun addTransformed(x: Float, y: Float, u: Float, v: Float, color: Color) {
        addVertexRaw(mvp.m[0] * x + mvp.m[4] * y + mvp.m[12], mvp.m[1] * x + mvp.m[5] * y + mvp.m[13], u, v, color)
    }

    private fun addVertex(x: Float, y: Float, u: Float, v: Float, color: Color) {
        addTransformed(x, y, u, v, color)
    }

    private fun addVertexRaw(x: Float, y: Float, u: Float, v: Float, color: Color) {
        if (vertexCount + verticesPerQuad > maxQuads * verticesPerQuad) flush()
        val index = vertexCount * floatsPerVertex
        buffer[index] = x
        buffer[index + 1] = y
        buffer[index + 2] = u
        buffer[index + 3] = v
        buffer[index + 4] = color.r
        buffer[index + 5] = color.g
        buffer[index + 6] = color.b
        buffer[index + 7] = color.a
        vertexCount++
    }

    private fun flush() {
        if (vertexCount == 0) return
        vertexBuffer.clear()
        vertexBuffer.put(buffer, 0, vertexCount * floatsPerVertex)
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glEnableVertexAttribArray(aColor)
        val stride = floatsPerVertex * 4
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        vertexBuffer.position(2)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        vertexBuffer.position(4)
        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        drawCalls++
        renderedQuads += vertexCount / verticesPerQuad
        vertexCount = 0
        vertexBuffer.position(0)
    }

    // ---------------------------------------------------------------- texture helpers

    fun whiteTextureId(): Int = whiteTexture

    fun createTexture(bitmap: Bitmap): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val id = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return id
    }

    private fun createWhiteTexture(): Int {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        val id = createTexture(bitmap)
        bitmap.recycle()
        return id
    }

    private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (vertexShader == 0 || fragmentShader == 0) return 0
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vertexShader)
        GLES20.glAttachShader(p, fragmentShader)
        GLES20.glLinkProgram(p)
        val status = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Logger.e("GLRenderer link error: " + GLES20.glGetProgramInfoLog(p))
            GLES20.glDeleteProgram(p)
            return 0
        }
        return p
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Logger.e("GLRenderer shader error: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            attribute vec4 aColor;
            varying vec2 vTexCoord;
            varying vec4 vColor;
            void main() {
                gl_Position = uMvp * vec4(aPosition.xy, 0.0, 1.0);
                vTexCoord = aTexCoord;
                vColor = aColor;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 vTexCoord;
            varying vec4 vColor;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord) * vColor;
            }
        """
    }
}
