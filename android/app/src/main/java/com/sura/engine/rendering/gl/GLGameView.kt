package com.sura.engine.rendering.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.sura.engine.core.Engine
import com.sura.engine.core.Time
import com.sura.engine.input.InputManager
import com.sura.engine.rendering.Camera
import com.sura.engine.rendering.CameraComponent
import com.sura.engine.rendering.SceneRenderer
import com.sura.engine.scene.Scene
import com.sura.engine.ui.UIElement
import com.sura.engine.utils.Color
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Hardware accelerated game surface. It owns the GL render thread, runs the simulation and
 * draws the scene with the batched OpenGL ES 2.0 renderer.
 */
class GLGameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    val glRenderer = GLRenderer()
    val camera = Camera()
    val textureLoader = GLTextureLoader(glRenderer)
    var scene: Scene? = null
    var inputManager: InputManager? = null
    var showStats = false
    var backgroundColor = Color(0.06f, 0.07f, 0.12f, 1f)
    var targetFps = 60

    private val sceneRenderer = SceneRenderer(glRenderer)
    private val time = Time()
    private val uiRoot = com.sura.engine.ui.Canvas()
    private var initialized = false

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        renderMode = RENDERMODE_CONTINUOUSLY
        setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                initialized = glRenderer.initialize()
                GLES20.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                camera.viewportWidth = width.toFloat()
                camera.viewportHeight = height.toFloat()
                uiRoot.width = width.toFloat()
                uiRoot.height = height.toFloat()
                inputManager?.screenWidth = width
                inputManager?.screenHeight = height
            }

            override fun onDrawFrame(gl: GL10?) {
                tick()
                render()
            }
        })
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        time.update(now)
        Engine.get().update()
        val dt = time.deltaTime
        val activeScene = scene ?: return
        activeScene.update(dt)
        uiRoot.update(dt)
        activeScene.allObjects.forEach { go ->
            val cam = go.getComponent<CameraComponent>() ?: return@forEach
            if (cam.followTargetName.isNotEmpty()) {
                val target = activeScene.findByName(cam.followTargetName)
                if (target != null) cam.follow(target.transform.position, camera, dt)
            } else if (cam.isMain) {
                cam.applyTo(camera)
            }
        }
        inputManager?.endFrame()
    }

    private fun render() {
        GLES20.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (!initialized) return
        glRenderer.begin(camera)
        sceneRenderer.render(scene, camera)
        glRenderer.end()
    }

    /** Loads a texture path into the GL cache (called by the runtime when a scene loads). */
    fun preloadTextures(assetManager: com.sura.engine.assets.AssetManager) {
        val activeScene = scene ?: return
        for (go in activeScene.allObjects) {
            val sprite = go.getComponent<com.sura.engine.rendering.SpriteRenderer>() ?: continue
            if (sprite.texturePath.isEmpty()) continue
            val texture = textureLoader.load(assetManager, sprite.texturePath)
            sprite.loadedTexture = texture
            sprite.sprite = com.sura.engine.rendering.Sprite().apply { this.texture = texture }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        inputManager?.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> uiRoot.handleTouch(event.x, event.y, UIElement.TouchType.DOWN)
            MotionEvent.ACTION_MOVE -> uiRoot.handleTouch(event.x, event.y, UIElement.TouchType.MOVE)
            MotionEvent.ACTION_UP -> uiRoot.handleTouch(event.x, event.y, UIElement.TouchType.UP)
        }
        return true
    }

    fun fps(): Float = time.fps
    fun frameTime(): Float = time.deltaTime
}
