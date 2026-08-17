package com.trazo.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.trazo.app.MainActivity
import com.trazo.app.R
import com.trazo.app.data.LocalStore
import com.trazo.app.model.HabitProgress
import java.time.LocalDate

/** Compact ritual widget with a genuine one-tap completion action. */
class HabitWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context, it)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) {
            val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
            val today = LocalDate.now()
            val store = LocalStore(context)
            val state = store.load()
            if (habitId != null) {
                store.save(state.copy(habits = state.habits.map { habit ->
                    if (habit.id != habitId) habit else HabitProgress.withAmount(
                        habit, today,
                        if (HabitProgress.isComplete(habit, today)) 0 else habit.target
                    )
                }))
            }
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val ACTION_TOGGLE = "com.trazo.app.widget.TOGGLE_HABIT"
        private const val EXTRA_HABIT_ID = "habit_id"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, HabitWidget::class.java)
            manager.getAppWidgetIds(component).forEach { manager.updateAppWidget(it, views(context, it)) }
        }

        private fun views(context: Context, widgetId: Int): RemoteViews {
            val today = LocalDate.now()
            val habits = LocalStore(context).load().habits.filter {
                !it.archived && it.deletedAt == null && HabitProgress.isScheduled(it, today)
            }
            val done = habits.count { HabitProgress.isComplete(it, today) }
            val next = habits.firstOrNull { !HabitProgress.isComplete(it, today) }
            return RemoteViews(context.packageName, R.layout.habit_widget).apply {
                setTextViewText(R.id.habit_widget_progress, "$done / ${habits.size} hoy")
                val open = openHabits(context, widgetId * 10)
                setOnClickPendingIntent(R.id.habit_widget_header, open)
                setOnClickPendingIntent(R.id.habit_widget_emoji, open)
                setOnClickPendingIntent(R.id.habit_widget_title, open)
                setOnClickPendingIntent(R.id.habit_widget_hint, open)
                if (next == null) {
                    setTextViewText(R.id.habit_widget_emoji, if (habits.isEmpty()) "✦" else "✓")
                    setTextViewText(R.id.habit_widget_title, if (habits.isEmpty()) "Sin rituales para hoy" else "Rituales completos")
                    setTextViewText(R.id.habit_widget_hint, if (habits.isEmpty()) "Crea uno en Trazo" else "Qué buen ritmo")
                    setViewVisibility(R.id.habit_widget_check, View.GONE)
                } else {
                    setTextViewText(R.id.habit_widget_emoji, next.emoji)
                    setTextViewText(R.id.habit_widget_title, next.title)
                    val streak = HabitProgress.streak(next, today)
                    setTextViewText(R.id.habit_widget_hint, if (streak == 0) "Empieza tu racha" else "$streak días de racha")
                    setViewVisibility(R.id.habit_widget_check, View.VISIBLE)
                    val toggle = Intent(context, HabitWidget::class.java)
                        .setAction(ACTION_TOGGLE)
                        .setData(Uri.parse("trazo://widget/habit/${next.id}"))
                        .putExtra(EXTRA_HABIT_ID, next.id)
                    setOnClickPendingIntent(
                        R.id.habit_widget_check,
                        PendingIntent.getBroadcast(context, next.id.hashCode(), toggle, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    )
                }
            }
        }

        private fun openHabits(context: Context, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setData(Uri.parse("trazo://open/HABITS"))
                .putExtra(MainActivity.EXTRA_SECTION, "HABITS")
            return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
