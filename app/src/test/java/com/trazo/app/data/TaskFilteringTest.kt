package com.trazo.app.data

import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskFilteringTest {
    private val today = LocalDate.of(2026, 8, 28)
    private val tasks = listOf(
        Task(title = "Atrasada", dueDate = today.minusDays(1), priority = TaskPriority.IMPORTANT),
        Task(title = "Hoy", dueDate = today),
        Task(title = "Importante semanal", dueDate = today.plusDays(2), priority = TaskPriority.IMPORTANT),
        Task(title = "Después", dueDate = today.plusDays(8)),
        Task(title = "Sin fecha"),
        Task(title = "Terminada", dueDate = today, completed = true)
    )

    @Test fun overdueOnlyExcludesCompletedTasks() {
        val visible = TaskFiltering.apply(tasks, "", TaskFilterSelection(status = TaskStatusFilter.OVERDUE), today)
        assertEquals(listOf("Atrasada"), visible.map(Task::title))
    }

    @Test fun dateAndPriorityFiltersCanBeCombined() {
        val visible = TaskFiltering.apply(
            tasks, "", TaskFilterSelection(
                status = TaskStatusFilter.ALL,
                date = TaskDateFilter.THIS_WEEK,
                priority = TaskPriorityFilter.IMPORTANT
            ), today
        )
        assertEquals(listOf("Atrasada", "Importante semanal"), visible.map(Task::title))
    }

    @Test fun progressStatsIgnoreArchivedAndDeletedTasks() {
        val stats = TaskProgressInsights.from(
            listOf(
                Task(title = "Activa", subtasks = listOf(
                    com.trazo.app.model.TaskSubtask(title = "Uno", completed = true),
                    com.trazo.app.model.TaskSubtask(title = "Dos")
                )),
                Task(title = "Archivada", archived = true, subtasks = listOf(com.trazo.app.model.TaskSubtask(title = "No cuenta"))),
                Task(title = "Borrada", deletedAt = 1L, subtasks = listOf(com.trazo.app.model.TaskSubtask(title = "No cuenta")))
            ), today
        )
        assertEquals(1, stats.openTasks)
        assertEquals(1, stats.completedSubtasks)
        assertEquals(2, stats.totalSubtasks)
        assertEquals(50, stats.checklistPercent)
    }
}
