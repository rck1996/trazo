package com.trazo.app.data

import android.content.Context

data class FocusStats(val sessions: Int, val minutes: Int)

object FocusStatsStore {
    private const val FILE = "trazo_focus_stats"

    fun record(context: Context, seconds: Int) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("sessions", prefs.getInt("sessions", 0) + 1)
            .putInt("minutes", prefs.getInt("minutes", 0) + (seconds / 60).coerceAtLeast(1))
            .apply()
    }

    fun load(context: Context): FocusStats {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return FocusStats(prefs.getInt("sessions", 0), prefs.getInt("minutes", 0))
    }
}
