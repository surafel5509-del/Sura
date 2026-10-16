package com.sura.engine.utils

/** Deterministic Xorshift128+ style generator so games can be fully reproducible offline. */
class Random(seed: Long = 0x9E3779B97F4A7C15uL.toLong()) {
    private var s0: Long
    private var s1: Long

    init {
        var s = if (seed == 0L) 0x2545F4914F6CDD1DL else seed
        s0 = splitmix(s)
        s1 = splitmix(s0)
        if (s0 == 0L && s1 == 0L) s0 = 1L
    }

    private fun splitmix(x: Long): Long {
        var z = x + 0x9E3779B97F4A7C15uL.toLong()
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9uL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBuL.toLong()
        return z xor (z ushr 31)
    }

    fun nextLong(): Long {
        var x = s0
        val y = s1
        s0 = y
        x = x xor (x shl 23)
        s1 = x xor y xor (x ushr 17) xor (y ushr 26)
        return s1 + y
    }

    fun nextInt(): Int = (nextLong() ushr 33).toInt()
    fun nextInt(bound: Int): Int {
        if (bound <= 0) return 0
        return (nextInt().toLong() and 0x7FFFFFFFL).rem(bound.toLong()).toInt()
    }
    fun nextFloat(): Float = (nextInt() ushr 8) / 16777216f
    fun range(min: Float, max: Float): Float = min + (max - min) * nextFloat()
    fun range(min: Int, max: Int): Int = if (max <= min) min else min + nextInt(max - min + 1)
    fun chance(probability: Float): Boolean = nextFloat() < probability
    fun pick(items: List<String>): String? = if (items.isEmpty()) null else items[nextInt(items.size)]
    fun reseed(seed: Long) { s0 = splitmix(seed); s1 = splitmix(s0) }
}
