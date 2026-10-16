package com.sura.engine.input

import android.view.MotionEvent
import com.sura.engine.core.EngineService
import com.sura.engine.utils.Vector2

class InputManager : EngineService {
    val touches = mutableMapOf<Int, Vector2>()
    val touchDown = mutableSetOf<Int>()
    val touchUp = mutableSetOf<Int>()
    var mousePosition = Vector2()
    var mouseDelta = Vector2()
    val keysDown = mutableSetOf<Int>()
    val keysPressed = mutableSetOf<Int>()
    val keysReleased = mutableSetOf<Int>()

    var screenWidth = 1
    var screenHeight = 1

    fun onTouchEvent(e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = e.actionIndex
                val id = e.getPointerId(idx)
                touches[id] = Vector2(e.getX(idx), e.getY(idx))
                touchDown.add(id)
                mousePosition.set(e.getX(idx), e.getY(idx))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    touches[id] = Vector2(e.getX(i), e.getY(i))
                }
                if (e.pointerCount > 0) mousePosition.set(e.getX(0), e.getY(0))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = e.actionIndex
                val id = e.getPointerId(idx)
                touches.remove(id)
                touchUp.add(id)
            }
            MotionEvent.ACTION_CANCEL -> touches.clear()
        }
    }

    fun isTouchDown(id: Int = 0): Boolean = touches.containsKey(id) && touchDown.contains(id)
    fun getTouch(id: Int): Vector2? = touches[id]
    fun getTouchCount(): Int = touches.size

    fun endFrame() {
        touchDown.clear()
        touchUp.clear()
        keysPressed.clear()
        keysReleased.clear()
        mouseDelta = Vector2(0f,0f)
    }

    override fun update(dt: Float) { endFrame() }
}
