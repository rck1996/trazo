package com.trazo.app.data

import com.trazo.app.model.Task
import java.time.LocalDate

data class TaskProgressStats(
    val openTasks: Int,
    val overdueTasks: Int,
    val scheduledThisWeek: Int,
    val completedSubtasks: Int,
    val totalSubtasks: Int
) {
    val checklistPercent: Int
        get() = if (totalSubtasks == 0) 0 else completedSubtasks * 100 / totalSubtasks
}

object TaskProgressInsights {
    fun from(tasks: List<Task>, today: LocalDate = LocalDate.now()): TaskProgressStats {
        val active = tasks.filter { !it.archived && it.deletedAt == null }
        val subtasks = active.flatMap(Task::subtasks)
        return TaskProgressStats(
            openTasks = active.count { !it.completed },
            overdueTasks = active.count { !it.completed && it.dueDate?.isBefore(today) == true },
            scheduledThisWeek = active.count { !it.completed && it.dueDate?.let { date -> date in today..today.plusDays(6) } == true },
            completedSubtasks = subtasks.count { it.completed },
            totalSubtasks = subtasks.size
        )
    }
}
