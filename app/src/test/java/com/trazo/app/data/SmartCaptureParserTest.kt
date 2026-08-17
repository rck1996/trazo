package com.trazo.app.data

import com.trazo.app.model.HabitCategory
import com.trazo.app.model.HabitUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.DayOfWeek

class SmartCaptureParserTest {
    private val today = LocalDate.of(2026, 8, 16)

    @Test fun parsesTaskDateTimePriorityAndTag() {
        val result = SmartCaptureParser.parse("Llamar a Ana mañana a las 18:30 importante #trabajo", today)
            as SmartCaptureResult.TaskDraft
        assertEquals("Llamar a Ana", result.input.title)
        assertEquals(today.plusDays(1), result.input.dueDate)
        assertEquals(18, result.input.reminderHour)
        assertEquals(30, result.input.reminderMinute)
        assertTrue(result.input.important)
        assertEquals(setOf("trabajo"), result.input.tags)
    }

    @Test fun detectsDailyHabitAndCategory() {
        val result = SmartCaptureParser.parse("Beber agua todos los días a las 9", today)
            as SmartCaptureResult.HabitDraft
        assertEquals("Beber agua", result.input.title)
        assertEquals(HabitCategory.HYDRATION, result.input.category)
        assertEquals(9, result.input.reminderHour)
    }

    @Test fun configuresSpinningOnThreeWeekdays() {
        val result = SmartCaptureParser.parse("Spinning lunes miércoles viernes", today)
            as SmartCaptureResult.HabitDraft
        assertEquals("Spinning", result.input.title)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), result.input.days)
        assertEquals(HabitCategory.MOVEMENT, result.input.category)
        assertEquals(HabitCategory.MOVEMENT.symbol, result.input.emoji)
    }

    @Test fun understandsWeekdayRangeAndReminder() {
        val result = SmartCaptureParser.parse("Meditar de lunes a viernes a las 7:15", today)
            as SmartCaptureResult.HabitDraft
        assertEquals("Meditar", result.input.title)
        assertEquals(DayOfWeek.entries.take(5).toSet(), result.input.days)
        assertEquals(7, result.input.reminderHour)
        assertEquals(15, result.input.reminderMinute)
    }

    @Test fun understandsWeekendAndMinutesTarget() {
        val result = SmartCaptureParser.parse("Leer 30 minutos fines de semana", today)
            as SmartCaptureResult.HabitDraft
        assertEquals("Leer", result.input.title)
        assertEquals(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), result.input.days)
        assertEquals(30, result.input.target)
        assertEquals(HabitUnit.MINUTES, result.input.unit)
    }

    @Test fun understandsStepsAndAbbreviatedDays() {
        val result = SmartCaptureParser.parse("Caminar 8000 pasos lun mie vie", today)
            as SmartCaptureResult.HabitDraft
        assertEquals("Caminar", result.input.title)
        assertEquals(8000, result.input.target)
        assertEquals(HabitUnit.STEPS, result.input.unit)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), result.input.days)
    }

    @Test fun singleWeekdayCreatesDatedTask() {
        val result = SmartCaptureParser.parse("Entregar informe viernes a las 16 #trabajo", today)
            as SmartCaptureResult.TaskDraft
        assertEquals("Entregar informe", result.input.title)
        assertEquals(LocalDate.of(2026, 8, 21), result.input.dueDate)
        assertEquals(16, result.input.reminderHour)
        assertEquals(setOf("trabajo"), result.input.tags)
    }

    @Test fun recurringSingleWeekdayCreatesHabit() {
        val result = SmartCaptureParser.parse("Yoga cada martes por la mañana", today)
            as SmartCaptureResult.HabitDraft
        assertEquals("Yoga", result.input.title)
        assertEquals(setOf(DayOfWeek.TUESDAY), result.input.days)
        assertEquals(8, result.input.reminderHour)
    }

    @Test fun pluralWeekdayCreatesRecurringHabit() {
        val result = SmartCaptureParser.parse("Yoga los martes a las 19", today)
            as SmartCaptureResult.HabitDraft
        assertEquals("Yoga", result.input.title)
        assertEquals(setOf(DayOfWeek.TUESDAY), result.input.days)
        assertEquals(19, result.input.reminderHour)
    }

    @Test fun everyMorningCreatesDailyHabit() {
        val result = SmartCaptureParser.parse("Meditar cada mañana", today)
            as SmartCaptureResult.HabitDraft
        assertEquals("Meditar", result.input.title)
        assertEquals(DayOfWeek.entries.toSet(), result.input.days)
        assertEquals(8, result.input.reminderHour)
    }

    @Test fun understandsExplicitSpanishDate() {
        val result = SmartCaptureParser.parse("Renovar licencia el 20 de agosto importante", today)
            as SmartCaptureResult.TaskDraft
        assertEquals("Renovar licencia", result.input.title)
        assertEquals(LocalDate.of(2026, 8, 20), result.input.dueDate)
        assertTrue(result.input.important)
    }
}
