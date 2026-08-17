package com.trazo.app.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitProgressTest {
    private val monday = LocalDate.of(2026, 7, 6)

    @Test
    fun `counts consecutive scheduled days and skips weekends`() {
        val habit = Habit(
            title = "Leer",
            activeDays = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            ),
            createdOn = monday.minusDays(7),
            completions = setOf(monday.minusDays(3), monday, monday.plusDays(1))
        )

        assertEquals(3, HabitProgress.streak(habit, monday.plusDays(1)))
    }

    @Test
    fun `does not lose yesterday streak before completing today`() {
        val habit = Habit(
            title = "Agua",
            activeDays = DayOfWeek.entries.toSet(),
            createdOn = monday,
            completions = setOf(monday, monday.plusDays(1))
        )

        assertEquals(2, HabitProgress.streak(habit, monday.plusDays(2)))
    }

    @Test
    fun `broken scheduled day resets streak`() {
        val habit = Habit(
            title = "Caminar",
            activeDays = DayOfWeek.entries.toSet(),
            createdOn = monday,
            completions = setOf(monday, monday.plusDays(2))
        )

        assertEquals(1, HabitProgress.streak(habit, monday.plusDays(2)))
    }
}
