package com.trazo.app.data

import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import java.time.LocalDate

/** Small, pure filter policy shared by the task list and its future widget views. */
enum class TaskStatusFilter(val label: String) {
    OPEN("Pendientes"),
    TODAY("Hoy"),
    OVERDUE("Atrasadas"),
    DONE("Hechas"),
    ALL("Todas")
}

enum class TaskDateFilter(val label: String) {
    ANY("Cualquier fecha"),
    UNSCHEDULED("Sin fecha"),
    THIS_WEEK("Esta semana"),
    LATER("Más adelante")
}

enum class TaskPriorityFilter(val label: String) {
    ANY("Cualquier prioridad"),
    IMPORTANT("Importantes"),
    CALM("Tranquilas")
}

data class TaskFilterSelection(
    val status: TaskStatusFilter = TaskStatusFilter.OPEN,
    val date: TaskDateFilter = TaskDateFilter.ANY,
    val priority: TaskPriorityFilter = TaskPriorityFilter.ANY
)

object TaskFiltering {
    fun apply(
        tasks: List<Task>,
        query: String,
        selection: TaskFilterSelection,
        today: LocalDate = LocalDate.now()
    ): List<Task> {
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val endOfWeek = weekStart.plusDays(6)
        return tasks.filter { task ->
            val matchesText = query.isBlank() || task.title.contains(query, true) ||
                task.note.contains(query, true) || task.tags.any { tag -> tag.contains(query, true) }
            val matchesStatus = when (selection.status) {
                TaskStatusFilter.OPEN -> !task.completed
                TaskStatusFilter.TODAY -> !task.completed && (task.dueDate == null || !task.dueDate.isAfter(today))
                TaskStatusFilter.OVERDUE -> !task.completed && task.dueDate?.isBefore(today) == true
                TaskStatusFilter.DONE -> task.completed
                TaskStatusFilter.ALL -> true
            }
            val matchesDate = when (selection.date) {
                TaskDateFilter.ANY -> true
                TaskDateFilter.UNSCHEDULED -> task.dueDate == null
                TaskDateFilter.THIS_WEEK -> task.dueDate?.let { it in weekStart..endOfWeek } == true
                TaskDateFilter.LATER -> task.dueDate?.isAfter(endOfWeek) == true
            }
            val matchesPriority = when (selection.priority) {
                TaskPriorityFilter.ANY -> true
                TaskPriorityFilter.IMPORTANT -> task.priority == TaskPriority.IMPORTANT
                TaskPriorityFilter.CALM -> task.priority == TaskPriority.CALM
            }
            matchesText && matchesStatus && matchesDate && matchesPriority
        }.sortedWith(
            compareBy<Task> { it.completed }
                .thenBy { it.dueDate ?: LocalDate.MAX }
                .thenByDescending { it.priority }
        )
    }
}
