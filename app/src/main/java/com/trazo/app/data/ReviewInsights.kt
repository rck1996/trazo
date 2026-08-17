package com.trazo.app.data

import com.trazo.app.model.Habit
import com.trazo.app.model.HabitProgress
import com.trazo.app.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReviewSummary(
    val completedTasks: Int,
    val overdueTasks: Int,
    val habitsDone: Int,
    val habitOpportunities: Int,
    val suggestion: String
)

object ReviewInsights {
    fun daily(tasks: List<Task>, habits: List<Habit>, date: LocalDate = LocalDate.now()): ReviewSummary {
        val activeTasks = tasks.filter { !it.archived && it.deletedAt == null }
        val completed = activeTasks.count { it.completedAt?.toLocalDate() == date }
        val overdue = activeTasks.count { !it.completed && it.dueDate?.isBefore(date) == true }
        val scheduled = habits.filter { !it.archived && it.deletedAt == null && HabitProgress.isScheduled(it, date) }
        val done = scheduled.count { HabitProgress.isComplete(it, date) }
        return ReviewSummary(completed, overdue, done, scheduled.size, suggestion(overdue, done, scheduled.size))
    }

    fun weekly(tasks: List<Task>, habits: List<Habit>, end: LocalDate = LocalDate.now()): ReviewSummary {
        val start = end.minusDays(6)
        val activeTasks = tasks.filter { !it.archived && it.deletedAt == null }
        val completed = activeTasks.count { task -> task.completedAt?.toLocalDate()?.let { it in start..end } == true }
        val overdue = activeTasks.count { !it.completed && it.dueDate?.isBefore(end) == true }
        var opportunities = 0
        var done = 0
        generateSequence(start) { it.plusDays(1).takeIf { next -> !next.isAfter(end) } }.forEach { date ->
            habits.filter { !it.archived && it.deletedAt == null && HabitProgress.isScheduled(it, date) }.forEach { habit ->
                opportunities++
                if (HabitProgress.isComplete(habit, date)) done++
            }
        }
        return ReviewSummary(completed, overdue, done, opportunities, suggestion(overdue, done, opportunities))
    }

    private fun suggestion(overdue: Int, done: Int, total: Int): String = when {
        overdue >= 3 -> "Hay $overdue tareas atrasadas. Reprograma, divide o archiva las que ya no importan."
        total >= 4 && done * 2 < total -> "Reduce un ritual o cambia su horario: la constancia mejora cuando el plan respira."
        total > 0 && done == total -> "Tus rituales están al día. Protege este ritmo sin agregar más por obligación."
        else -> "Elige un siguiente trazo pequeño y reserva un bloque de enfoque para terminarlo."
    }

    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault()).toLocalDate()
}
