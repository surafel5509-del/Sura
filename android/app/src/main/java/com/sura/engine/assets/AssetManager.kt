package com.sura.engine.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.sura.engine.core.EngineService
import com.sura.engine.rendering.Texture
import java.io.IOException

class AssetManager(private val context: Context) : EngineService {
    private val textures = mutableMapOf<String, Texture>()
    private val bytes = mutableMapOf<String, ByteArray>()
    var basePath = "assets"

    fun loadTexture(path: String): Texture {
        textures[path]?.let { return it }
        return try {
            val bmp = context.assets.open(path).use { BitmapFactory.decodeStream(it) }
            val t = Texture(0, bmp?.width ?: 0, bmp?.height ?: 0, path)
            t.isLoaded = bmp != null
            textures[path] = t
            t
        } catch (e: IOException) {
            Texture(0,0,0,path)
        }
    }
    fun getTexture(path: String): Texture? = textures[path]
    fun loadBytes(path: String): ByteArray? {
        bytes[path]?.let { return it }
        return try {
            val b = context.assets.open(path).readBytes()
            bytes[path] = b
            b
        } catch (e: IOException) { null }
    }
    fun loadText(path: String): String? = loadBytes(path)?.toString(Charsets.UTF_8)
    fun exists(path: String): Boolean = try { context.assets.open(path).close(); true } catch (e: IOException) { false }
    fun list(dir: String): Array<String> = try { context.assets.list(dir) ?: emptyArray() } catch (e: IOException) { emptyArray() }
    override fun update(dt: Float) {}
}
