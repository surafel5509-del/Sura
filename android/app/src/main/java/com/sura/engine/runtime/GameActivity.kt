package com.sura.engine.runtime

import android.os.Bundle
import android.view.KeyEvent
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
import com.sura.engine.scene.SceneManager

/**
 * Standalone player used to run exported games. It boots the scene configured in the project
 * (or a demo scene when launched straight from the APK) with no editor overhead.
 */
class GameActivity : AppCompatActivity() {

    private lateinit var engineView: EngineView
    private val sceneManager = SceneManager()
    private val physics = PhysicsWorld()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        engineView = findViewById(R.id.gameView)

        val engine = Engine.get()
        val input = InputManager()
        engine.services.register(SceneManager::class.java, sceneManager)
        engine.services.register(InputManager::class.java, input)
        engine.services.register(PhysicsWorld::class.java, physics)
        engine.services.register(AudioManager::class.java, AudioManager(this))
        engine.services.register(AssetManager::class.java, AssetManager(this))
        engine.services.register(JobSystem::class.java, JobSystem())
        engine.services.register(Scheduler::class.java, Scheduler())
        engine.start()

        engineView.isEditorMode = false
        engineView.showGrid = false
        engineView.showStats = true
        engineView.showColliders = false
        engineView.inputManager = input
        engineView.sceneManager = sceneManager
        EngineView.physicsWorldRef = physics

        val scene = resolveStartScene()
        engineView.scene = scene
        sceneManager.loadScene(scene)
    }

    private fun resolveStartScene(): com.sura.engine.scene.Scene {
        val requested = intent.getStringExtra(EXTRA_SCENE)
        val projectName = intent.getStringExtra(EXTRA_PROJECT) ?: ProjectManager(Platform(this)).lastProjectName()
        if (projectName.isNotEmpty()) {
            val manager = ProjectManager(Platform(this))
            val project = manager.openProject(projectName)
            if (project != null) {
                val sceneName = requested ?: project.settings.startScene
                val scene = manager.loadScene(project, sceneName)
                if (scene != null) return scene
                val available = manager.listScenes(project)
                if (available.isNotEmpty()) {
                    manager.loadScene(project, available.first())?.let { return it }
                }
            }
        }
        val demo = requested ?: "platformer"
        return DemoScenes.build(demo)
    }

    override fun onResume() {
        super.onResume()
        Engine.get().resume()
    }

    override fun onPause() {
        super.onPause()
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
    }
}
