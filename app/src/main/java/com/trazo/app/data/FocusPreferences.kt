package com.trazo.app.data

import android.content.Context
import androidx.core.content.edit

data class FocusPreferences(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val cyclesBeforeLongBreak: Int = 4,
    val autoAdvance: Boolean = false
)

object FocusPreferencesStore {
    private const val FILE = "trazo_focus_preferences"

    fun load(context: Context): FocusPreferences {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return FocusPreferences(
            focusMinutes = prefs.getInt("focus", 25).coerceIn(1, 180),
            shortBreakMinutes = prefs.getInt("short_break", 5).coerceIn(1, 60),
            longBreakMinutes = prefs.getInt("long_break", 15).coerceIn(1, 90),
            cyclesBeforeLongBreak = prefs.getInt("cycles", 4).coerceIn(2, 8),
            autoAdvance = prefs.getBoolean("auto_advance", false)
        )
    }

    fun save(context: Context, value: FocusPreferences) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putInt("focus", value.focusMinutes)
            putInt("short_break", value.shortBreakMinutes)
            putInt("long_break", value.longBreakMinutes)
            putInt("cycles", value.cyclesBeforeLongBreak)
            putBoolean("auto_advance", value.autoAdvance)
        }
    }
}
