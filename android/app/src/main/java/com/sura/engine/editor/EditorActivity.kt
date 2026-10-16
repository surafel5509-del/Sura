package com.sura.engine.editor

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.sura.engine.core.Engine
import com.sura.engine.core.Logger

/**
 * The Sura Engine editor - a full landscape IDE for building 2D games on the device.
 * Everything runs offline: no account, no network, no external tools required.
 */
class EditorActivity : AppCompatActivity() {

    private lateinit var screen: EditorScreen
    private var ticker: Thread? = null
    @Volatile private var ticking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()
        Engine.get()
        screen = EditorScreen(this)
        setContentView(screen)
        startTicker()
        Logger.i("EditorActivity started")
    }

    private fun enableImmersiveFullscreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }

    private fun startTicker() {
        ticking = true
        ticker = Thread {
            while (ticking) {
                try {
                    runOnUiThread { if (!isFinishing) screen.onTick() }
                    Thread.sleep(250)
                } catch (e: InterruptedException) {
                    break
                } catch (t: Throwable) {
                    Logger.e("Editor tick failed: ${t.message}")
                }
            }
        }.also { it.start() }
    }

    /** Physical keyboard support: Ctrl+Z/S/P/B/G like a desktop engine editor. */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (event != null && event.isCtrlPressed) {
            when (keyCode) {
                android.view.KeyEvent.KEYCODE_Z -> {
                    if (event.isShiftPressed) screen.redo() else screen.undo()
                    return true
                }
                android.view.KeyEvent.KEYCODE_Y -> { screen.redo(); return true }
                android.view.KeyEvent.KEYCODE_S -> {
                    if (event.isShiftPressed) screen.saveProjectNow() else screen.saveSceneNow()
                    return true
                }
                android.view.KeyEvent.KEYCODE_P -> { screen.togglePlay(); return true }
                android.view.KeyEvent.KEYCODE_B -> { screen.openBuildDialog(); return true }
                android.view.KeyEvent.KEYCODE_G -> { screen.toggleGridNow(); return true }
                android.view.KeyEvent.KEYCODE_PERIOD -> { screen.stopPlay(); return true }
                android.view.KeyEvent.KEYCODE_H -> { screen.toggleStatsNow(); return true }
            }
        }
        if (keyCode == android.view.KeyEvent.KEYCODE_SPACE) {
            screen.togglePlay()
            return true
        }
        return super.onKeyDown(keyCode, event)
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
        ticking = false
        ticker?.interrupt()
        ticker = null
        try {
            screen.shutdown()
        } catch (t: Throwable) {
            Logger.e("Shutdown failed: ${t.message}")
        }
        Engine.get().stop()
    }
}
