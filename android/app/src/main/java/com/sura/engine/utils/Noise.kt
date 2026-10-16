package com.sura.engine.utils

/** Classic value-noise / Perlin-style generator used for procedural levels and textures. */
object Noise {
    private val permutation = IntArray(512)

    init {
        val p = IntArray(256) { it }
        val rng = Random(1337L)
        for (i in 255 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = p[i]; p[i] = p[j]; p[j] = tmp
        }
        for (i in 0 until 512) permutation[i] = p[i and 255]
    }

    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)
    private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)

    private fun grad(hash: Int, x: Float, y: Float): Float {
        return when (hash and 3) {
            0 -> x + y
            1 -> -x + y
            2 -> x - y
            else -> -x - y
        }
    }

    /** Perlin noise in range roughly [-1, 1]. */
    fun perlin2D(x: Float, y: Float): Float {
        val xi = x.toInt() and 255
        val yi = y.toInt() and 255
        val xf = x - x.toInt()
        val yf = y - y.toInt()
        val u = fade(xf)
        val v = fade(yf)
        val aa = permutation[permutation[xi] + yi]
        val ab = permutation[permutation[xi] + yi + 1]
        val ba = permutation[permutation[xi + 1] + yi]
        val bb = permutation[permutation[xi + 1] + yi + 1]
        val x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1f, yf), u)
        val x2 = lerp(grad(ab, xf, yf - 1f), grad(bb, xf - 1f, yf - 1f), u)
        return lerp(x1, x2, v)
    }

    /** Fractal brownian motion: several octaves of perlin noise. */
    fun fbm(x: Float, y: Float, octaves: Int = 4, persistence: Float = 0.5f, lacunarity: Float = 2f): Float {
        var total = 0f
        var amplitude = 1f
        var frequency = 1f
        var maxValue = 0f
        for (i in 0 until octaves) {
            total += perlin2D(x * frequency, y * frequency) * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= lacunarity
        }
        return if (maxValue == 0f) 0f else total / maxValue
    }

    /** Normalized 0..1 noise helper. */
    fun noise01(x: Float, y: Float): Float = (perlin2D(x, y) + 1f) * 0.5f
}
