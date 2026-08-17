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
import androidx.core.content.ContextCompat
import com.trazo.app.MainActivity
import com.trazo.app.R
import com.trazo.app.data.LocalStore
import com.trazo.app.model.HabitProgress
import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import com.trazo.app.notifications.FocusSessionStore
import com.trazo.app.notifications.FocusTimerService
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Interactive home-screen overview. All content and navigation remain local. */
class TrazoWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, createViews(context, it)) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) = manager.updateAppWidget(appWidgetId, createViews(context, appWidgetId))

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPreferences.remove(context, it) }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_COLLECTION -> handleCollectionAction(context, intent)
            ACTION_TOGGLE_FOCUS -> toggleFocusSession(
                context,
                intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            )
        }
        super.onReceive(context, intent)
    }

    private fun handleCollectionAction(context: Context, intent: Intent) {
        val store = LocalStore(context)
        val state = store.load()
        when (intent.getStringExtra(EXTRA_COLLECTION_COMMAND)) {
            COMMAND_COMPLETE_TASK -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                if (state.tasks.any { it.id == taskId && !it.completed }) {
                    store.save(state.copy(tasks = state.tasks.map {
                        if (it.id == taskId) it.copy(completed = true) else it
                    }))
                }
            }
            COMMAND_TOGGLE_HABIT -> {
                val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
                val today = LocalDate.now()
                if (state.habits.any { it.id == habitId }) {
                    store.save(state.copy(habits = state.habits.map { habit ->
                        if (habit.id != habitId) habit else HabitProgress.withAmount(
                            habit, today,
                            if (HabitProgress.isComplete(habit, today)) 0 else habit.target
                        )
                    }))
                }
            }
            COMMAND_OPEN_TASKS -> openSectionNow(context, "TASKS")
            COMMAND_OPEN_HABITS -> openSectionNow(context, "HABITS")
        }
    }

    private fun toggleFocusSession(context: Context, widgetId: Int) {
        val session = FocusSessionStore.load(context)
        if (session == null) {
            val focusSeconds = WidgetPreferences.load(context, widgetId).focusMinutes * 60
            val today = LocalDate.now()
            val task = LocalStore(context).load().tasks.firstOrNull {
                !it.completed && !it.archived && it.deletedAt == null && (it.dueDate == null || !it.dueDate.isAfter(today))
            }
            ContextCompat.startForegroundService(
                context,
                Intent(context, FocusTimerService::class.java).apply {
                    putExtra(
                        FocusTimerService.EXTRA_END_AT,
                        System.currentTimeMillis() + focusSeconds * 1000L
                    )
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

    companion object {
        private const val ACTION_COLLECTION = "com.trazo.app.widget.COLLECTION_ACTION"
        private const val ACTION_TOGGLE_FOCUS = "com.trazo.app.widget.MAIN_TOGGLE_FOCUS"

        internal const val EXTRA_COLLECTION_COMMAND = "collection_command"
        internal const val EXTRA_TASK_ID = "task_id"
        internal const val EXTRA_HABIT_ID = "habit_id"
        internal const val COMMAND_COMPLETE_TASK = "complete_task"
        internal const val COMMAND_TOGGLE_HABIT = "toggle_habit"
        internal const val COMMAND_OPEN_TASKS = "open_tasks"
        internal const val COMMAND_OPEN_HABITS = "open_habits"

        fun updateAll(context: Context) {
            updateSelf(context)
            HabitWidget.updateAll(context)
            FocusWidget.updateAll(context)
            TaskListWidget.updateAll(context)
            HabitListWidget.updateAll(context)
        }

        fun updateSelf(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TrazoWidget::class.java)
            manager.getAppWidgetIds(component).forEach { widgetId ->
                manager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_task_stack)
                manager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_habit_stack)
                manager.updateAppWidget(widgetId, createViews(context, widgetId))
            }
        }

        fun updateOne(context: Context, widgetId: Int) {
            val manager = AppWidgetManager.getInstance(context)
            manager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_task_stack)
            manager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_habit_stack)
            manager.updateAppWidget(widgetId, createViews(context, widgetId))
        }

        private fun createViews(context: Context, widgetId: Int): RemoteViews {
            val today = LocalDate.now()
            val state = LocalStore(context).load()
            val pendingTasks = state.tasks
                .filter { !it.completed && !it.archived && it.deletedAt == null &&
                    (it.dueDate == null || !it.dueDate.isAfter(today)) }
                .sortedWith(
                    compareByDescending<Task> { it.priority == TaskPriority.IMPORTANT }
                        .thenBy { it.dueDate ?: LocalDate.MAX }
                )
            val scheduledHabits = state.habits.filter {
                !it.archived && it.deletedAt == null && HabitProgress.isScheduled(it, today)
            }
            val habitsDone = scheduledHabits.count { HabitProgress.isComplete(it, today) }
            val focusSession = FocusSessionStore.load(context)
            val config = WidgetPreferences.load(context, widgetId)
            val minHeight = AppWidgetManager.getInstance(context)
                .getAppWidgetOptions(widgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 300)
            val compact = minHeight < 245
            var showTasks = config.showTasks
            var showHabits = config.showHabits
            if (compact) {
                val preferredTasks = config.firstSection == WidgetSection.TASKS
                val useTasks = when {
                    preferredTasks && showTasks && (pendingTasks.isNotEmpty() || !showHabits) -> true
                    !preferredTasks && showHabits && (scheduledHabits.isNotEmpty() || !showTasks) -> false
                    showTasks && pendingTasks.isNotEmpty() -> true
                    showHabits && scheduledHabits.isNotEmpty() -> false
                    else -> showTasks
                }
                showTasks = useTasks
                showHabits = !useTasks
            } else {
                if (pendingTasks.isEmpty() && scheduledHabits.isNotEmpty() && showHabits) showTasks = false
                if (scheduledHabits.isEmpty() && pendingTasks.isNotEmpty() && showTasks) showHabits = false
                if (pendingTasks.isEmpty() && scheduledHabits.isEmpty() && showTasks && showHabits) {
                    showTasks = config.firstSection == WidgetSection.TASKS
                    showHabits = !showTasks
                }
            }
            val progress = when {
                scheduledHabits.isNotEmpty() ->
                    (habitsDone * 100 / scheduledHabits.size).coerceIn(0, 100)
                pendingTasks.isEmpty() -> 100
                else -> 0
            }
            val locale = Locale.forLanguageTag("es-CL")
            val date = today.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale))
                .replaceFirstChar { it.titlecase(locale) }
            val greeting = when (LocalTime.now().hour) {
                in 5..11 -> "Buenos días · un trazo a la vez"
                in 12..19 -> "Tu tarde, de un vistazo"
                else -> "Cierra el día con calma"
            }

            return RemoteViews(context.packageName, R.layout.trazo_widget).apply {
                setInt(
                    R.id.widget_root,
                    "setBackgroundResource",
                    when (config.palette) {
                        WidgetPalette.CORAL -> R.drawable.widget_paper
                        WidgetPalette.BOTANICAL -> R.drawable.widget_paper_botanical
                        WidgetPalette.INK -> R.drawable.widget_paper_ink
                    }
                )
                setInt(
                    R.id.widget_focus_action,
                    "setBackgroundResource",
                    when (config.palette) {
                        WidgetPalette.CORAL -> R.drawable.widget_button_coral
                        WidgetPalette.BOTANICAL -> R.drawable.widget_button_botanical
                        WidgetPalette.INK -> R.drawable.widget_button_ink
                    }
                )
                setViewVisibility(R.id.widget_rhythm, if (compact) View.GONE else View.VISIBLE)
                setViewVisibility(R.id.widget_task_label, if (showTasks) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.widget_task_region, if (showTasks) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.widget_habit_label, if (showHabits) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.widget_habit_region, if (showHabits) View.VISIBLE else View.GONE)
                setTextViewText(
                    R.id.widget_task_label,
                    when (pendingTasks.size) {
                        0 -> "TAREAS  ·  TODO EN CALMA"
                        1 -> "TAREAS  ·  1 TRAZO"
                        else -> "TAREAS  ·  DESLIZA ↕"
                    }
                )
                setTextViewText(
                    R.id.widget_habit_label,
                    when (scheduledHabits.size) {
                        0 -> "HÁBITOS  ·  EL JARDÍN DESCANSA"
                        1 -> "HÁBITOS  ·  1 RITUAL"
                        else -> "HÁBITOS  ·  DESLIZA ↕"
                    }
                )
                setTextViewText(R.id.widget_date, date)
                setTextViewText(R.id.widget_greeting, greeting)
                setTextViewText(R.id.widget_progress_percent, "$progress%")
                setProgressBar(R.id.widget_day_progress, 100, progress, false)
                val pendingLabel = if (pendingTasks.size == 1) {
                    "1 pendiente"
                } else {
                    "${pendingTasks.size} pendientes"
                }
                setTextViewText(
                    R.id.widget_progress,
                    "$pendingLabel  ·  $habitsDone/${scheduledHabits.size} rituales"
                )

                setOnClickPendingIntent(
                    R.id.widget_header,
                    openSection(context, "TODAY", widgetId * 10)
                )
                setOnClickPendingIntent(
                    R.id.widget_planner,
                    openSection(context, "CALENDAR", widgetId * 10 + 1)
                )
                setOnClickPendingIntent(R.id.widget_focus_action, focusToggleIntent(context, widgetId))
                setOnClickPendingIntent(
                    R.id.widget_task_empty,
                    openSection(context, "TASKS", widgetId * 10 + 2)
                )
                setOnClickPendingIntent(
                    R.id.widget_habit_empty,
                    openSection(context, "HABITS", widgetId * 10 + 3)
                )

                setRemoteAdapter(
                    R.id.widget_task_stack,
                    collectionServiceIntent(context, widgetId, TrazoCollectionService.KIND_TASKS)
                )
                setRemoteAdapter(
                    R.id.widget_habit_stack,
                    collectionServiceIntent(context, widgetId, TrazoCollectionService.KIND_HABITS)
                )
                setEmptyView(R.id.widget_task_stack, R.id.widget_task_empty)
                setEmptyView(R.id.widget_habit_stack, R.id.widget_habit_empty)
                setPendingIntentTemplate(
                    R.id.widget_task_stack,
                    collectionTemplate(context, widgetId, TrazoCollectionService.KIND_TASKS)
                )
                setPendingIntentTemplate(
                    R.id.widget_habit_stack,
                    collectionTemplate(context, widgetId, TrazoCollectionService.KIND_HABITS)
                )

                setTextViewText(
                    R.id.widget_focus_action,
                    when (focusSession?.phase) {
                        "BREAK" -> "■  Detener descanso"
                        "FOCUS" -> "■  Detener enfoque"
                        else -> "🍅  Iniciar ${config.focusMinutes} min"
                    }
                )
            }
        }

        private fun collectionServiceIntent(context: Context, widgetId: Int, kind: String) =
            Intent(context, TrazoCollectionService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra(TrazoCollectionService.EXTRA_KIND, kind)
                .setData(Uri.parse("trazo://widget/collection/$widgetId/$kind"))

        private fun collectionTemplate(context: Context, widgetId: Int, kind: String): PendingIntent {
            val intent = Intent(context, TrazoWidget::class.java)
                .setAction(ACTION_COLLECTION)
                .setData(Uri.parse("trazo://widget/collection-action/$widgetId/$kind"))
            return PendingIntent.getBroadcast(
                context,
                widgetId * 50 + kind.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        private fun focusToggleIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, TrazoWidget::class.java)
                .setAction(ACTION_TOGGLE_FOCUS)
                .setData(Uri.parse("trazo://widget/main-focus/$widgetId"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            return PendingIntent.getBroadcast(
                context,
                widgetId * 30 + 7,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun openSection(context: Context, section: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setData(Uri.parse("trazo://open/$section"))
                .putExtra(MainActivity.EXTRA_SECTION, section)
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun openSectionNow(context: Context, section: String) {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    .setData(Uri.parse("trazo://open/$section"))
                    .putExtra(MainActivity.EXTRA_SECTION, section)
            )
        }
    }
}
