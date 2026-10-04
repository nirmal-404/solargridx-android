package com.solargridx.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "solargridx_theme_prefs"
    const val KEY_THEME_MODE = "theme_mode"

    const val MODE_LIGHT = "light"
    const val MODE_DARK = "dark"
    const val MODE_SYSTEM = "system"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): String {
        return getPrefs(context).getString(KEY_THEME_MODE, MODE_SYSTEM) ?: MODE_SYSTEM
    }

    fun applyTheme(context: Context) {
        val mode = getThemeMode(context)
        applyThemeMode(mode)
    }

    fun setThemeMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
        applyThemeMode(mode)
    }

    private fun applyThemeMode(mode: String) {
        val nightMode = when (mode) {
            MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
