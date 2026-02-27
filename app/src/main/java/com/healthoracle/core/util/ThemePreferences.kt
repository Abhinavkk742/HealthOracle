package com.healthoracle.core.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

object ThemePreferences {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "theme_mode"

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedMode = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        _themeMode.value = ThemeMode.valueOf(savedMode)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }
}