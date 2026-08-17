package com.trazo.app.notifications

import android.content.Context
import androidx.core.content.edit

data class FocusSession(
    val endAt: Long,
    val taskTitle: String?,
    val phase: String,
    val totalSeconds: Int
)

/** Tiny shared snapshot so notifications and home-screen widgets show the same timer. */
object FocusSessionStore {
    private const val NAME = "trazo_focus_session"
    private const val END_AT = "end_at"
    private const val TASK = "task"
    private const val PHASE = "phase"
    private const val TOTAL_SECONDS = "total_seconds"

    fun save(context: Context, endAt: Long, taskTitle: String?, phase: String, totalSeconds: Int) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit {
            putLong(END_AT, endAt)
            putString(TASK, taskTitle)
            putString(PHASE, phase)
            putInt(TOTAL_SECONDS, totalSeconds)
        }
    }

    fun load(context: Context): FocusSession? {
        val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val endAt = preferences.getLong(END_AT, 0L)
        if (endAt <= System.currentTimeMillis()) return null
        return FocusSession(
            endAt,
            preferences.getString(TASK, null),
            preferences.getString(PHASE, "FOCUS") ?: "FOCUS",
            preferences.getInt(TOTAL_SECONDS, ((endAt - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(1))
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit { clear() }
    }
}
