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
import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import java.time.LocalDate

/** A three-item task list whose checks work directly from the launcher. */
class TaskListWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context, it)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COMPLETE) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID)
            val store = LocalStore(context)
            val state = store.load()
            if (taskId != null && state.tasks.any { it.id == taskId && !it.completed }) {
                store.save(state.copy(tasks = state.tasks.map { task ->
                    if (task.id == taskId) task.copy(completed = true, completedAt = System.currentTimeMillis()) else task
                }))
            }
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val ACTION_COMPLETE = "com.trazo.app.widget.TASK_LIST_COMPLETE"
        private const val EXTRA_TASK_ID = "task_id"

        private val rows = listOf(
            Triple(R.id.task_list_row_1, R.id.task_list_title_1, R.id.task_list_check_1),
            Triple(R.id.task_list_row_2, R.id.task_list_title_2, R.id.task_list_check_2),
            Triple(R.id.task_list_row_3, R.id.task_list_title_3, R.id.task_list_check_3)
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, TaskListWidget::class.java)).forEach {
                manager.updateAppWidget(it, views(context, it))
            }
        }

        private fun views(context: Context, widgetId: Int): RemoteViews {
            val today = LocalDate.now()
            val state = LocalStore(context).load()
            val pending = state.tasks
                .filter { !it.completed && !it.archived && it.deletedAt == null && (it.dueDate == null || !it.dueDate.isAfter(today)) }
                .sortedWith(compareByDescending<Task> { it.priority == TaskPriority.IMPORTANT }
                    .thenBy { it.dueDate ?: LocalDate.MAX })
            val completedToday = state.tasks.count { it.completed && it.dueDate == today }

            return RemoteViews(context.packageName, R.layout.task_list_widget).apply {
                setTextViewText(
                    R.id.task_list_progress,
                    if (pending.isEmpty()) "Día despejado" else "${pending.size} por trazar  ·  $completedToday hechas"
                )
                val open = openTasks(context, widgetId * 100)
                setOnClickPendingIntent(R.id.task_list_header, open)
                setOnClickPendingIntent(R.id.task_list_footer, open)

                rows.forEachIndexed { index, (rowId, titleId, checkId) ->
                    val task = pending.getOrNull(index)
                    setViewVisibility(rowId, if (task == null) View.GONE else View.VISIBLE)
                    if (task != null) {
                        val prefix = if (task.priority == TaskPriority.IMPORTANT) "★  " else "○  "
                        setTextViewText(titleId, prefix + task.title)
                        setContentDescription(checkId, "Completar ${task.title}")
                        val complete = Intent(context, TaskListWidget::class.java)
                            .setAction(ACTION_COMPLETE)
                            .setData(Uri.parse("trazo://widget/tasks/${task.id}"))
                            .putExtra(EXTRA_TASK_ID, task.id)
                        setOnClickPendingIntent(
                            checkId,
                            PendingIntent.getBroadcast(
                                context,
                                task.id.hashCode(),
                                complete,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                        setOnClickPendingIntent(titleId, open)
                    }
                }
                setViewVisibility(R.id.task_list_empty, if (pending.isEmpty()) View.VISIBLE else View.GONE)
            }
        }

        private fun openTasks(context: Context, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setData(Uri.parse("trazo://open/TASKS"))
                .putExtra(MainActivity.EXTRA_SECTION, "TASKS")
            return PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
