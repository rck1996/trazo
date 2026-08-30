package com.trazo.app.data

import android.content.Context

enum class ThemePreference { SYSTEM, LIGHT, DARK }
enum class TodayLayout { FOCUS, BALANCED, OVERVIEW }

data class AppSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val largeText: Boolean = false,
    val reducedMotion: Boolean = false,
    val haptics: Boolean = true,
    val minimalMode: Boolean = false,
    val nightReviewEnabled: Boolean = true,
    val nightReviewHour: Int = 20,
    val todayLayout: TodayLayout = TodayLayout.BALANCED,
    val onboardingCompleted: Boolean = false,
    val seenRestructureTour: Boolean = false,
    val taskAdvancedExpanded: Boolean = false,
    val habitAdvancedExpanded: Boolean = false
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
        nightReviewHour = preferences.getInt("night_review_hour", 20).coerceIn(17, 23),
        todayLayout = runCatching {
            TodayLayout.valueOf(preferences.getString("today_layout", null).orEmpty())
        }.getOrDefault(TodayLayout.BALANCED),
        onboardingCompleted = preferences.getBoolean("onboarding_completed", false),
        seenRestructureTour = preferences.getBoolean("seen_restructure_tour", false),
        taskAdvancedExpanded = preferences.getBoolean("task_advanced_expanded", false),
        habitAdvancedExpanded = preferences.getBoolean("habit_advanced_expanded", false)
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
            .putString("today_layout", settings.todayLayout.name)
            .putBoolean("onboarding_completed", settings.onboardingCompleted)
            .putBoolean("seen_restructure_tour", settings.seenRestructureTour)
            .putBoolean("task_advanced_expanded", settings.taskAdvancedExpanded)
            .putBoolean("habit_advanced_expanded", settings.habitAdvancedExpanded)
            .apply()
    }
}
