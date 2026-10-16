package com.sura.engine.runtime

import android.os.Bundle
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.sura.engine.R
import com.sura.engine.assets.AssetManager
import com.sura.engine.audio.AudioManager
import com.sura.engine.core.Engine
import com.sura.engine.core.JobSystem
import com.sura.engine.core.Scheduler
import com.sura.engine.editor.EngineView
import com.sura.engine.games.DemoScenes
import com.sura.engine.input.InputManager
import com.sura.engine.physics.PhysicsWorld
import com.sura.engine.platform.Platform
import com.sura.engine.project.ProjectManager
import com.sura.engine.project.ProjectSettings
import com.sura.engine.rendering.gl.GLGameView
import com.sura.engine.scene.Scene
import com.sura.engine.scene.SceneManager

/**
 * Standalone player used to run exported games. It boots the scene configured in the project
 * (or a demo scene when launched straight from the APK) with no editor overhead, using either
 * the OpenGL ES renderer or the Canvas renderer as configured by the project.
 */
class GameActivity : AppCompatActivity() {

    private val sceneManager = SceneManager()
    private val physics = PhysicsWorld()
    private var canvasView: EngineView? = null
    private var glView: GLGameView? = null
    private var settings = ProjectSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        val container = findViewById<FrameLayout>(R.id.gameContainer)

        val engine = Engine.get()
        val input = InputManager()
        val assets = AssetManager(this)
        engine.services.register(SceneManager::class.java, sceneManager)
        engine.services.register(InputManager::class.java, input)
        engine.services.register(PhysicsWorld::class.java, physics)
        engine.services.register(AudioManager::class.java, AudioManager(this))
        engine.services.register(AssetManager::class.java, assets)
        engine.services.register(JobSystem::class.java, JobSystem())
        engine.services.register(Scheduler::class.java, Scheduler())
        engine.start()

        val scene = resolveStartScene()
        val useOpenGl = intent.getBooleanExtra(EXTRA_OPENGL, settings.rendererBackend.equals("opengl", ignoreCase = true))
        if (useOpenGl) attachGlRenderer(container, scene, input, assets) else attachCanvasRenderer(container, scene, input)

        sceneManager.loadScene(scene)
        com.sura.engine.animation.TextureResolver.applyTo(scene, assets)
        EngineView.physicsWorldRef = physics
        com.sura.engine.core.Logger.i("GameActivity started with ${scene.name} (${scene.allObjects.size} objects), renderer=${if (useOpenGl) "opengl" else "canvas"}")
    }

    private fun attachCanvasRenderer(container: FrameLayout, scene: Scene, input: InputManager) {
        val view = EngineView(this)
        view.isEditorMode = false
        view.showGrid = false
        view.showStats = settings.showStats
        view.showColliders = false
        view.inputManager = input
        view.sceneManager = sceneManager
        view.scene = scene
        canvasView = view
        container.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun attachGlRenderer(container: FrameLayout, scene: Scene, input: InputManager, assets: AssetManager) {
        val view = GLGameView(this)
        view.inputManager = input
        view.scene = scene
        view.showStats = settings.showStats
        view.targetFps = settings.targetFps
        glView = view
        container.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        view.queueEvent { com.sura.engine.animation.TextureResolver.applyTo(scene, assets) }
    }

    private fun resolveStartScene(): Scene {
        val requested = intent.getStringExtra(EXTRA_SCENE)
        val projectName = intent.getStringExtra(EXTRA_PROJECT) ?: ProjectManager(Platform(this)).lastProjectName()
        if (projectName.isNotEmpty()) {
            val manager = ProjectManager(Platform(this))
            val project = manager.openProject(projectName)
            if (project != null) {
                settings = project.settings
                val sceneName = requested ?: project.settings.startScene
                val scene = manager.loadScene(project, sceneName)
                if (scene != null) return scene
                val available = manager.listScenes(project)
                if (available.isNotEmpty()) {
                    manager.loadScene(project, available.first())?.let { loaded -> return loaded }
                }
            }
        }
        val demo = requested ?: "platformer"
        return DemoScenes.build(demo)
    }

    override fun onResume() {
        super.onResume()
        Engine.get().resume()
        glView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        glView?.onPause()
        Engine.get().pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Engine.get().stop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_SCENE = "extra_scene"
        const val EXTRA_PROJECT = "extra_project"
        const val EXTRA_OPENGL = "extra_opengl"
    }
}
