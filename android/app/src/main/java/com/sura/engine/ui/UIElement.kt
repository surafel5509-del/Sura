package com.sura.engine.ui

import com.sura.engine.utils.Color
import com.sura.engine.utils.Rect
import com.sura.engine.utils.Vector2

abstract class UIElement {
    var id: String = ""
    var name: String = "UIElement"
    var rect = Rect()
    var anchor = Anchor.TOP_LEFT
    var pivot = Vector2(0.5f, 0.5f)
    var visible = true
    var interactable = true
    var color = Color.WHITE
    var parent: UIElement? = null
    val children = mutableListOf<UIElement>()
    var onClick: (() -> Unit)? = null
    var onHover: (() -> Unit)? = null
    var onValueChanged: ((Any?) -> Unit)? = null

    enum class Anchor { TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, STRETCH }

    fun addChild(e: UIElement) { e.parent = this; children.add(e) }
    fun removeChild(e: UIElement) { children.remove(e); e.parent = null }
    fun contains(x: Float, y: Float): Boolean = rect.contains(x, y)

    open fun update(dt: Float) { for (c in children) c.update(dt) }
    open fun render(renderer: com.sura.engine.rendering.Renderer) {
        if (!visible) return
        for (c in children) c.render(renderer)
    }
    open fun handleTouch(x: Float, y: Float, type: TouchType): Boolean {
        for (c in children.reversed()) if (c.handleTouch(x, y, type)) return true
        if (contains(x, y) && interactable) {
            if (type == TouchType.DOWN) onClick?.invoke()
            return true
        }
        return false
    }
    enum class TouchType { DOWN, MOVE, UP, CANCEL }
}

class Canvas : UIElement() {
    var width = 0f
    var height = 0f
    init { name = "Canvas" }
}

class Panel : UIElement() {
    var backgroundColor = Color(0.2f,0.2f,0.25f,0.9f)
    var borderColor = Color(0.4f,0.4f,0.5f,1f)
    var borderWidth = 1f
    var cornerRadius = 4f
    override fun render(renderer: com.sura.engine.rendering.Renderer) {
        if (!visible) return
        renderer.drawRect(rect, backgroundColor, true)
        super.render(renderer)
    }
}

class Label : UIElement() {
    var text = ""
    var fontSize = 14f
    var textColor = Color.WHITE
    var alignment = Alignment.LEFT
    enum class Alignment { LEFT, CENTER, RIGHT }
    override fun render(renderer: com.sura.engine.rendering.Renderer) {
        if (!visible) return
        renderer.drawText(text, com.sura.engine.utils.Vector2(rect.x + 4f, rect.y + fontSize + 2f), fontSize, textColor)
        super.render(renderer)
    }
}

class Button : UIElement() {
    var text = "Button"
    var fontSize = 14f
    var textColor = Color.WHITE
    var backgroundColor = Color(0.3f,0.3f,0.4f,1f)
    var hoverColor = Color(0.4f,0.4f,0.5f,1f)
    var pressedColor = Color(0.2f,0.2f,0.3f,1f)
    var isPressed = false
    var isHovered = false
    override fun render(renderer: com.sura.engine.rendering.Renderer) {
        if (!visible) return
        val bg = if (isPressed) pressedColor else if (isHovered) hoverColor else backgroundColor
        renderer.drawRect(rect, bg, true)
        renderer.drawText(text, com.sura.engine.utils.Vector2(rect.x + rect.w/2f - text.length*3f, rect.y + rect.h/2f + 4f), fontSize, textColor)
        super.render(renderer)
    }
    override fun handleTouch(x: Float, y: Float, type: UIElement.TouchType): Boolean {
        if (!contains(x,y)) { isHovered = false; isPressed = false; return false }
        when (type) {
            UIElement.TouchType.DOWN -> { isPressed = true; return true }
            UIElement.TouchType.UP -> { if (isPressed) onClick?.invoke(); isPressed = false; return true }
            UIElement.TouchType.MOVE -> { isHovered = true; return true }
            else -> {}
        }
        return super.handleTouch(x,y,type)
    }
}

class Image : UIElement() {
    var texturePath: String = ""
    var tint = Color.WHITE
    override fun render(renderer: com.sura.engine.rendering.Renderer) {
        if (!visible) return
        renderer.drawRect(rect, tint, true)
        super.render(renderer)
    }
}

class TextField : UIElement() {
    var text = ""
    var placeholder = ""
    var fontSize = 14f
    var textColor = Color.WHITE
    var backgroundColor = Color(0.15f,0.15f,0.2f,1f)
    override fun render(renderer: com.sura.engine.rendering.Renderer) {
        if (!visible) return
        renderer.drawRect(rect, backgroundColor, true)
        val t = if (text.isEmpty()) placeholder else text
        renderer.drawText(t, com.sura.engine.utils.Vector2(rect.x+4f, rect.y+fontSize+2f), fontSize, textColor)
        super.render(renderer)
    }
}

class Slider : UIElement() {
    var value = 0.5f
    var min = 0f
    var max = 1f
    var backgroundColor = Color(0.2f,0.2f,0.25f,1f)
    var fillColor = Color(0.5f,0.7f,1f,1f)
    var handleColor = Color.WHITE
    override fun render(renderer: com.sura.engine.rendering.Renderer) {
        if (!visible) return
        renderer.drawRect(rect, backgroundColor, true)
        val ratio = ((value - min)/(max-min)).coerceIn(0f,1f)
        renderer.drawRect(com.sura.engine.utils.Rect(rect.x, rect.y, rect.w*ratio, rect.h), fillColor, true)
        super.render(renderer)
    }
    override fun handleTouch(x: Float, y: Float, type: UIElement.TouchType): Boolean {
        if (!contains(x,y)) return false
        val ratio = ((x - rect.x)/rect.w).coerceIn(0f,1f)
        value = min + (max-min)*ratio
        onValueChanged?.invoke(value)
        return true
    }
}

class Toggle : UIElement() {
    var isOn = false
    var onColor = Color(0.3f,0.7f,0.3f,1f)
    var offColor = Color(0.4f,0.4f,0.4f,1f)
    override fun render(renderer: com.sura.engine.rendering.Renderer) {
        if (!visible) return
        renderer.drawRect(rect, if (isOn) onColor else offColor, true)
        super.render(renderer)
    }
    override fun handleTouch(x: Float, y: Float, type: UIElement.TouchType): Boolean {
        if (!contains(x,y)) return false
        if (type == UIElement.TouchType.UP) { isOn = !isOn; onValueChanged?.invoke(isOn); onClick?.invoke() }
        return true
    }
}

class ScrollView : UIElement() {
    var scrollOffset = Vector2()
    var contentSize = Vector2()
    override fun update(dt: Float) { super.update(dt) }
}

class Dropdown : UIElement() {
    val options = mutableListOf<String>()
    var selectedIndex = 0
    var isOpen = false
    var backgroundColor = Color(0.2f,0.2f,0.25f,1f)
    override fun render(renderer: com.sura.engine.rendering.Renderer) {
        if (!visible) return
        renderer.drawRect(rect, backgroundColor, true)
        val label = if (options.isEmpty()) "" else options[selectedIndex]
        renderer.drawText(label, com.sura.engine.utils.Vector2(rect.x+4f, rect.y+14f), 12f, Color.WHITE)
        super.render(renderer)
    }
}

class ProgressBar : UIElement() {
    var value = 0f
    var max = 1f
    var fillColor = Color(0.3f,0.7f,1f,1f)
    var backgroundColor = Color(0.2f,0.2f,0.25f,1f)
    override fun render(renderer: com.sura.engine.rendering.Renderer) {
        if (!visible) return
        renderer.drawRect(rect, backgroundColor, true)
        val ratio = (value/max).coerceIn(0f,1f)
        renderer.drawRect(com.sura.engine.utils.Rect(rect.x, rect.y, rect.w*ratio, rect.h), fillColor, true)
        super.render(renderer)
    }
}

class LayoutGroup : UIElement() {
    enum class Direction { HORIZONTAL, VERTICAL }
    var direction = Direction.VERTICAL
    var spacing = 4f
    var padding = 4f
    fun arrange() {
        var cursor = padding
        for (c in children) {
            if (direction == Direction.VERTICAL) {
                c.rect.x = rect.x + padding
                c.rect.y = rect.y + cursor
                cursor += c.rect.h + spacing
            } else {
                c.rect.x = rect.x + cursor
                c.rect.y = rect.y + padding
                cursor += c.rect.w + spacing
            }
        }
    }
    override fun update(dt: Float) { arrange(); super.update(dt) }
}
