package com.trazo.app.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

enum class TaskPriority { CALM, IMPORTANT }
enum class TaskRecurrence { NONE, DAILY, WEEKLY, MONTHLY }

enum class HabitCategory(val label: String, val symbol: String) {
    GENERAL("General", "✦"),
    HYDRATION("Hidratación", "💧"),
    SELF_CARE("Autocuidado", "🌿"),
    FOOD("Alimentación", "🥣"),
    MOVEMENT("Movimiento", "👟"),
    REST("Descanso", "☾");

    companion object {
        /** One-time best effort for backups created before categories existed. */
        fun infer(title: String): HabitCategory {
            val text = java.text.Normalizer.normalize(title.lowercase(), java.text.Normalizer.Form.NFD)
                .replace("\\p{M}+".toRegex(), "")
            return when {
                listOf("agua", "hidrat", "beber", "vaso").any(text::contains) -> HYDRATION
                listOf("skin", "cuid", "piel", "ducha", "medit", "respir").any(text::contains) -> SELF_CARE
                listOf("comer", "comida", "desayun", "almuerzo", "cena", "fruta").any(text::contains) -> FOOD
                listOf("caminar", "correr", "ejercicio", "entren", "gym", "yoga", "pasos", "spinning", "bici", "cicl").any(text::contains) -> MOVEMENT
                listOf("dormir", "sueno", "descanso", "cama", "siesta").any(text::contains) -> REST
                else -> GENERAL
            }
        }
    }
}

enum class HabitUnit(val label: String, val shortLabel: String) {
    CHECK("completado", "vez"),
    TIMES("veces", "veces"),
    MINUTES("minutos", "min"),
    STEPS("pasos", "pasos")
}

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val priority: TaskPriority = TaskPriority.CALM,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val createdOn: LocalDate = LocalDate.now(),
    val dueDate: LocalDate? = null,
    /** Estimated effort used by Planner and Pomodoro suggestions. */
    val durationMinutes: Int = 25,
    val recurrence: TaskRecurrence = TaskRecurrence.NONE,
    val reminderHour: Int? = null,
    val reminderMinute: Int = 0,
    val tags: Set<String> = emptySet(),
    val archived: Boolean = false,
    val deletedAt: Long? = null
)

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "✦",
    val category: HabitCategory = HabitCategory.GENERAL,
    val activeDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val repeatEveryWeeks: Int = 1,
    val skippedDates: Set<LocalDate> = emptySet(),
    val completions: Set<LocalDate> = emptySet(),
    val progress: Map<LocalDate, Int> = emptyMap(),
    val target: Int = 1,
    val unit: HabitUnit = HabitUnit.CHECK,
    val reminderHour: Int? = null,
    val reminderMinute: Int = 0,
    val tags: Set<String> = emptySet(),
    val archived: Boolean = false,
    val deletedAt: Long? = null,
    val createdOn: LocalDate = LocalDate.now()
)

data class TrazoState(
    val tasks: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList()
)

object HabitProgress {
    fun isBaseScheduled(habit: Habit, date: LocalDate): Boolean =
        date >= habit.createdOn &&
            date.dayOfWeek in habit.activeDays &&
            (java.time.temporal.ChronoUnit.WEEKS.between(
                habit.createdOn.with(DayOfWeek.MONDAY),
                date.with(DayOfWeek.MONDAY)
            ) % habit.repeatEveryWeeks.coerceAtLeast(1) == 0L)

    fun isScheduled(habit: Habit, date: LocalDate): Boolean =
        isBaseScheduled(habit, date) && date !in habit.skippedDates

    fun streak(habit: Habit, today: LocalDate = LocalDate.now()): Int {
        if (habit.activeDays.isEmpty()) return 0
        var cursor = today
        if (isScheduled(habit, cursor) && !isComplete(habit, cursor)) {
            cursor = cursor.minusDays(1)
        }
        var result = 0
        while (cursor >= habit.createdOn) {
            if (isScheduled(habit, cursor)) {
                if (isComplete(habit, cursor)) result++ else break
            }
            cursor = cursor.minusDays(1)
        }
        return result
    }

    fun amount(habit: Habit, date: LocalDate): Int =
        habit.progress[date] ?: if (date in habit.completions) habit.target.coerceAtLeast(1) else 0

    fun isComplete(habit: Habit, date: LocalDate): Boolean =
        amount(habit, date) >= habit.target.coerceAtLeast(1)

    fun withAmount(habit: Habit, date: LocalDate, amount: Int): Habit {
        val target = habit.target.coerceAtLeast(1)
        val safe = amount.coerceIn(0, target)
        return habit.copy(
            progress = if (safe == 0) habit.progress - date else habit.progress + (date to safe),
            completions = if (safe >= target) habit.completions + date else habit.completions - date
        )
    }
}

object TaskSchedule {
    fun onDate(tasks: List<Task>, date: LocalDate): List<Task> = tasks.filter { it.dueDate == date && !it.archived && it.deletedAt == null }

    fun actionable(tasks: List<Task>, date: LocalDate): List<Task> =
        tasks.filter { !it.archived && it.deletedAt == null && (it.dueDate == null || !it.dueDate.isAfter(date)) }
}
