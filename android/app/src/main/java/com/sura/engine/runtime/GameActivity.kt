package com.sura.engine.runtime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sura.engine.R
import com.sura.engine.core.Engine
import com.sura.engine.input.InputManager
import com.sura.engine.scene.SceneManager

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        val engine = Engine.get()
        val sm = SceneManager()
        val im = InputManager()
        engine.services.register(SceneManager::class.java, sm)
        engine.services.register(InputManager::class.java, im)
        engine.start()
        sm.loadScene(sm.createScene("GameScene"))
    }
    override fun onResume() { super.onResume(); Engine.get().resume() }
    override fun onPause() { super.onPause(); Engine.get().pause() }
    override fun onDestroy() { super.onDestroy(); Engine.get().stop() }
}
