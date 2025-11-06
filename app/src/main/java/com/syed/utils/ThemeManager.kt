package com.syed.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_SYSTEM = 2

    private fun getPrefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun applyTheme(themeMode: Int) {
        when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun saveThemeMode(
        context: Context,
        themeMode: Int,
    ) {
        getPrefs(context).edit().putInt(KEY_THEME_MODE, themeMode).apply()
        applyTheme(themeMode)
    }

    fun getThemeMode(context: Context): Int = getPrefs(context).getInt(KEY_THEME_MODE, THEME_SYSTEM)

    fun getThemeModeName(themeMode: Int): String =
        when (themeMode) {
            THEME_LIGHT -> "Light Mode"
            THEME_DARK -> "Dark Mode"
            THEME_SYSTEM -> "System Default"
            else -> "Unknown"
        }
}
