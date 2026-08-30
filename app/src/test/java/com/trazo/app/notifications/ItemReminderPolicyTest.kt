package com.trazo.app.notifications

import com.trazo.app.model.Habit
import com.trazo.app.model.ItemReminderMode
import com.trazo.app.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ItemReminderPolicyTest {
    private val today = LocalDate.of(2026, 8, 17)

    @Test
    fun `an archived task cannot keep an active reminder`() {
        val task = Task(title = "Informe", dueDate = today, reminderHour = 9, archived = true)

        assertFalse(task.hasActiveReminder())
    }

    @Test
    fun `a task in trash or completed cannot keep an active reminder`() {
        assertFalse(Task(title = "Informe", dueDate = today, reminderHour = 9, deletedAt = 1L).hasActiveReminder())
        assertFalse(Task(title = "Informe", dueDate = today, reminderHour = 9, completed = true).hasActiveReminder())
    }

    @Test
    fun `a pending dated task can keep an active reminder`() {
        assertTrue(Task(title = "Informe", dueDate = today, reminderHour = 9).hasActiveReminder())
    }

    @Test
    fun `an archived or completed habit cannot notify today`() {
        val habit = Habit(title = "Spinning", reminderHour = 9, createdOn = today)

        assertFalse(habit.copy(archived = true).hasActiveReminderOn(today))
        assertFalse(habit.copy(completions = setOf(today)).hasActiveReminderOn(today))
    }

    @Test
    fun `a scheduled pending habit can notify today`() {
        val habit = Habit(title = "Spinning", reminderHour = 9, createdOn = today)

        assertTrue(habit.hasActiveReminderOn(today))
    }

    @Test
    fun `item delivery override wins over global preference`() {
        val settings = ReminderSettings(deliveryMode = ReminderDeliveryMode.NOTIFICATION)

        assertEquals(
            ReminderDeliveryMode.BOTH_ALARMS,
            ItemReminderScheduler.effectiveDeliveryMode(ItemReminderMode.BOTH_ALARMS, settings)
        )
    }

    @Test
    fun `items without override keep the existing global preference`() {
        val settings = ReminderSettings(deliveryMode = ReminderDeliveryMode.EARLY_ALARM)

        assertEquals(
            ReminderDeliveryMode.EARLY_ALARM,
            ItemReminderScheduler.effectiveDeliveryMode(null, settings)
        )
    }
}
