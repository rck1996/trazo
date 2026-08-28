package com.trazo.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.trazo.app.MainActivity
import com.trazo.app.R
import com.trazo.app.data.LocalStore
import com.trazo.app.notifications.FocusSessionStore
import com.trazo.app.notifications.FocusTimerService

/** A live countdown when focusing and a useful launch surface while idle. */
class FocusWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context, it)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE_FOCUS) {
            val session = FocusSessionStore.load(context)
            if (session == null) {
                val task = LocalStore(context).load().tasks.firstOrNull { !it.completed && !it.archived && it.deletedAt == null }
                val durationSeconds = (task?.durationMinutes ?: 25).coerceIn(1, 180) * 60
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, FocusTimerService::class.java).apply {
                        putExtra(FocusTimerService.EXTRA_END_AT, System.currentTimeMillis() + durationSeconds * 1000L)
                        putExtra(FocusTimerService.EXTRA_TASK, task?.title)
                        putExtra(FocusTimerService.EXTRA_PHASE, "FOCUS")
                    }
                )
            } else {
                context.startService(
                    Intent(context, FocusTimerService::class.java)
                        .setAction(FocusTimerService.ACTION_STOP)
                        .putExtra(FocusTimerService.EXTRA_RESET_UI, true)
                )
            }
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val ACTION_TOGGLE_FOCUS = "com.trazo.app.widget.TOGGLE_FOCUS"
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, FocusWidget::class.java)
            manager.getAppWidgetIds(component).forEach { manager.updateAppWidget(it, views(context, it)) }
            TrazoWidget.updateSelf(context)
        }

        private fun views(context: Context, widgetId: Int): RemoteViews {
            val session = FocusSessionStore.load(context)
            val nextTask = LocalStore(context).load().tasks.firstOrNull { !it.completed && !it.archived && it.deletedAt == null }
            return RemoteViews(context.packageName, R.layout.focus_widget).apply {
                val open = openFocus(context, widgetId)
                setOnClickPendingIntent(R.id.focus_widget_phase, open)
                setOnClickPendingIntent(R.id.focus_widget_task, open)
                setOnClickPendingIntent(R.id.focus_widget_timer, open)
                setOnClickPendingIntent(R.id.focus_widget_idle_time, open)
                setOnClickPendingIntent(R.id.focus_widget_action, toggleFocus(context, widgetId))
                setTextViewText(R.id.focus_widget_phase, if (session?.phase == "BREAK") "DESCANSO" else "MODO ENFOQUE")
                setTextViewText(
                    R.id.focus_widget_task,
                    session?.taskTitle ?: nextTask?.title ?: "Un trazo a la vez"
                )
                if (session == null) {
                    setViewVisibility(R.id.focus_widget_timer, View.GONE)
                    setViewVisibility(R.id.focus_widget_idle_time, View.VISIBLE)
                    val minutes = nextTask?.durationMinutes?.coerceIn(1, 180) ?: 25
                    setTextViewText(R.id.focus_widget_action, "▶  Iniciar $minutes min")
                } else {
                    val base = SystemClock.elapsedRealtime() + (session.endAt - System.currentTimeMillis())
                    setViewVisibility(R.id.focus_widget_timer, View.VISIBLE)
                    setViewVisibility(R.id.focus_widget_idle_time, View.GONE)
                    setChronometer(R.id.focus_widget_timer, base, null, true)
                    setChronometerCountDown(R.id.focus_widget_timer, true)
                    setTextViewText(R.id.focus_widget_action, "■  Detener")
                }
            }
        }

        private fun toggleFocus(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, FocusWidget::class.java)
                .setAction(ACTION_TOGGLE_FOCUS)
                .setData(Uri.parse("trazo://widget/focus/$widgetId"))
            return PendingIntent.getBroadcast(
                context,
                widgetId * 20 + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun openFocus(context: Context, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setData(Uri.parse("trazo://open/FOCUS"))
                .putExtra(MainActivity.EXTRA_SECTION, "FOCUS")
            return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
