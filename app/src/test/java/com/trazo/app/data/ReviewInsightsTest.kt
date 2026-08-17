package com.trazo.app.data

import com.trazo.app.model.Habit
import com.trazo.app.model.HabitProgress
import com.trazo.app.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ReviewInsightsTest {
    private val today = LocalDate.of(2026, 8, 17)

    @Test fun fortnightlyHabitAndExceptionAreRespected() {
        val habit = Habit(
            title = "Spinning",
            activeDays = setOf(DayOfWeek.MONDAY),
            repeatEveryWeeks = 2,
            skippedDates = setOf(today.plusWeeks(2)),
            createdOn = today
        )
        assertTrue(HabitProgress.isScheduled(habit, today))
        assertFalse(HabitProgress.isScheduled(habit, today.plusWeeks(1)))
        assertFalse(HabitProgress.isScheduled(habit, today.plusWeeks(2)))
        assertTrue(HabitProgress.isScheduled(habit, today.plusWeeks(4)))
    }

    @Test fun dailyReviewDetectsOverdueAndScheduledHabits() {
        val summary = ReviewInsights.daily(
            tasks = listOf(Task(title = "Atrasada", dueDate = today.minusDays(1))),
            habits = listOf(Habit(title = "Agua", createdOn = today)),
            date = today
        )
        assertEquals(1, summary.overdueTasks)
        assertEquals(1, summary.habitOpportunities)
        assertEquals(0, summary.habitsDone)
    }

    @Test fun suggestionUsesFocusWithoutSendingDataAnywhere() {
        val summary = ReviewInsights.daily(emptyList(), emptyList(), LocalDate.of(2026, 8, 17), focusMinutes = 95)
        assertTrue(summary.suggestion.contains("95 minutos"))
        assertTrue(summary.suggestion.contains("pausa"))
    }
}
