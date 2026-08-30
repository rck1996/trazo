package com.trazo.app.ui

import com.trazo.app.model.Task
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarInsightsTest {
    private val date = LocalDate.of(2026, 8, 29)
    private fun task(hour: Int, minute: Int, duration: Int) = Task(
        title = "Bloque", dueDate = date, reminderHour = hour,
        reminderMinute = minute, durationMinutes = duration
    )

    @Test fun `detects overlapping calendar blocks`() {
        assertEquals(1, CalendarInsights.conflictCount(listOf(task(9, 0, 60), task(9, 30, 45)), date))
    }

    @Test fun `calculates free windows around scheduled work`() {
        val free = CalendarInsights.freeWindows(listOf(task(9, 0, 60), task(12, 0, 30)), date, 8, 14)
        assertEquals(listOf(60, 120, 90), free.map { it.durationMinutes })
    }

    @Test fun `sums planned duration`() {
        assertEquals(105, CalendarInsights.plannedMinutes(listOf(task(9, 0, 60), task(12, 0, 45)), date))
    }

    @Test fun `workload includes untimed work but ignores completed tasks`() {
        val untimed = Task(title = "Sin hora", dueDate = date, durationMinutes = 50)
        val done = Task(title = "Lista", dueDate = date, durationMinutes = 90, completed = true)
        assertEquals(110, CalendarInsights.workloadMinutes(listOf(task(9, 0, 60), untimed, done), date))
    }

    @Test fun `finds first conflict and every involved task`() {
        val first = task(9, 0, 60).copy(id = "a")
        val second = task(9, 30, 45).copy(id = "b")
        val separate = task(12, 0, 30).copy(id = "c")
        assertEquals(9 * 60, CalendarInsights.firstConflictStart(listOf(first, second, separate), date))
        assertEquals(setOf("a", "b"), CalendarInsights.conflictingTaskIds(listOf(first, second, separate), date))
    }
}
