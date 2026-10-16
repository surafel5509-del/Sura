package com.sura.engine.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.sura.engine.core.Engine
import com.sura.engine.core.Time
import com.sura.engine.input.InputManager
import com.sura.engine.physics.PhysicsWorld
import com.sura.engine.rendering.Camera
import com.sura.engine.rendering.CameraComponent
import com.sura.engine.rendering.CanvasRenderer
import com.sura.engine.rendering.Light2D
import com.sura.engine.rendering.SceneRenderer
import com.sura.engine.rendering.SpriteRenderer
import com.sura.engine.scene.GameObject
import com.sura.engine.scene.Scene
import com.sura.engine.scene.SceneManager
import com.sura.engine.tilemap.TilemapComponent
import com.sura.engine.utils.Color
import com.sura.engine.utils.Rect
import com.sura.engine.utils.Vector2
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * The scene/game surface: renders the active scene with the Canvas backend and adds all
 * editor interactions (selection, gizmo drag, pan, zoom, tile painting).
 */
class EngineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    val renderer = CanvasRenderer()
    val camera = Camera()
    var scene: Scene? = null
    var inputManager: InputManager? = null
    var editorState: EditorState? = null
    var sceneManager: SceneManager? = null
    var isEditorMode = true
    var showGrid = true
    var gridSize = 64f
    var showStats = true
    var showColliders = false
    var backgroundColor = Color(0.07f, 0.08f, 0.13f, 1f)

    private val time = Time()
    private val sceneRenderer = SceneRenderer(renderer)
    private var thread: Thread? = null
    @Volatile private var running = false

    // interaction state
    private var dragTarget: GameObject? = null
    private var dragStart = Vector2()
    private var objectStart = Vector2()
    private var dragging = false
    private var panning = false
    private var lastPanPoint = Vector2()
    private var pinchDistance = 0f
    private var pinchZoomStart = 1f
    private var activePointers = 0
    @Volatile private var stepRequested = false
    private val gridPaint = Paint()
    private val axisPaint = Paint()
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        holder.addCallback(this)
        isFocusable = true
        selectionPaint.style = Paint.Style.STROKE
        selectionPaint.strokeWidth = 2f
        overlayPaint.textSize = 26f
        overlayPaint.color = android.graphics.Color.argb(220, 205, 214, 244)
    }

    // ------------------------------------------------------------------ surface

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread { frameLoop() }.also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        inputManager?.screenWidth = width
        inputManager?.screenHeight = height
        if (editorState?.scene == null) editorState?.scene = scene
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        thread?.join(400)
        thread = null
    }

    // ------------------------------------------------------------------ loop

    private fun frameLoop() {
        while (running) {
            val started = System.currentTimeMillis()
            try {
                updateFrame()
                drawFrame()
            } catch (t: Throwable) {
                com.sura.engine.core.Logger.e("Frame error: ${t.message}", t = t)
            }
            val elapsed = System.currentTimeMillis() - started
            val sleep = 16 - elapsed
            if (sleep > 0) {
                try { Thread.sleep(sleep) } catch (e: InterruptedException) { break }
            }
        }
    }

    private fun updateFrame() {
        val now = System.currentTimeMillis()
        time.update(now)
        val activeScene = editorState?.scene ?: scene
        val state = editorState
        val simulate = state == null || state.isPlaying || !isEditorMode
        var dt = time.deltaTime
        if (state != null && state.isPaused && !stepRequested) dt = 0f
        val singleStep = stepRequested
        if (singleStep) {
            stepRequested = false
            dt = 1f / 60f
        }
        sceneRenderer.sortByLayer = true
        if ((simulate || singleStep) && dt > 0f) {
            activeScene?.update(dt)
            scene = activeScene
        }
        // camera follow support keeps the view on the primary target
        val playing = state?.isPlaying ?: false
        activeScene?.allObjects?.forEach { go ->
            val cam = go.getComponent<CameraComponent>() ?: return@forEach
            if (cam.followTargetName.isEmpty()) {
                if (cam.isMain && playing) cam.applyTo(camera)
            } else {
                val target = activeScene.findByName(cam.followTargetName)
                if (target != null) cam.follow(target.transform.position, camera, dt)
            }
        }
        inputManager?.endFrame()
        state?.profiler?.endFrame(time.deltaTime)
    }

    private fun drawFrame() {
        val canvas = holder.lockCanvas() ?: return
        renderer.canvas = canvas
        camera.viewportWidth = canvas.width.toFloat()
        camera.viewportHeight = canvas.height.toFloat()
        renderer.clear(backgroundColor)
        renderer.begin(camera)
        val activeScene = editorState?.scene ?: scene
        if (showGrid) drawGrid(canvas)
        sceneRenderer.render(activeScene, camera)
        drawLights(canvas, activeScene)
        if (isEditorMode) {
            if (showColliders) drawColliders(canvas, activeScene)
            drawSelection(canvas)
        }
        renderer.end()
        if (showStats) drawStats(canvas, activeScene)
        holder.unlockCanvasAndPost(canvas)
    }

    // ------------------------------------------------------------------ drawing helpers

    private fun drawGrid(canvas: Canvas) {
        val step = (gridSize * camera.zoom).coerceAtLeast(6f)
        gridPaint.color = android.graphics.Color.argb(38, 120, 124, 160)
        gridPaint.strokeWidth = 1f
        var x = (camera.viewportWidth / 2f - camera.position.x * camera.zoom) % step
        if (x < 0) x += step
        while (x < camera.viewportWidth) { canvas.drawLine(x, 0f, x, camera.viewportHeight, gridPaint); x += step }
        var y = (camera.viewportHeight / 2f - camera.position.y * camera.zoom) % step
        if (y < 0) y += step
        while (y < camera.viewportHeight) { canvas.drawLine(0f, y, camera.viewportWidth, y, gridPaint); y += step }
        val axisX = camera.viewportWidth / 2f - camera.position.x * camera.zoom
        val axisY = camera.viewportHeight / 2f - camera.position.y * camera.zoom
        axisPaint.strokeWidth = 2f
        axisPaint.color = android.graphics.Color.argb(90, 243, 139, 168)
        canvas.drawLine(axisX, 0f, axisX, camera.viewportHeight, axisPaint)
        axisPaint.color = android.graphics.Color.argb(90, 166, 227, 161)
        canvas.drawLine(0f, axisY, camera.viewportWidth, axisY, axisPaint)
    }

    private fun drawLights(canvas: Canvas, activeScene: Scene?) {
        if (activeScene == null) return
        for (go in activeScene.allObjects) {
            if (!go.active) continue
            val light = go.getComponent<Light2D>() ?: continue
            if (!light.enabled) continue
            val center = camera.worldToScreen(go.transform.position)
            val radius = (light.radius * camera.zoom).coerceAtLeast(4f)
            val intensity = light.currentIntensity().coerceIn(0f, 2f)
            val alpha = (140 * intensity).toInt().coerceIn(0, 255)
            val colorInt = android.graphics.Color.argb(
                alpha, (light.color.r * 255).toInt().coerceIn(0, 255),
                (light.color.g * 255).toInt().coerceIn(0, 255),
                (light.color.b * 255).toInt().coerceIn(0, 255)
            )
            lightPaint.shader = RadialGradient(
                center.x, center.y, radius,
                colorInt, android.graphics.Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(center.x, center.y, radius, lightPaint)
            lightPaint.shader = null
        }
    }

    private fun drawColliders(canvas: Canvas, activeScene: Scene?) {
        val world = PhysicsWorldHolder.world ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = android.graphics.Color.argb(200, 249, 226, 175)
        for (collider in world.colliders) {
            val bounds = collider.getBounds()
            val topLeft = camera.worldToScreen(Vector2(bounds.x, bounds.y))
            val bottomRight = camera.worldToScreen(Vector2(bounds.x + bounds.w, bounds.y + bounds.h))
            canvas.drawRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, paint)
        }
    }

    private fun drawSelection(canvas: Canvas) {
        val go = editorState?.selection ?: return
        val bounds = objectBounds(go)
        val topLeft = camera.worldToScreen(Vector2(bounds.x, bounds.y))
        val bottomRight = camera.worldToScreen(Vector2(bounds.x + bounds.w, bounds.y + bounds.h))
        selectionPaint.color = android.graphics.Color.parseColor("#89B4FA")
        canvas.drawRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, selectionPaint)
        val handle = 10f
        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        handlePaint.color = android.graphics.Color.parseColor("#89B4FA")
        canvas.drawRect(topLeft.x - handle / 2, topLeft.y - handle / 2, topLeft.x + handle / 2, topLeft.y + handle / 2, handlePaint)
        canvas.drawRect(bottomRight.x - handle / 2, bottomRight.y - handle / 2, bottomRight.x + handle / 2, bottomRight.y + handle / 2, handlePaint)
    }

    private fun drawStats(canvas: Canvas, activeScene: Scene?) {
        val state = editorState
        val lines = mutableListOf<String>()
        lines.add("FPS ${time.fps.toInt()}   frame ${"%.1f".format(time.deltaTime * 1000f)} ms")
        lines.add("Objects ${activeScene?.allObjects?.size ?: 0}   Components ${activeScene?.componentCount() ?: 0}")
        val world = PhysicsWorldHolder.world
        if (world != null) lines.add("Bodies ${world.bodyCount()}   Colliders ${world.colliderCount()}")
        lines.add("Camera (${camera.position.x.toInt()}, ${camera.position.y.toInt()}) zoom ${"%.2f".format(camera.zoom)}")
        if (state != null) {
            lines.add("Tool ${state.tool.name}   Selection ${state.selection?.name ?: "-"}")
            state.profiler.drawCalls = activeScene?.allObjects?.size ?: 0
            state.profiler.objectsRendered = activeScene?.allObjects?.size ?: 0
        }
        overlayPaint.textSize = 22f
        var y = 28f
        for (line in lines) {
            canvas.drawText(line, 14f, y, overlayPaint)
            y += 26f
        }
    }

    // ------------------------------------------------------------------ hit testing

    /** Bounding box in world space including sprite size, tilemap size and colliders. */
    fun objectBounds(go: GameObject): Rect {
        val sprite = go.getComponent<SpriteRenderer>()
        if (sprite != null) {
            val size = if (sprite.size.x > 1.5f && sprite.size.y > 1.5f) sprite.size else sprite.pixelSize()
            val halfW = size.x * 0.5f * abs(go.transform.scale.x)
            val halfH = size.y * 0.5f * abs(go.transform.scale.y)
            return Rect(go.transform.position.x - halfW, go.transform.position.y - halfH, halfW * 2f, halfH * 2f)
        }
        val tilemapComponent = go.getComponent<TilemapComponent>()
        val map = tilemapComponent?.tilemap
        if (map != null && tilemapComponent != null) {
            val width = map.width * tilemapComponent.tileSize
            val height = map.height * tilemapComponent.tileSize
            return Rect(go.transform.position.x, go.transform.position.y, width, height)
        }
        val collider = go.components.filterIsInstance<com.sura.engine.physics.Collider2D>().firstOrNull()
        if (collider != null) return collider.getBounds()
        val light = go.getComponent<Light2D>()
        if (light != null) {
            return Rect(
                go.transform.position.x - light.radius / 2f, go.transform.position.y - light.radius / 2f,
                light.radius, light.radius
            )
        }
        return Rect(go.transform.position.x - 12f, go.transform.position.y - 12f, 24f, 24f)
    }

    fun pickObject(world: Vector2): GameObject? {
        val activeScene = editorState?.scene ?: scene ?: return null
        val ordered = activeScene.allObjects.sortedByDescending { it.transform.zOrder }
        for (go in ordered) {
            if (!go.active) continue
            if (objectBounds(go).contains(world.x, world.y)) return go
        }
        return null
    }

    fun screenToWorld(x: Float, y: Float): Vector2 = camera.screenToWorld(Vector2(x, y))

    // ------------------------------------------------------------------ input

    override fun onTouchEvent(event: MotionEvent): Boolean {
        inputManager?.onTouchEvent(event)
        activePointers = event.pointerCount
        val state = editorState
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown(event.x, event.y, state)
            MotionEvent.ACTION_POINTER_DOWN -> if (event.pointerCount == 2) startPinch(event)
            MotionEvent.ACTION_MOVE -> handleMove(event, state)
            MotionEvent.ACTION_POINTER_UP -> if (event.pointerCount <= 2) endPinch()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handleUp(state)
        }
        return true
    }

    private fun startPinch(event: MotionEvent) {
        if (event.pointerCount < 2) return
        pinchDistance = hypot(
            (event.getX(0) - event.getX(1)).toDouble(),
            (event.getY(0) - event.getY(1)).toDouble()
        ).toFloat()
        pinchZoomStart = camera.zoom
        panning = false
        dragging = false
    }

    private fun endPinch() {
        pinchDistance = 0f
        panning = false
    }

    private fun handleDown(x: Float, y: Float, state: EditorState?) {
        if (!isEditorMode || state == null) return
        if (activePointers >= 2) { panning = true; lastPanPoint.set(x, y); return }
        val world = screenToWorld(x, y)
        when (state.tool) {
            EditorTool.TILE_PAINT, EditorTool.TILE_ERASE -> {
                paintTile(world, state, state.tool == EditorTool.TILE_ERASE)
                dragging = true
                lastPanPoint.set(world.x, world.y)
                return
            }
            EditorTool.PAN -> {
                panning = true
                lastPanPoint.set(x, y)
                return
            }
            EditorTool.SELECT -> {
                val hit = pickObject(world)
                state.select(hit)
                if (hit != null) {
                    dragTarget = hit
                    dragStart.set(world.x, world.y)
                    objectStart.set(hit.transform.position.x, hit.transform.position.y)
                    dragging = true
                }
                return
            }
            else -> {
                val hit = pickObject(world)
                if (hit != null && hit !== state.selection) state.select(hit)
                val target = hit
                if (target != null) {
                    dragTarget = target
                    dragStart.set(world.x, world.y)
                    objectStart.set(target.transform.position.x, target.transform.position.y)
                    dragging = true
                } else {
                    panning = true
                    lastPanPoint.set(x, y)
                }
                return
            }
        }
    }

    private fun handleMove(event: MotionEvent, state: EditorState?) {
        if (!isEditorMode || state == null) return
        if (event.pointerCount >= 2 && pinchDistance > 0f) {
            val distance = hypot(
                (event.getX(0) - event.getX(1)).toDouble(),
                (event.getY(0) - event.getY(1)).toDouble()
            ).toFloat()
            if (distance > 1f) {
                camera.zoom = (pinchZoomStart * (distance / pinchDistance)).coerceIn(0.15f, 6f)
            }
            return
        }
        if (panning) {
            val dx = (event.x - lastPanPoint.x) / camera.zoom
            val dy = (event.y - lastPanPoint.y) / camera.zoom
            camera.position.x -= dx
            camera.position.y -= dy
            lastPanPoint.set(event.x, event.y)
            return
        }
        val world = screenToWorld(event.x, event.y)
        if ((state.tool == EditorTool.TILE_PAINT || state.tool == EditorTool.TILE_ERASE) && dragging) {
            paintTile(world, state, state.tool == EditorTool.TILE_ERASE)
            return
        }
        val target = dragTarget ?: return
        if (!dragging) return
        var newX = objectStart.x + (world.x - dragStart.x)
        var newY = objectStart.y + (world.y - dragStart.y)
        if (state.snapToGrid) {
            val snap = state.snapSize
            newX = kotlin.math.round(newX / snap) * snap
            newY = kotlin.math.round(newY / snap) * snap
        }
        when (state.tool) {
            EditorTool.ROTATE -> {
                val angle = Math.toDegrees(atan2((world.y - target.transform.position.y).toDouble(), (world.x - target.transform.position.x).toDouble())).toFloat()
                target.transform.rotation = angle
            }
            EditorTool.SCALE -> {
                val scale = ((world.x - target.transform.position.x) / 32f).coerceAtLeast(0.05f)
                target.transform.scale.set(scale, scale)
            }
            else -> target.transform.position.set(newX, newY)
        }
        state.isDirty = true
    }

    private fun handleUp(state: EditorState?) {
        if (!isEditorMode || state == null) { dragging = false; panning = false; dragTarget = null; return }
        val target = dragTarget
        if (target != null && dragging) {
            val finalX = target.transform.position.x
            val finalY = target.transform.position.y
            if (finalX != objectStart.x || finalY != objectStart.y) {
                val startX = objectStart.x
                val startY = objectStart.y
                state.undoStack.push("Move ${target.name}", {
                    target.transform.position.set(startX, startY)
                    state.notifyChanged()
                }, {
                    target.transform.position.set(finalX, finalY)
                    state.notifyChanged()
                })
                state.notifyChanged()
            }
        }
        dragging = false
        panning = false
        dragTarget = null
        pinchDistance = 0f
    }

    private fun paintTile(world: Vector2, state: EditorState, erase: Boolean) {
        val selected = state.selection
        val tilemap = selected?.getComponent<TilemapComponent>()
            ?: (state.scene?.allObjects?.firstOrNull { it.getComponent<TilemapComponent>() != null }?.getComponent())
            ?: return
        tilemap.paintAt(world.x, world.y, if (erase) -1 else state.brushTileId)
        state.isDirty = true
    }

    /** Runs exactly one simulation step (editor "step frame" button). */
    fun stepFrame() {
        stepRequested = true
    }

    /** Utility for the tool palette: focus the camera on the current selection. */
    fun focusSelection() {
        val go = editorState?.selection ?: return
        camera.position.set(go.transform.position.x, go.transform.position.y)
    }

    companion object {
        var physicsWorldRef: PhysicsWorld? = null
    }

    private object PhysicsWorldHolder {
        val world: PhysicsWorld? get() = physicsWorldRef
            ?: if (Engine.isInitialized()) Engine.get().services.get(PhysicsWorld::class.java) else null
    }
}
