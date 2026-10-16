package com.sura.engine.editor

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sura.engine.R
import com.sura.engine.core.Engine
import com.sura.engine.core.ServiceLocator
import com.sura.engine.input.InputManager
import com.sura.engine.platform.Platform
import com.sura.engine.scene.Scene
import com.sura.engine.scene.SceneManager

class EditorActivity : AppCompatActivity() {
    private lateinit var engineView: EngineView
    private lateinit var hierarchy: HierarchyPanel
    private lateinit var inspector: InspectorPanel
    private lateinit var console: ConsolePanel
    private lateinit var sceneManager: SceneManager
    private lateinit var inputManager: InputManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val engine = Engine.get()
        sceneManager = SceneManager()
        inputManager = InputManager()
        engine.services.register(SceneManager::class.java, sceneManager)
        engine.services.register(InputManager::class.java, inputManager)
        engine.services.register(com.sura.engine.physics.PhysicsWorld::class.java, com.sura.engine.physics.PhysicsWorld())
        engine.services.register(com.sura.engine.audio.AudioManager::class.java, com.sura.engine.audio.AudioManager(this))
        val assetManager = com.sura.engine.assets.AssetManager(this)
        engine.services.register(com.sura.engine.assets.AssetManager::class.java, assetManager)
        engine.start()

        engineView = findViewById(R.id.engineView)
        hierarchy = findViewById(R.id.hierarchyPanel)
        inspector = findViewById(R.id.inspectorPanel)
        console = findViewById(R.id.consolePanel)

        engineView.inputManager = inputManager
        engineView.scene = sceneManager.createScene("MainScene")
        sceneManager.loadScene("MainScene")

        // seed scene with sample objects
        val scene = sceneManager.activeScene!!
        val player = scene.createGameObject("Player")
        player.transform.position.set(0f, 0f)
        val cam = scene.createGameObject("Main Camera")
        cam.tag = "MainCamera"
        val light = scene.createGameObject("Directional Light")
        light.tag = "Light"

        hierarchy.refresh(scene)
        hierarchy.onObjectSelected = { go -> inspector.inspect(go) }

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            engineView.isEditorMode = false
            console.log("INFO: Entering play mode")
            Toast.makeText(this, "Play mode", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPause).setOnClickListener {
            engine.pause()
            console.log("INFO: Paused")
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            engine.resume()
            engineView.isEditorMode = true
            console.log("INFO: Stopped play mode")
        }
        findViewById<Button>(R.id.btnBuild).setOnClickListener {
            com.sura.engine.build.BuildManager().buildDebug { ok, msg ->
                runOnUiThread { console.log(if (ok) "INFO: $msg" else "ERROR: $msg") }
            }
        }
        findViewById<Button>(R.id.tabScene).setOnClickListener {
            engineView.isEditorMode = true
            console.log("INFO: Scene view")
        }
        findViewById<Button>(R.id.tabGame).setOnClickListener {
            engineView.isEditorMode = false
            console.log("INFO: Game view")
        }

        com.sura.engine.utils.Logger.i("EditorActivity created")
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
}
