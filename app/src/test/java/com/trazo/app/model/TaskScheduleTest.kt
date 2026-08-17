package com.trazo.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskScheduleTest {
    private val today = LocalDate.of(2026, 7, 12)
    private val undated = Task(id = "none", title = "Sin fecha")
    private val overdue = Task(id = "past", title = "Atrasada", dueDate = today.minusDays(1))
    private val current = Task(id = "today", title = "Hoy", dueDate = today)
    private val future = Task(id = "future", title = "Futura", dueDate = today.plusDays(1))

    @Test
    fun `daily agenda contains only tasks assigned to that date`() {
        val result = TaskSchedule.onDate(listOf(undated, overdue, current, future), today)
        assertEquals(listOf("today"), result.map { it.id })
    }

    @Test
    fun `today includes undated overdue and current but not future tasks`() {
        val result = TaskSchedule.actionable(listOf(undated, overdue, current, future), today)
        assertEquals(setOf("none", "past", "today"), result.map { it.id }.toSet())
        assertTrue(future !in result)
    }
}
