package com.trazo.app.debug

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.trazo.app.MainActivity
import com.trazo.app.data.LocalStore
import com.trazo.app.model.Habit
import com.trazo.app.model.HabitCategory
import com.trazo.app.model.HabitUnit
import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import com.trazo.app.model.TaskSubtask
import com.trazo.app.model.TrazoState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Debug-only deterministic workload used for calendar and responsive QA. */
class DemoDataActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalStore(this).save(demoState(LocalDate.now()))
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}

private fun demoState(today: LocalDate): TrazoState {
    val month = YearMonth.from(today)
    val first = month.atDay(1)
    fun date(day: Int) = month.atDay(day.coerceIn(1, month.lengthOfMonth()))
    fun steps(vararg values: Pair<String, Boolean>) = values.mapIndexed { index, value ->
        TaskSubtask(id = "step-$index-${value.first}", title = value.first, completed = value.second)
    }
    fun task(
        id: String, title: String, day: LocalDate, hour: Int? = null, minute: Int = 0,
        duration: Int = 25, completed: Boolean = false, important: Boolean = false,
        subtasks: List<TaskSubtask> = emptyList()
    ) = Task(
        id = id, title = title, createdOn = first, dueDate = day,
        reminderHour = hour, reminderMinute = minute, durationMinutes = duration,
        completed = completed, priority = if (important) TaskPriority.IMPORTANT else TaskPriority.CALM,
        categoryId = if (important) "movement" else "general", subtasks = subtasks
    )

    val tasks = listOf(
        task("monthly-plan", "Plan mensual", date(3), 9, duration = 60, completed = true),
        task("dentist", "Control dental", date(5), 16, duration = 45),
        task("invoice", "Enviar facturas", date(8), 11, duration = 40, completed = true),
        task("groceries", "Compra de la semana", date(12), 18, duration = 60),
        task("reading", "Terminar capítulo", date(15), 20, duration = 45),
        task("presentation", "Preparar presentación", date(18), 9, duration = 120, important = true,
            subtasks = steps("Ordenar ideas" to true, "Diseñar diapositivas" to true, "Ensayar" to false, "Enviar" to false)),
        task("call", "Llamar al banco", date(20), 13, duration = 30),
        task("training", "Entrenamiento largo", date(22), 8, duration = 90, completed = true),
        task("report", "Cerrar informe trimestral", today, 9, duration = 90, important = true,
            subtasks = steps("Reunir métricas" to true, "Validar cifras" to true, "Redactar conclusiones" to false, "Revisar" to false, "Enviar" to false)),
        task("team", "Reunión de equipo y planificación semanal", today, 10, 15, 60, important = true),
        task("design", "Revisar propuesta de diseño", today, 10, 30, 45),
        task("mail", "Responder correos importantes", today, 12, duration = 30, completed = true),
        task("lunch", "Preparar almuerzo", today, 13, duration = 45),
        task("walk", "Caminata consciente", today, 17, 30, 40),
        task("backup", "Respaldar documentos", today, duration = 25,
            subtasks = steps("Ordenar carpetas" to true, "Subir archivos" to false)),
        task("tomorrow", "Organizar escritorio", today.plusDays(1), 10, duration = 35),
        task("review", "Revisión de la semana", today.plusDays(1), 18, duration = 45),
        task("project", "Avanzar proyecto personal", today.plusDays(2), 19, duration = 75,
            subtasks = steps("Definir alcance" to true, "Construir prototipo" to false, "Probar" to false)),
        task("appointment", "Reservar hora médica", date(27), 8, 30, 20, completed = true),
        task("budget", "Revisar presupuesto", date(28), 14, duration = 60),
        task("cleanup", "Limpieza profunda", date(month.lengthOfMonth()), 11, duration = 120)
    )

    val allDays = DayOfWeek.entries.toSet()
    val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    val habits = listOf(
        Habit(id = "water", title = "Beber agua", emoji = "💧", category = HabitCategory.HYDRATION,
            activeDays = allDays, createdOn = first, target = 8, unit = HabitUnit.TIMES,
            progress = mapOf(today to 5, today.minusDays(1) to 8), completions = setOf(today.minusDays(1))),
        Habit(id = "stretch", title = "Movilidad de mañana", emoji = "🌿", category = HabitCategory.MOVEMENT,
            activeDays = weekdays, createdOn = first, completions = setOf(today.minusDays(1), today.minusDays(2))),
        Habit(id = "reading-habit", title = "Leer 20 minutos", emoji = "✦", category = HabitCategory.SELF_CARE,
            activeDays = allDays, createdOn = first, target = 20, unit = HabitUnit.MINUTES,
            progress = mapOf(today to 20), completions = setOf(today)),
        Habit(id = "sleep", title = "Preparar descanso", emoji = "☾", category = HabitCategory.REST,
            activeDays = allDays, createdOn = first)
    )
    return TrazoState(tasks = tasks, habits = habits)
}
