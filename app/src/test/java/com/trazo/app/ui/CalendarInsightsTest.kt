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
}
