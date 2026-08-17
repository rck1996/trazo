package com.trazo.app.data

import android.content.Context

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val largeText: Boolean = false,
    val reducedMotion: Boolean = false,
    val haptics: Boolean = true,
    val minimalMode: Boolean = false
)

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("trazo_app_settings", Context.MODE_PRIVATE)

    fun load() = AppSettings(
        theme = runCatching {
            ThemePreference.valueOf(preferences.getString("theme", null).orEmpty())
        }.getOrDefault(ThemePreference.SYSTEM),
        largeText = preferences.getBoolean("large_text", false),
        reducedMotion = preferences.getBoolean("reduced_motion", false),
        haptics = preferences.getBoolean("haptics", true),
        minimalMode = preferences.getBoolean("minimal_mode", false)
    )

    fun save(settings: AppSettings) {
        preferences.edit()
            .putString("theme", settings.theme.name)
            .putBoolean("large_text", settings.largeText)
            .putBoolean("reduced_motion", settings.reducedMotion)
            .putBoolean("haptics", settings.haptics)
            .putBoolean("minimal_mode", settings.minimalMode)
            .apply()
    }
}
