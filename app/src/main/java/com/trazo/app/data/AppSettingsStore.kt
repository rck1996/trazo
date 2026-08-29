package com.trazo.app.data

import android.content.Context

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val largeText: Boolean = false,
    val reducedMotion: Boolean = false,
    val haptics: Boolean = true,
    val minimalMode: Boolean = false,
    val nightReviewEnabled: Boolean = true,
    val nightReviewHour: Int = 20
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
        minimalMode = preferences.getBoolean("minimal_mode", false),
        nightReviewEnabled = preferences.getBoolean("night_review_enabled", true),
        nightReviewHour = preferences.getInt("night_review_hour", 20).coerceIn(17, 23)
    )

    fun save(settings: AppSettings) {
        preferences.edit()
            .putString("theme", settings.theme.name)
            .putBoolean("large_text", settings.largeText)
            .putBoolean("reduced_motion", settings.reducedMotion)
            .putBoolean("haptics", settings.haptics)
            .putBoolean("minimal_mode", settings.minimalMode)
            .putBoolean("night_review_enabled", settings.nightReviewEnabled)
            .putInt("night_review_hour", settings.nightReviewHour.coerceIn(17, 23))
            .apply()
    }
}
