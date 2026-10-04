package com.solargridx.app

import android.app.Application
import com.solargridx.app.utils.ThemeManager

class SolarGridXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyTheme(this)
    }
}
