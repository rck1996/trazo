package com.trazo.app

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.trazo.app.data.AppSettings
import com.trazo.app.data.AppSettingsStore
import com.trazo.app.data.FocusStats
import com.trazo.app.data.FocusStatsStore
import com.trazo.app.data.LocalStore
import com.trazo.app.model.Habit
import com.trazo.app.model.CategoryDefinition
import com.trazo.app.model.HabitCategory
import com.trazo.app.model.HabitProgress
import com.trazo.app.model.HabitUnit
import com.trazo.app.model.ItemReminderMode
import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import com.trazo.app.model.TaskRecurrence
import com.trazo.app.model.TaskSubtask
import com.trazo.app.model.TaskTemplate
import com.trazo.app.model.TrazoState
import java.time.DayOfWeek
import java.time.LocalDate

data class TaskInput(
    val title: String,
    val note: String = "",
    val important: Boolean = false,
    val dueDate: LocalDate? = null,
    val durationMinutes: Int = 25,
    val recurrence: TaskRecurrence = TaskRecurrence.NONE,
    val categoryId: String = "general",
    val subtasks: List<TaskSubtask> = emptyList(),
    val reminderHour: Int? = null,
    val reminderMinute: Int = 0,
    val reminderMode: ItemReminderMode? = null,
    val criticalAlarm: Boolean = false,
    val tags: Set<String> = emptySet()
)

data class HabitInput(
    val title: String,
    val emoji: String = "✦",
    val category: HabitCategory = HabitCategory.GENERAL,
    val categoryId: String = category.name.lowercase(),
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val repeatEveryWeeks: Int = 1,
    val skippedDates: Set<LocalDate> = emptySet(),
    val target: Int = 1,
    val unit: HabitUnit = HabitUnit.CHECK,
    val reminderHour: Int? = null,
    val reminderMinute: Int = 0,
    val reminderMode: ItemReminderMode? = null,
    val criticalAlarm: Boolean = false,
    val tags: Set<String> = emptySet()
)

data class UndoAction(
    val kind: String,
    val id: String,
    val message: String,
    val previousDueDate: LocalDate? = null,
    val previousHour: Int? = null,
    val previousMinute: Int = 0
)

class TrazoViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalStore(application)
    private val settingsStore = AppSettingsStore(application)
    private val _state = mutableStateOf(store.load())
    val state: State<TrazoState> = _state
    private val _settings = mutableStateOf(settingsStore.load())
    val settings: State<AppSettings> = _settings
    private val _undoAction = mutableStateOf<UndoAction?>(null)
    val undoAction: State<UndoAction?> = _undoAction
    private val _parentCompletionSuggestion = mutableStateOf<String?>(null)
    val parentCompletionSuggestion: State<String?> = _parentCompletionSuggestion

    fun refresh() { _state.value = store.load() }
    fun exportBackup(): String = store.exportJson()
    fun importBackup(raw: String): Result<Unit> = store.importJson(raw).map { _state.value = it }
    fun focusStats(): FocusStats = FocusStatsStore.load(getApplication())

    fun addCategory(name: String, symbol: String = "✦", colorArgb: Long = 0xFF5B7F67) {
        val clean = name.trim()
        if (clean.isBlank() || _state.value.categories.any { it.name.equals(clean, true) }) return
        update { state -> state.copy(categories = state.categories + CategoryDefinition(
            name = clean, symbol = symbol.ifBlank { "✦" }.take(2), colorArgb = colorArgb
        )) }
    }

    fun deleteCategory(id: String) = update { state ->
        if (id == "general") state else state.copy(
            categories = state.categories.filterNot { it.id == id },
            tasks = state.tasks.map { if (it.categoryId == id) it.copy(categoryId = "general") else it },
            habits = state.habits.map { if (it.categoryId == id) it.copy(categoryId = "general") else it },
            taskTemplates = state.taskTemplates.map { if (it.categoryId == id) it.copy(categoryId = "general") else it }
        )
    }

    fun saveTaskTemplate(name: String, input: TaskInput) {
        val clean = name.trim()
        if (clean.isBlank() || input.title.isBlank()) return
        update { state -> state.copy(taskTemplates = state.taskTemplates + TaskTemplate(
            name = clean, title = input.title.trim(), note = input.note.trim(),
            priority = if (input.important) TaskPriority.IMPORTANT else TaskPriority.CALM,
            durationMinutes = input.durationMinutes.coerceIn(5, 480), recurrence = input.recurrence,
            categoryId = input.categoryId, subtasks = input.subtasks.resetForReuse(),
            reminderHour = input.reminderHour, reminderMinute = input.reminderMinute,
            reminderMode = input.reminderMode, tags = input.tags.cleanTags()
        )) }
    }

    fun deleteTaskTemplate(id: String) = update { state ->
        state.copy(taskTemplates = state.taskTemplates.filterNot { it.id == id })
    }

    fun taskInputFromTemplate(template: TaskTemplate): TaskInput = TaskInput(
        title = template.title, note = template.note,
        important = template.priority == TaskPriority.IMPORTANT,
        durationMinutes = template.durationMinutes, recurrence = template.recurrence,
        categoryId = template.categoryId,
        subtasks = template.subtasks.resetForReuse(),
        reminderHour = template.reminderHour, reminderMinute = template.reminderMinute,
        reminderMode = template.reminderMode, tags = template.tags
    )

    fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        settingsStore.save(settings)
    }

    fun addTask(input: TaskInput) {
        if (input.title.isBlank()) return
        update { state -> state.copy(tasks = listOf(Task(
            title = input.title.trim(), note = input.note.trim(),
            priority = if (input.important) TaskPriority.IMPORTANT else TaskPriority.CALM,
            dueDate = input.dueDate,
            durationMinutes = input.durationMinutes.coerceIn(5, 480),
            recurrence = input.recurrence,
            categoryId = input.categoryId,
            subtasks = input.subtasks,
            reminderHour = input.reminderHour.takeIf { input.dueDate != null },
            reminderMinute = input.reminderMinute,
            reminderMode = input.reminderMode,
            criticalAlarm = input.criticalAlarm,
            tags = input.tags.cleanTags()
        )) + state.tasks) }
    }

    fun toggleTask(id: String) = update { state ->
        val updatedTasks = state.tasks.map { task ->
            if (task.id != id) task else {
                val completed = !task.completed
                task.copy(completed = completed, completedAt = if (completed) System.currentTimeMillis() else null)
            }
        }
        val completed = updatedTasks.firstOrNull { it.id == id }
        val nextTask = completed?.takeIf { it.completed && it.recurrence != TaskRecurrence.NONE && it.dueDate != null }?.let { task ->
            val nextDate = when (task.recurrence) {
                TaskRecurrence.DAILY -> task.dueDate!!.plusDays(1)
                TaskRecurrence.WEEKLY -> task.dueDate!!.plusWeeks(1)
                TaskRecurrence.MONTHLY -> task.dueDate!!.plusMonths(1)
                TaskRecurrence.NONE -> task.dueDate
            }
            task.copy(id = java.util.UUID.randomUUID().toString(), completed = false, completedAt = null, dueDate = nextDate, createdOn = LocalDate.now())
        }
        state.copy(tasks = updatedTasks + listOfNotNull(nextTask))
    }

    /** Marks one checklist entry without changing the completion state of its parent task. */
    fun toggleTaskSubtask(taskId: String, subtaskId: String) = update { state ->
        val tasks = state.tasks.map { task ->
            if (task.id != taskId) task else task.copy(
                subtasks = task.subtasks.map { subtask ->
                    val dependencyDone = subtask.dependsOnId == null || task.subtasks.any {
                        it.id == subtask.dependsOnId && it.completed
                    }
                    if (subtask.id == subtaskId && (subtask.completed || dependencyDone)) {
                        subtask.copy(completed = !subtask.completed)
                    } else subtask
                }
            )
        }
        tasks.firstOrNull { it.id == taskId }?.takeIf {
            !it.completed && it.subtasks.isNotEmpty() && it.subtasks.all(TaskSubtask::completed)
        }?.let { _parentCompletionSuggestion.value = it.id }
        state.copy(tasks = tasks)
    }

    fun dismissParentCompletionSuggestion() { _parentCompletionSuggestion.value = null }

    fun completeSuggestedParent() {
        _parentCompletionSuggestion.value?.let(::toggleTask)
        _parentCompletionSuggestion.value = null
    }

    fun deleteTask(id: String) {
        update { state -> state.copy(tasks = state.tasks.map {
            if (it.id == id) it.copy(deletedAt = System.currentTimeMillis()) else it
        }) }
        _undoAction.value = UndoAction("task", id, "Tarea enviada a la papelera")
    }

    fun setTaskDate(id: String, dueDate: LocalDate?) = update { state ->
        state.copy(tasks = state.tasks.map {
            if (it.id == id) it.copy(dueDate = dueDate, reminderHour = it.reminderHour.takeIf { dueDate != null }) else it
        })
    }

    /** Moves a planned task without opening the editor. Used by the Day agenda controls. */
    fun rescheduleTask(id: String, dueDate: LocalDate, hour: Int, minute: Int) {
        val original = _state.value.tasks.firstOrNull { it.id == id } ?: return
        update { state -> state.copy(tasks = state.tasks.map {
            if (it.id == id) it.copy(
                dueDate = dueDate,
                reminderHour = hour.coerceIn(0, 23),
                reminderMinute = minute.coerceIn(0, 59)
            ) else it
        }) }
        _undoAction.value = UndoAction(
            kind = "schedule",
            id = id,
            message = "Tarea reprogramada",
            previousDueDate = original.dueDate,
            previousHour = original.reminderHour,
            previousMinute = original.reminderMinute
        )
    }

    fun updateTask(id: String, input: TaskInput) {
        if (input.title.isBlank()) return
        update { state -> state.copy(tasks = state.tasks.map {
            if (it.id == id) it.copy(
                title = input.title.trim(), note = input.note.trim(),
                priority = if (input.important) TaskPriority.IMPORTANT else TaskPriority.CALM,
                dueDate = input.dueDate,
                durationMinutes = input.durationMinutes.coerceIn(5, 480),
                recurrence = input.recurrence,
                categoryId = input.categoryId,
                subtasks = input.subtasks,
                reminderHour = input.reminderHour.takeIf { input.dueDate != null },
                reminderMinute = input.reminderMinute,
                reminderMode = input.reminderMode,
                criticalAlarm = input.criticalAlarm,
                tags = input.tags.cleanTags()
            ) else it
        }) }
    }

    fun addHabit(input: HabitInput) {
        if (input.title.isBlank() || input.days.isEmpty()) return
        update { state -> state.copy(habits = state.habits + Habit(
            title = input.title.trim(), emoji = input.emoji.ifBlank { input.category.symbol }.take(2),
            category = input.category, categoryId = input.categoryId, activeDays = input.days,
            repeatEveryWeeks = input.repeatEveryWeeks.coerceIn(1, 12),
            skippedDates = input.skippedDates,
            target = input.target.coerceAtLeast(1), unit = input.unit,
            reminderHour = input.reminderHour, reminderMinute = input.reminderMinute,
            reminderMode = input.reminderMode,
            criticalAlarm = input.criticalAlarm,
            tags = input.tags.cleanTags()
        )) }
    }

    fun toggleHabit(id: String, date: LocalDate = LocalDate.now()) = update { state ->
        state.copy(habits = state.habits.map { habit ->
            if (habit.id != id) habit else HabitProgress.withAmount(
                habit, date, if (HabitProgress.isComplete(habit, date)) 0 else habit.target
            )
        })
    }

    fun adjustHabitProgress(id: String, delta: Int, date: LocalDate = LocalDate.now()) = update { state ->
        state.copy(habits = state.habits.map { habit ->
            if (habit.id != id) habit else HabitProgress.withAmount(
                habit, date, HabitProgress.amount(habit, date) + delta
            )
        })
    }

    fun toggleHabitException(id: String, date: LocalDate) = update { state ->
        state.copy(habits = state.habits.map { habit ->
            if (habit.id != id) habit else habit.copy(
                skippedDates = if (date in habit.skippedDates) habit.skippedDates - date else habit.skippedDates + date
            )
        })
    }

    fun deleteHabit(id: String) {
        update { state -> state.copy(habits = state.habits.map {
            if (it.id == id) it.copy(deletedAt = System.currentTimeMillis()) else it
        }) }
        _undoAction.value = UndoAction("habit", id, "Hábito enviado a la papelera")
    }

    fun updateHabit(id: String, input: HabitInput) {
        if (input.title.isBlank() || input.days.isEmpty()) return
        update { state -> state.copy(habits = state.habits.map {
            if (it.id == id) it.copy(
                title = input.title.trim(), emoji = input.emoji.ifBlank { input.category.symbol }.take(2),
                category = input.category, categoryId = input.categoryId, activeDays = input.days,
                repeatEveryWeeks = input.repeatEveryWeeks.coerceIn(1, 12),
                skippedDates = input.skippedDates,
                target = input.target.coerceAtLeast(1), unit = input.unit,
                reminderHour = input.reminderHour, reminderMinute = input.reminderMinute,
                reminderMode = input.reminderMode,
                criticalAlarm = input.criticalAlarm,
                tags = input.tags.cleanTags()
            ) else it
        }) }
    }

    fun archiveTask(id: String) {
        update { state -> state.copy(tasks = state.tasks.map {
            if (it.id == id) it.copy(archived = true) else it
        }) }
        _undoAction.value = UndoAction("task", id, "Tarea movida a Archivadas")
    }
    fun archiveHabit(id: String) {
        update { state -> state.copy(habits = state.habits.map {
            if (it.id == id) it.copy(archived = true) else it
        }) }
        _undoAction.value = UndoAction("habit", id, "Hábito movido a Archivadas")
    }
    fun restoreTask(id: String) = update { state -> state.copy(tasks = state.tasks.map {
        if (it.id == id) it.copy(archived = false, deletedAt = null) else it
    }) }
    fun restoreHabit(id: String) = update { state -> state.copy(habits = state.habits.map {
        if (it.id == id) it.copy(archived = false, deletedAt = null) else it
    }) }
    fun permanentlyDeleteTask(id: String) = update { it.copy(tasks = it.tasks.filterNot { task -> task.id == id }) }
    fun permanentlyDeleteHabit(id: String) = update { it.copy(habits = it.habits.filterNot { habit -> habit.id == id }) }

    fun undoLast() {
        val action = _undoAction.value ?: return
        when (action.kind) {
            "task" -> restoreTask(action.id)
            "habit" -> restoreHabit(action.id)
            "schedule" -> update { state -> state.copy(tasks = state.tasks.map { task ->
                if (task.id == action.id) task.copy(
                    dueDate = action.previousDueDate,
                    reminderHour = action.previousHour,
                    reminderMinute = action.previousMinute
                ) else task
            }) }
        }
        _undoAction.value = null
    }
    fun consumeUndo() { _undoAction.value = null }

    private inline fun update(transform: (TrazoState) -> TrazoState) {
        _state.value = transform(_state.value)
        store.save(_state.value)
    }

    private fun Set<String>.cleanTags(): Set<String> = mapNotNull {
        it.trim().lowercase().takeIf(String::isNotBlank)
    }.take(8).toSet()

    private fun List<TaskSubtask>.resetForReuse(): List<TaskSubtask> {
        val newIds = associate { it.id to java.util.UUID.randomUUID().toString() }
        return map { subtask ->
            subtask.copy(
                id = newIds.getValue(subtask.id),
                completed = false,
                dependsOnId = subtask.dependsOnId?.let(newIds::get)
            )
        }
    }
}
