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

/** A small ritual board where every visible habit can be toggled in place. */
class HabitListWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context, it)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) {
            val habitId = intent.getStringExtra(EXTRA_HABIT_ID)
            val today = LocalDate.now()
            val store = LocalStore(context)
            val state = store.load()
            if (habitId != null && state.habits.any { it.id == habitId }) {
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
        private const val ACTION_TOGGLE = "com.trazo.app.widget.HABIT_LIST_TOGGLE"
        private const val EXTRA_HABIT_ID = "habit_id"

        private data class Row(val root: Int, val emoji: Int, val title: Int, val check: Int)
        private val rows = listOf(
            Row(R.id.habit_list_row_1, R.id.habit_list_emoji_1, R.id.habit_list_title_1, R.id.habit_list_check_1),
            Row(R.id.habit_list_row_2, R.id.habit_list_emoji_2, R.id.habit_list_title_2, R.id.habit_list_check_2),
            Row(R.id.habit_list_row_3, R.id.habit_list_emoji_3, R.id.habit_list_title_3, R.id.habit_list_check_3)
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, HabitListWidget::class.java)).forEach {
                manager.updateAppWidget(it, views(context, it))
            }
        }

        private fun views(context: Context, widgetId: Int): RemoteViews {
            val today = LocalDate.now()
            val habits = LocalStore(context).load().habits.filter {
                !it.archived && it.deletedAt == null && HabitProgress.isScheduled(it, today)
            }
            val done = habits.count { HabitProgress.isComplete(it, today) }
            val ordered = habits.sortedBy { HabitProgress.isComplete(it, today) }

            return RemoteViews(context.packageName, R.layout.habit_list_widget).apply {
                setTextViewText(R.id.habit_list_progress, "$done / ${habits.size} floreciendo")
                val open = openHabits(context, widgetId * 100)
                setOnClickPendingIntent(R.id.habit_list_header, open)
                setOnClickPendingIntent(R.id.habit_list_footer, open)

                rows.forEachIndexed { index, row ->
                    val habit = ordered.getOrNull(index)
                    setViewVisibility(row.root, if (habit == null) View.GONE else View.VISIBLE)
                    if (habit != null) {
                        val isDone = HabitProgress.isComplete(habit, today)
                        setTextViewText(row.emoji, habit.emoji)
                        setTextViewText(row.title, habit.title)
                        setTextViewText(row.check, if (isDone) "↶" else "✓")
                        setTextColor(row.title, if (isDone) 0xFF77716A.toInt() else 0xFF272522.toInt())
                        setContentDescription(
                            row.check,
                            if (isDone) "Desmarcar ${habit.title}" else "Completar ${habit.title}"
                        )
                        val toggle = Intent(context, HabitListWidget::class.java)
                            .setAction(ACTION_TOGGLE)
                            .setData(Uri.parse("trazo://widget/rituals/${habit.id}"))
                            .putExtra(EXTRA_HABIT_ID, habit.id)
                        setOnClickPendingIntent(
                            row.check,
                            PendingIntent.getBroadcast(
                                context,
                                habit.id.hashCode(),
                                toggle,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                        setOnClickPendingIntent(row.title, open)
                        setOnClickPendingIntent(row.emoji, open)
                    }
                }
                setViewVisibility(R.id.habit_list_empty, if (habits.isEmpty()) View.VISIBLE else View.GONE)
            }
        }

        private fun openHabits(context: Context, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setData(Uri.parse("trazo://open/HABITS"))
                .putExtra(MainActivity.EXTRA_SECTION, "HABITS")
            return PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
