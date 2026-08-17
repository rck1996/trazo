package com.trazo.app.widget

import android.content.Context

enum class WidgetSection { TASKS, HABITS }
enum class WidgetPalette { CORAL, BOTANICAL, INK }
enum class WidgetPrivacy { FULL, DISCREET }
enum class WidgetStyle { ARTISTIC, MINIMAL }

data class WidgetConfig(
    val firstSection: WidgetSection = WidgetSection.TASKS,
    val showTasks: Boolean = true,
    val showHabits: Boolean = true,
    val focusMinutes: Int = 25,
    val palette: WidgetPalette = WidgetPalette.CORAL,
    val maxItems: Int = 4,
    val tagFilter: String = "",
    val overdueOnly: Boolean = false,
    val privacy: WidgetPrivacy = WidgetPrivacy.FULL,
    val style: WidgetStyle = WidgetStyle.ARTISTIC
)

/** Preferences are namespaced per widget, so two widgets can behave differently. */
object WidgetPreferences {
    private const val FILE = "trazo_widget_preferences"

    fun load(context: Context, widgetId: Int): WidgetConfig {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return WidgetConfig(
            firstSection = enumValueOrDefault(
                prefs.getString(key(widgetId, "first"), null), WidgetSection.TASKS
            ),
            showTasks = prefs.getBoolean(key(widgetId, "tasks"), true),
            showHabits = prefs.getBoolean(key(widgetId, "habits"), true),
            focusMinutes = prefs.getInt(key(widgetId, "focus"), 25).coerceIn(5, 120),
            palette = enumValueOrDefault(
                prefs.getString(key(widgetId, "palette"), null), WidgetPalette.CORAL
            ),
            maxItems = prefs.getInt(key(widgetId, "items"), 4).coerceIn(2, 8),
            tagFilter = prefs.getString(key(widgetId, "tag"), "").orEmpty(),
            overdueOnly = prefs.getBoolean(key(widgetId, "overdue"), false),
            privacy = enumValueOrDefault(prefs.getString(key(widgetId, "privacy"), null), WidgetPrivacy.FULL),
            style = enumValueOrDefault(prefs.getString(key(widgetId, "style"), null), WidgetStyle.ARTISTIC)
        )
    }

    fun save(context: Context, widgetId: Int, config: WidgetConfig) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(key(widgetId, "first"), config.firstSection.name)
            .putBoolean(key(widgetId, "tasks"), config.showTasks)
            .putBoolean(key(widgetId, "habits"), config.showHabits)
            .putInt(key(widgetId, "focus"), config.focusMinutes)
            .putString(key(widgetId, "palette"), config.palette.name)
            .putInt(key(widgetId, "items"), config.maxItems)
            .putString(key(widgetId, "tag"), config.tagFilter.trim().removePrefix("#").lowercase())
            .putBoolean(key(widgetId, "overdue"), config.overdueOnly)
            .putString(key(widgetId, "privacy"), config.privacy.name)
            .putString(key(widgetId, "style"), config.style.name)
            .apply()
    }

    fun remove(context: Context, widgetId: Int) {
        val editor = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
        listOf("first", "tasks", "habits", "focus", "palette", "items", "tag", "overdue", "privacy", "style")
            .forEach { editor.remove(key(widgetId, it)) }
        editor.apply()
    }

    private fun key(widgetId: Int, name: String) = "widget_${widgetId}_$name"

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T =
        runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)
}
