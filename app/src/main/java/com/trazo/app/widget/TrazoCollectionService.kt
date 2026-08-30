package com.trazo.app.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.trazo.app.R
import com.trazo.app.data.LocalStore
import com.trazo.app.model.Habit
import com.trazo.app.model.HabitCategory
import com.trazo.app.model.HabitProgress
import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import java.time.LocalDate

/** Supplies the two single-row, gesture-scrollable lists inside the main widget. */
class TrazoCollectionService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        CollectionFactory(
            applicationContext,
            intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1),
            intent.getStringExtra(EXTRA_KIND) ?: KIND_TASKS
        )

    private class CollectionFactory(
        private val context: Context,
        private val widgetId: Int,
        private val kind: String
    ) : RemoteViewsFactory {
        private var tasks: List<Task> = emptyList()
        private var habits: List<Habit> = emptyList()
        private var today: LocalDate = LocalDate.now()
        private var itemHeightDp: Int? = null

        override fun onCreate() = Unit

        override fun onDataSetChanged() {
            today = LocalDate.now()
            val state = LocalStore(context).load()
            val config = WidgetPreferences.load(context, widgetId)
            val maxItems = config.maxItems
            tasks = state.tasks
                .filter { !it.completed && !it.archived && it.deletedAt == null &&
                    (it.dueDate == null || !it.dueDate.isAfter(today)) }
                .filter { config.tagFilter.isBlank() || config.tagFilter in it.tags }
                .filter { !config.overdueOnly || it.dueDate?.isBefore(today) == true }
                .sortedWith(
                    compareByDescending<Task> { it.priority == TaskPriority.IMPORTANT }
                        .thenBy { it.dueDate ?: LocalDate.MAX }
                ).take(maxItems)
            habits = state.habits.filter {
                !it.archived && it.deletedAt == null && HabitProgress.isScheduled(it, today)
            }.filter { config.tagFilter.isBlank() || config.tagFilter in it.tags }.take(maxItems)
            val minHeight = android.appwidget.AppWidgetManager.getInstance(context)
                .getAppWidgetOptions(widgetId)
                .getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 300)
            val compact = minHeight < 245
            var visibleTasks = config.showTasks
            var visibleHabits = config.showHabits
            if (compact) {
                val preferTasks = config.firstSection == WidgetSection.TASKS
                val useTasks = when {
                    preferTasks && visibleTasks && (tasks.isNotEmpty() || !visibleHabits) -> true
                    !preferTasks && visibleHabits && (habits.isNotEmpty() || !visibleTasks) -> false
                    visibleTasks && tasks.isNotEmpty() -> true
                    visibleHabits && habits.isNotEmpty() -> false
                    else -> visibleTasks
                }
                visibleTasks = useTasks
                visibleHabits = !useTasks
            } else {
                if (tasks.isEmpty() && habits.isNotEmpty() && visibleHabits) visibleTasks = false
                if (habits.isEmpty() && tasks.isNotEmpty() && visibleTasks) visibleHabits = false
            }
            val thisSectionIsOnlyOne = if (kind == KIND_TASKS) {
                visibleTasks && !visibleHabits
            } else {
                visibleHabits && !visibleTasks
            }
            val count = if (kind == KIND_TASKS) tasks.size else habits.size
            val baseHeight = if (kind == KIND_TASKS) 62 else 54
            itemHeightDp = if (thisSectionIsOnlyOne && count > 0) {
                val fixedSpace = if (compact) 132 else 176
                ((minHeight - fixedSpace) / count).coerceIn(baseHeight, 86)
            } else null
        }

        override fun onDestroy() {
            tasks = emptyList()
            habits = emptyList()
        }

        override fun getCount(): Int = if (kind == KIND_TASKS) tasks.size else habits.size

        override fun getViewAt(position: Int): RemoteViews? =
            if (kind == KIND_TASKS) taskView(position) else habitView(position)

        private fun taskView(position: Int): RemoteViews? {
            val task = tasks.getOrNull(position) ?: return null
            val completedSteps = task.subtasks.count { it.completed }
            val stepsLabel = task.subtasks.takeIf { it.isNotEmpty() }
                ?.let { "$completedSteps/${it.size} pasos" }
            return RemoteViews(context.packageName, R.layout.widget_task_stack_item).apply {
                val config = WidgetPreferences.load(context, widgetId)
                val minimal = config.style == WidgetStyle.MINIMAL
                setViewVisibility(R.id.widget_task_item_image, if (minimal) android.view.View.GONE else android.view.View.VISIBLE)
                if (minimal) {
                    setInt(R.id.widget_task_item, "setBackgroundResource", R.drawable.widget_surface_minimal)
                    setInt(R.id.widget_task_item_complete, "setBackgroundResource", R.drawable.widget_button_minimal)
                    setTextColor(R.id.widget_task_item_mark, android.graphics.Color.DKGRAY)
                    setTextColor(R.id.widget_task_item_title, android.graphics.Color.BLACK)
                    setTextColor(R.id.widget_task_item_meta, android.graphics.Color.DKGRAY)
                    setTextColor(R.id.widget_task_item_complete, android.graphics.Color.WHITE)
                }
                if (Build.VERSION.SDK_INT >= 31) itemHeightDp?.let {
                    setViewLayoutHeight(R.id.widget_task_item, it.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                }
                setTextViewText(R.id.widget_task_item_title, if (config.privacy == WidgetPrivacy.DISCREET) "Tarea privada" else task.title)
                setTextViewText(
                    R.id.widget_task_item_mark,
                    "${position + 1}/${tasks.size} · " +
                        if (config.privacy == WidgetPrivacy.DISCREET) "PRIVADO"
                        else if (task.priority == TaskPriority.IMPORTANT) "★ IMPORTANTE" else "PRÓXIMO TRAZO"
                )
                setTextViewText(
                    R.id.widget_task_item_meta,
                    if (config.privacy == WidgetPrivacy.DISCREET) "Toca ✓ cuando esté listo"
                    else listOfNotNull(stepsLabel, task.note.takeIf { it.isNotBlank() })
                        .joinToString(" · ")
                        .ifBlank { "Toca ✓ cuando esté listo" }
                )
                setContentDescription(R.id.widget_task_item_complete,
                    if (config.privacy == WidgetPrivacy.DISCREET) "Completar tarea privada" else "Completar ${task.title}")
                val open = Intent()
                    .putExtra(TrazoWidget.EXTRA_COLLECTION_COMMAND, TrazoWidget.COMMAND_OPEN_TASKS)
                setOnClickFillInIntent(R.id.widget_task_item_image, open)
                setOnClickFillInIntent(R.id.widget_task_item_title, open)
                setOnClickFillInIntent(R.id.widget_task_item_meta, open)
                setOnClickFillInIntent(
                    R.id.widget_task_item_complete,
                    Intent()
                        .putExtra(TrazoWidget.EXTRA_COLLECTION_COMMAND, TrazoWidget.COMMAND_COMPLETE_TASK)
                        .putExtra(TrazoWidget.EXTRA_TASK_ID, task.id)
                )
            }
        }

        private fun habitView(position: Int): RemoteViews? {
            val habit = habits.getOrNull(position) ?: return null
            val isDone = HabitProgress.isComplete(habit, today)
            val streak = HabitProgress.streak(habit, today)
            return RemoteViews(context.packageName, R.layout.widget_habit_stack_item).apply {
                val config = WidgetPreferences.load(context, widgetId)
                val minimal = config.style == WidgetStyle.MINIMAL
                setViewVisibility(R.id.widget_habit_item_image, if (minimal) android.view.View.GONE else android.view.View.VISIBLE)
                if (minimal) {
                    setInt(R.id.widget_habit_item, "setBackgroundResource", R.drawable.widget_surface_minimal)
                    setInt(R.id.widget_habit_item_check, "setBackgroundResource", R.drawable.widget_button_minimal)
                    setTextColor(R.id.widget_habit_item_title, android.graphics.Color.BLACK)
                    setTextColor(R.id.widget_habit_item_meta, android.graphics.Color.DKGRAY)
                    setTextColor(R.id.widget_habit_item_check, android.graphics.Color.WHITE)
                }
                if (Build.VERSION.SDK_INT >= 31) itemHeightDp?.let {
                    setViewLayoutHeight(R.id.widget_habit_item, it.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                }
                setImageViewResource(R.id.widget_habit_item_image, habitIllustration(habit))
                setTextViewText(R.id.widget_habit_item_title,
                    if (config.privacy == WidgetPrivacy.DISCREET) "Ritual privado"
                    else if (minimal) habit.title else "${habit.emoji}  ${habit.title}")
                setTextViewText(
                    R.id.widget_habit_item_meta,
                    if (config.privacy == WidgetPrivacy.DISCREET) "Toca ✓ para cambiar el estado"
                    else when {
                        isDone -> "${position + 1}/${habits.size} · COMPLETADO"
                        habit.target > 1 -> "${HabitProgress.amount(habit, today)}/${habit.target} ${habit.unit.shortLabel}"
                        streak == 0 -> "${position + 1}/${habits.size} · empieza tu racha"
                        else -> "${position + 1}/${habits.size} · $streak días de racha"
                    }
                )
                setTextViewText(R.id.widget_habit_item_check, if (isDone) "↶" else "✓")
                setContentDescription(
                    R.id.widget_habit_item_check,
                    if (config.privacy == WidgetPrivacy.DISCREET) {
                        if (isDone) "Desmarcar ritual privado" else "Completar ritual privado"
                    } else if (isDone) "Desmarcar ${habit.title}" else "Completar ${habit.title}"
                )
                val open = Intent()
                    .putExtra(TrazoWidget.EXTRA_COLLECTION_COMMAND, TrazoWidget.COMMAND_OPEN_HABITS)
                setOnClickFillInIntent(R.id.widget_habit_item_image, open)
                setOnClickFillInIntent(R.id.widget_habit_item_title, open)
                setOnClickFillInIntent(R.id.widget_habit_item_meta, open)
                setOnClickFillInIntent(
                    R.id.widget_habit_item_check,
                    Intent()
                        .putExtra(TrazoWidget.EXTRA_COLLECTION_COMMAND, TrazoWidget.COMMAND_TOGGLE_HABIT)
                        .putExtra(TrazoWidget.EXTRA_HABIT_ID, habit.id)
                )
            }
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long =
            if (kind == KIND_TASKS) tasks.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()
            else habits.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()
        override fun hasStableIds(): Boolean = true

        private fun habitIllustration(habit: Habit): Int {
            return when (habit.category) {
                HabitCategory.HYDRATION -> R.drawable.habit_ai_hydration
                HabitCategory.SELF_CARE -> R.drawable.habit_ai_selfcare
                HabitCategory.FOOD -> R.drawable.habit_ai_food
                HabitCategory.MOVEMENT -> R.drawable.habit_ai_movement
                HabitCategory.REST -> R.drawable.habit_ai_rest
                HabitCategory.GENERAL -> R.drawable.widget_ai_ritual
            }
        }
    }

    companion object {
        const val EXTRA_KIND = "collection_kind"
        const val KIND_TASKS = "tasks"
        const val KIND_HABITS = "habits"
    }
}
