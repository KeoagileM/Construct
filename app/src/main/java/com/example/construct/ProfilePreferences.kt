package com.example.construct


import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object ProfilePreferences {

    private const val PREFS_NAME = "construct360_preferences"
    private const val KEY_THEME = "profile_theme"
    private const val KEY_TEXT_SIZE = "profile_text_size"

    fun saveTheme(context: Context, theme: String) {
        preferences(context).edit()
            .putString(KEY_THEME, theme)
            .apply()

        applyTheme(theme)
    }

    fun getTheme(context: Context): String {
        return preferences(context)
            .getString(KEY_THEME, "System Default") ?: "System Default"
    }

    fun applySavedTheme(context: Context) {
        applyTheme(getTheme(context))
    }

    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            "Light" -> AppCompatDelegate.MODE_NIGHT_NO
            "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun saveTextSize(context: Context, size: String) {
        preferences(context).edit()
            .putString(KEY_TEXT_SIZE, size)
            .apply()
    }

    fun getTextSize(context: Context): String {
        return preferences(context)
            .getString(KEY_TEXT_SIZE, "Medium") ?: "Medium"
    }

    fun getTextScale(context: Context): Float {
        return when (getTextSize(context)) {
            "Small" -> 0.9f
            "Large" -> 1.15f
            else -> 1.0f
        }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}