package com.sura.engine.rendering.gl

import android.graphics.BitmapFactory
import com.sura.engine.assets.AssetManager
import com.sura.engine.rendering.Texture

/** Loads textures from the asset folder straight into GPU memory with caching. */
class GLTextureLoader(private val renderer: GLRenderer) {
    private val cache = mutableMapOf<String, Texture>()

    fun load(assetManager: AssetManager, path: String): Texture {
        cache[path]?.let { return it }
        return try {
            val bitmap = assetManager.openBitmap(path)
            if (bitmap == null) {
                Texture(renderer.whiteTextureId(), 1, 1, path).also { cache[path] = it }
            } else {
                val id = renderer.createTexture(bitmap)
                bitmap.recycle()
                Texture(id, bitmap.width, bitmap.height, path).also { it.isLoaded = true; cache[path] = it }
            }
        } catch (t: Throwable) {
            Texture(renderer.whiteTextureId(), 1, 1, path)
        }
    }

    fun loadBitmap(path: String): android.graphics.Bitmap? = BitmapFactory.decodeFile(path)

    fun unload(path: String) { cache.remove(path) }
    fun clear() { cache.clear() }
    fun cachedCount(): Int = cache.size
}
