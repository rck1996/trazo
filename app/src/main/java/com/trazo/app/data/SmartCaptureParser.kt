package com.trazo.app.data

import com.trazo.app.HabitInput
import com.trazo.app.TaskInput
import com.trazo.app.model.HabitCategory
import com.trazo.app.model.HabitUnit
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month

sealed interface SmartCaptureResult {
    data class TaskDraft(val input: TaskInput) : SmartCaptureResult
    data class HabitDraft(val input: HabitInput) : SmartCaptureResult
}

object SmartCaptureParser {
    private val dayNames = linkedMapOf(
        DayOfWeek.MONDAY to "lunes|lun",
        DayOfWeek.TUESDAY to "martes|mar",
        DayOfWeek.WEDNESDAY to "miercoles|mie",
        DayOfWeek.THURSDAY to "jueves|jue",
        DayOfWeek.FRIDAY to "viernes|vie",
        DayOfWeek.SATURDAY to "sabado|sab",
        DayOfWeek.SUNDAY to "domingo|dom"
    )
    private val weekdayPattern = dayNames.values.joinToString("|")
    private val months = mapOf(
        "enero" to Month.JANUARY, "febrero" to Month.FEBRUARY, "marzo" to Month.MARCH,
        "abril" to Month.APRIL, "mayo" to Month.MAY, "junio" to Month.JUNE,
        "julio" to Month.JULY, "agosto" to Month.AUGUST, "septiembre" to Month.SEPTEMBER,
        "setiembre" to Month.SEPTEMBER, "octubre" to Month.OCTOBER,
        "noviembre" to Month.NOVEMBER, "diciembre" to Month.DECEMBER
    )

    fun parse(raw: String, today: LocalDate = LocalDate.now()): SmartCaptureResult {
        val original = raw.trim().replace(Regex("\\s+"), " ")
        val plain = normalize(original)
        val days = extractDays(plain)
        val target = extractTarget(plain)
        val recurringPhrase = listOf(
            "todos los dias", "cada dia", "diario", "diaria", "habito", "rutina",
            "entre semana", "fin de semana", "fines de semana",
            "cada manana", "cada tarde", "cada noche"
        ).any(plain::contains) || Regex("\\b(?:cada|los)\\s+(?:$weekdayPattern)\\b").containsMatchIn(plain)
        val isHabit = "tarea" !in plain && (recurringPhrase || days.size >= 2 || target != null)
        val time = extractTime(plain)
        val explicitDate = extractDate(plain, today)
        val dueDate = when {
            "pasado manana" in plain -> today.plusDays(2)
            Regex("\\bmanana\\b").containsMatchIn(plain) -> today.plusDays(1)
            Regex("\\bhoy\\b").containsMatchIn(plain) -> today
            explicitDate != null -> explicitDate
            !isHabit && days.size == 1 -> nextOrSame(today, days.first())
            else -> null
        }
        val tags = Regex("#([\\p{L}\\d_-]+)").findAll(original).map { it.groupValues[1].lowercase() }.toSet()
        val cleanTitle = cleanTitle(original)
            .ifBlank { if (isHabit) "Nuevo hábito" else "Nueva tarea" }
        val important = listOf("importante", "urgente", "prioridad alta").any(plain::contains)
        return if (isHabit) {
            val category = HabitCategory.infer(cleanTitle)
            SmartCaptureResult.HabitDraft(HabitInput(
                title = cleanTitle,
                emoji = category.symbol,
                category = category,
                days = if (days.isEmpty()) DayOfWeek.entries.toSet() else days,
                target = target?.first ?: 1,
                unit = target?.second ?: HabitUnit.CHECK,
                reminderHour = time?.hour,
                reminderMinute = time?.minute ?: 0,
                tags = tags
            ))
        } else {
            SmartCaptureResult.TaskDraft(TaskInput(
                title = cleanTitle,
                important = important,
                dueDate = dueDate ?: if (time != null) today else null,
                reminderHour = time?.hour,
                reminderMinute = time?.minute ?: 0,
                tags = tags
            ))
        }
    }

    private fun extractDays(text: String): Set<DayOfWeek> {
        if ("todos los dias" in text) return DayOfWeek.entries.toSet()
        if ("entre semana" in text || Regex("\\b(?:de\\s+)?lunes\\s+a\\s+viernes\\b").containsMatchIn(text)) {
            return DayOfWeek.entries.take(5).toSet()
        }
        if ("fin de semana" in text || "fines de semana" in text) {
            return setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        }
        return dayNames.mapNotNull { (day, names) ->
            day.takeIf { Regex("\\b(?:$names)\\b").containsMatchIn(text) }
        }.toSet()
    }

    private fun extractTarget(text: String): Pair<Int, HabitUnit>? {
        val match = Regex("\\b(\\d{1,5})\\s*(pasos?|minutos?|mins?|veces?)\\b").find(text) ?: return null
        val amount = match.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: return null
        val unit = when {
            match.groupValues[2].startsWith("paso") -> HabitUnit.STEPS
            match.groupValues[2].startsWith("min") -> HabitUnit.MINUTES
            else -> HabitUnit.TIMES
        }
        return amount to unit
    }

    private fun extractTime(text: String): LocalTime? {
        val clock = Regex("\\b(?:a\\s+las?|alas)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b").find(text)
            ?: Regex("\\b(\\d{1,2}):(\\d{2})\\s*(am|pm)?\\b").find(text)
        if (clock != null) {
            var hour = clock.groupValues[1].toIntOrNull() ?: return null
            val minute = clock.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: return null
            when (clock.groupValues[3]) {
                "pm" -> if (hour < 12) hour += 12
                "am" -> if (hour == 12) hour = 0
            }
            return runCatching { LocalTime.of(hour, minute) }.getOrNull()
        }
        return when {
            "por la manana" in text || "cada manana" in text -> LocalTime.of(8, 0)
            "por la tarde" in text || "cada tarde" in text -> LocalTime.of(15, 0)
            "por la noche" in text || "cada noche" in text -> LocalTime.of(21, 0)
            else -> null
        }
    }

    private fun extractDate(text: String, today: LocalDate): LocalDate? {
        Regex("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{4}))?\\b").find(text)?.let { match ->
            val year = match.groupValues[3].toIntOrNull() ?: today.year
            val candidate = runCatching { LocalDate.of(year, match.groupValues[2].toInt(), match.groupValues[1].toInt()) }.getOrNull()
                ?: return@let
            return if (match.groupValues[3].isBlank() && candidate.isBefore(today)) candidate.plusYears(1) else candidate
        }
        val names = months.keys.joinToString("|")
        Regex("\\b(?:el\\s+)?(\\d{1,2})\\s+de\\s+($names)(?:\\s+de\\s+(\\d{4}))?\\b").find(text)?.let { match ->
            val year = match.groupValues[3].toIntOrNull() ?: today.year
            val candidate = runCatching { LocalDate.of(year, months.getValue(match.groupValues[2]), match.groupValues[1].toInt()) }.getOrNull()
                ?: return@let
            return if (match.groupValues[3].isBlank() && candidate.isBefore(today)) candidate.plusYears(1) else candidate
        }
        return null
    }

    private fun cleanTitle(original: String): String = original
            .replace(Regex("#([\\p{L}\\d_-]+)"), "")
            .replace(Regex("(?i)\\b(?:(?:por la|cada)\\s+(?:mañana|manana|tarde|noche))\\b"), "")
            .replace(Regex("(?i)\\b(?:pasado\\s+mañana|pasado\\s+manana|hoy|mañana|manana|importante|urgente|prioridad alta|tarea|hábito|habito|rutina|diario|diaria)\\b"), "")
            .replace(Regex("(?i)\\b(?:todos los días|todos los dias|cada día|cada dia|entre semana|fines? de semana|(?:de\\s+)?lunes\\s+a\\s+viernes)\\b"), "")
            .replace(Regex("(?i)\\b(?:(?:cada|los|el)\\s+)?(?:lunes|lun|martes|mar|miércoles|miercoles|mie|jueves|jue|viernes|vie|sábado|sabado|sab|domingo|dom)\\b"), "")
            .replace(Regex("(?i)\\b(?:a\\s+las?|alas)\\s*\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?\\b"), "")
            .replace(Regex("(?i)\\b\\d{1,2}:\\d{2}\\s*(?:am|pm)?\\b"), "")
            .replace(Regex("(?i)\\b\\d{1,5}\\s*(?:pasos?|minutos?|mins?|veces?)\\b"), "")
            .replace(Regex("(?i)\\b(?:el\\s+)?\\d{1,2}\\s+de\\s+(?:${months.keys.joinToString("|")})(?:\\s+de\\s+\\d{4})?\\b"), "")
            .replace(Regex("\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{4})?\\b"), "")
            .replace(Regex("\\s*[,;]+\\s*"), " ")
            .replace(Regex("\\s+"), " ").trim().trim(',', '.', '-', ':')
            .replace(Regex("(?i)(?:[,;]\\s*)?\\by\\b$"), "").trim()

    private fun nextOrSame(today: LocalDate, day: DayOfWeek): LocalDate {
        val delta = (day.value - today.dayOfWeek.value + 7) % 7
        return today.plusDays(delta.toLong())
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
}
