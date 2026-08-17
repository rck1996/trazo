package com.trazo.app.data

import android.content.Context

data class FocusStats(
    val sessions: Int,
    val minutes: Int,
    val todayMinutes: Int = 0,
    val weekMinutes: Int = 0
)

object FocusStatsStore {
    private const val FILE = "trazo_focus_stats"

    fun record(context: Context, seconds: Int) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val minutes = (seconds / 60).coerceAtLeast(1)
        val dateKey = "minutes_${java.time.LocalDate.now()}"
        prefs.edit()
            .putInt("sessions", prefs.getInt("sessions", 0) + 1)
            .putInt("minutes", prefs.getInt("minutes", 0) + minutes)
            .putInt(dateKey, prefs.getInt(dateKey, 0) + minutes)
            .apply()
    }

    fun load(context: Context): FocusStats {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now()
        val todayMinutes = prefs.getInt("minutes_$today", 0)
        val weekMinutes = (0L..6L).sumOf { prefs.getInt("minutes_${today.minusDays(it)}", 0) }
        return FocusStats(prefs.getInt("sessions", 0), prefs.getInt("minutes", 0), todayMinutes, weekMinutes)
    }
}
