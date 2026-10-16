package com.sura.engine.rendering

class Texture(val id: Int = 0, val width: Int = 0, val height: Int = 0, val path: String = "") {
    var isLoaded = false
    fun bind() {}
    fun unbind() {}
    companion object { val WHITE = Texture(0,1,1,"white") }
}
