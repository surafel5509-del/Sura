package com.sura.engine

import android.app.Application
import com.sura.engine.core.Engine
import com.sura.engine.utils.Logger

class SuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Engine.init(this)
        Logger.i("SuraApplication created")
    }
}
