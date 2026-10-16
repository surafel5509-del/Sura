package com.sura.engine.editor

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.sura.engine.core.Engine
import com.sura.engine.core.Time
import com.sura.engine.input.InputManager
import com.sura.engine.rendering.Camera
import com.sura.engine.rendering.CanvasRenderer
import com.sura.engine.scene.Scene
import com.sura.engine.scene.SceneManager
import com.sura.engine.ui.UIElement
import com.sura.engine.utils.Color

class EngineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    val renderer = CanvasRenderer()
    val camera = Camera()
    var scene: Scene? = null
    var inputManager: InputManager? = null
    var isEditorMode = true
    var showGrid = true
    var gridSize = 64f
    var running = false
    private var thread: Thread? = null
    private val fps = Time()

    // UI overlay
    val uiRoot = com.sura.engine.ui.Canvas()

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread { gameLoop() }.also { it.start() }
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        camera.viewportWidth = w.toFloat()
        camera.viewportHeight = h.toFloat()
        uiRoot.width = w.toFloat()
        uiRoot.height = h.toFloat()
        inputManager?.screenWidth = w
        inputManager?.screenHeight = h
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        thread?.join(500)
        thread = null
    }

    private fun gameLoop() {
        while (running) {
            val start = System.currentTimeMillis()
            update()
            render()
            val elapsed = System.currentTimeMillis() - start
            val sleep = (16 - elapsed).coerceAtLeast(0)
            try { Thread.sleep(sleep) } catch (e: InterruptedException) { }
        }
    }

    private fun update() {
        val now = System.currentTimeMillis()
        fps.update(now)
        Engine.get().update()
        val dt = fps.deltaTime
        // physics
        // ui
        uiRoot.update(dt)
        inputManager?.endFrame()
    }

    private fun render() {
        val canvas = holder.lockCanvas() ?: return
        renderer.canvas = canvas
        renderer.clear(Color(0.12f,0.12f,0.18f,1f))
        renderer.begin(camera)
        if (showGrid) drawGrid(canvas)
        scene?.let { s ->
            // render game objects with SpriteRenderer
            for (go in s.allObjects) {
                val sr = go.getComponent<com.sura.engine.rendering.SpriteRenderer>()
                if (sr != null && go.active) {
                    renderer.drawSprite(sr.sprite ?: com.sura.engine.rendering.Sprite(), go.transform.position, go.transform.rotation, sr.size, sr.color)
                }
            }
        }
        // gizmos in editor mode
        if (isEditorMode) drawGizmos(canvas)
        renderer.end()
        // UI overlay
        uiRoot.render(renderer)
        holder.unlockCanvasAndPost(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val paint = android.graphics.Paint()
        paint.color = android.graphics.Color.argb(40, 100, 100, 120)
        paint.strokeWidth = 1f
        val step = gridSize * camera.zoom
        var x = (camera.viewportWidth/2f - camera.position.x*camera.zoom) % step
        if (x < 0) x += step
        while (x < camera.viewportWidth) { canvas.drawLine(x,0f,x,camera.viewportHeight,paint); x += step }
        var y = (camera.viewportHeight/2f - camera.position.y*camera.zoom) % step
        if (y < 0) y += step
        while (y < camera.viewportHeight) { canvas.drawLine(0f,y,camera.viewportWidth,y,paint); y += step }
        // axes
        val ax = camera.viewportWidth/2f - camera.position.x*camera.zoom
        val ay = camera.viewportHeight/2f - camera.position.y*camera.zoom
        val p2 = android.graphics.Paint(); p2.color = android.graphics.Color.RED; p2.strokeWidth=2f
        canvas.drawLine(ax,0f,ax,camera.viewportHeight,p2)
        p2.color = android.graphics.Color.GREEN
        canvas.drawLine(0f,ay,camera.viewportWidth,ay,p2)
    }

    private fun drawGizmos(canvas: Canvas) {}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        inputManager?.onTouchEvent(event)
        val x = event.x; val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> uiRoot.handleTouch(x, y, UIElement.TouchType.DOWN)
            MotionEvent.ACTION_MOVE -> uiRoot.handleTouch(x, y, UIElement.TouchType.MOVE)
            MotionEvent.ACTION_UP -> uiRoot.handleTouch(x, y, UIElement.TouchType.UP)
        }
        // camera pan with two fingers
        if (event.pointerCount == 2) {
            // pan
        }
        return true
    }
}
