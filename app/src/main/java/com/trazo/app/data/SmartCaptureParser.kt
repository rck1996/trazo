package com.trazo.app.data

import com.trazo.app.HabitInput
import com.trazo.app.TaskInput
import com.trazo.app.model.HabitCategory
import com.trazo.app.model.HabitUnit
import com.trazo.app.model.TaskRecurrence
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
        val explicitTask = Regex("\\b(?:tarea|pendiente|recordatorio)\\b").containsMatchIn(plain)
        val explicitHabit = Regex("\\b(?:habito|rutina)\\b").containsMatchIn(plain)
        val recurringPhrase = listOf(
            "todos los dias", "cada dia", "diario", "diaria", "habito", "rutina",
            "entre semana", "fin de semana", "fines de semana",
            "cada manana", "cada tarde", "cada noche"
        ).any(plain::contains) ||
            Regex("\\b(?:cada|los)\\s+(?:$weekdayPattern)\\b").containsMatchIn(plain) ||
            Regex("\\bcada\\s+(?:dos|tres|cuatro|\\d+)\\s+semanas?\\b").containsMatchIn(plain)
        // Minutes alone normally describe task effort ("estudiar 45 minutos mañana"),
        // while a cadence, several weekdays, or an explicit cue describes a habit.
        val stepGoalWithoutDate = target?.second == HabitUnit.STEPS && extractDate(plain, today) == null &&
            !Regex("\\b(?:manana|hoy|pasado manana|en\\s+\\d+\\s+dias?)\\b").containsMatchIn(plain)
        val isHabit = !explicitTask && (explicitHabit || recurringPhrase || days.size >= 2 || stepGoalWithoutDate)
        val time = extractTime(plain)
        val explicitDate = extractDate(plain, today)
        val dueDate = when {
            "pasado manana" in plain -> today.plusDays(2)
            Regex("\\bmanana\\b").containsMatchIn(plain) -> today.plusDays(1)
            Regex("\\bhoy\\b").containsMatchIn(plain) -> today
            extractRelativeDays(plain) != null -> today.plusDays(extractRelativeDays(plain)!!.toLong())
            explicitDate != null -> explicitDate
            !isHabit && days.size == 1 -> nextOccurrence(today, days.first(), plain)
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
                repeatEveryWeeks = extractWeekInterval(plain),
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
                dueDate = dueDate ?: if (time != null || extractTaskRecurrence(plain) != TaskRecurrence.NONE) today else null,
                durationMinutes = extractDurationMinutes(plain),
                recurrence = extractTaskRecurrence(plain),
                reminderHour = time?.hour,
                reminderMinute = time?.minute ?: 0,
                tags = tags
            ))
        }
    }

    private fun extractDurationMinutes(text: String): Int {
        val hoursAndMinutes = Regex(
            "\\b(\\d{1,2})\\s*(?:h|horas?)\\s*(?:y\\s*)?(\\d{1,2})\\s*(?:m|min(?:uto)?s?)\\b"
        ).find(text)
        val hoursAndHalf = Regex("\\b(\\d{1,2})\\s*(?:h|horas?)\\s+y\\s+media\\b").find(text)
        val minutes = when {
            hoursAndMinutes != null -> hoursAndMinutes.groupValues[1].toInt() * 60 + hoursAndMinutes.groupValues[2].toInt()
            hoursAndHalf != null -> hoursAndHalf.groupValues[1].toInt() * 60 + 30
            Regex("\\bmedia\\s+hora\\b").containsMatchIn(text) -> 30
            Regex("\\b(?:una|1)\\s+hora\\b").containsMatchIn(text) -> 60
            else -> Regex("\\b(\\d{1,3})\\s*(?:m|min(?:uto)?s?|h|horas?)\\b")
                .find(text)?.let { match ->
                    val amount = match.groupValues[1].toIntOrNull() ?: return@let null
                    if (Regex("(?:h|hora)").containsMatchIn(match.value)) amount * 60 else amount
                }
        }
        return (minutes ?: 25).coerceIn(5, 480)
    }

    private fun extractTaskRecurrence(text: String): TaskRecurrence = when {
        Regex("\\bcada\\s+d[ií]a\\b|\\bdiari[oa]\\b").containsMatchIn(text) -> TaskRecurrence.DAILY
        Regex("\\b(?:cada|todos?\\s+los?|los)\\s+(?:$weekdayPattern)\\b").containsMatchIn(text) -> TaskRecurrence.WEEKLY
        Regex("\\bcada\\s+semana\\b|\\bsemanal\\b").containsMatchIn(text) -> TaskRecurrence.WEEKLY
        Regex("\\bcada\\s+mes\\b|\\bmensual\\b").containsMatchIn(text) -> TaskRecurrence.MONTHLY
        else -> TaskRecurrence.NONE
    }

    private fun extractDays(text: String): Set<DayOfWeek> {
        val base = when {
            "todos los dias" in text -> DayOfWeek.entries.toSet()
            "entre semana" in text || Regex("\\b(?:de\\s+)?lunes\\s+a\\s+viernes\\b").containsMatchIn(text) ->
                DayOfWeek.entries.take(5).toSet()
            "fin de semana" in text || "fines de semana" in text ->
                setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            else -> dayNames.mapNotNull { (day, names) ->
                day.takeIf { Regex("\\b(?:$names)\\b").containsMatchIn(text) }
            }.toSet()
        }
        val excluded = dayNames.mapNotNull { (day, names) ->
            day.takeIf { Regex("\\bexcepto\\s+(?:el\\s+)?(?:$names)\\b").containsMatchIn(text) }
        }.toSet()
        return base - excluded
    }

    private fun extractWeekInterval(text: String): Int {
        val raw = Regex("\\bcada\\s+(dos|tres|cuatro|\\d+)\\s+semanas?\\b")
            .find(text)?.groupValues?.get(1) ?: return 1
        return when (raw) {
            "dos" -> 2
            "tres" -> 3
            "cuatro" -> 4
            else -> raw.toIntOrNull()?.coerceIn(1, 12) ?: 1
        }
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
        val wordClock = Regex(
            "\\b(?:a\\s+las?|alas)\\s*(una|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce)(?:\\s+y\\s+(media|cuarto))?\\b"
        ).find(text)
        if (wordClock != null) {
            val hour = mapOf(
                "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6,
                "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12
            ).getValue(wordClock.groupValues[1])
            val minute = when (wordClock.groupValues[2]) { "media" -> 30; "cuarto" -> 15; else -> 0 }
            val isAfternoon = Regex("\\b(?:de la tarde|de la noche)\\b").containsMatchIn(text)
            val isMorning = Regex("\\bde la manana\\b").containsMatchIn(text)
            val normalizedHour = when {
                isAfternoon && hour < 12 -> hour + 12
                isMorning && hour == 12 -> 0
                else -> hour
            }
            return LocalTime.of(normalizedHour, minute)
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

    private fun extractRelativeDays(text: String): Int? {
        val token = Regex("\\ben\\s+(\\d+|uno|una|dos|tres|cuatro|cinco|seis|siete)\\s+d[ií]as?\\b")
            .find(text)?.groupValues?.get(1) ?: return null
        return when (token) {
            "uno", "una" -> 1
            "dos" -> 2
            "tres" -> 3
            "cuatro" -> 4
            "cinco" -> 5
            "seis" -> 6
            "siete" -> 7
            else -> token.toIntOrNull()?.coerceIn(1, 365)
        }
    }

    private fun cleanTitle(original: String): String = original
            .replace(Regex("#([\\p{L}\\d_-]+)"), "")
            .replace(Regex("(?i)\\b(?:(?:por la|cada)\\s+(?:mañana|manana|tarde|noche))\\b"), "")
            .replace(Regex("(?i)\\bde la (?:mañana|manana|tarde|noche)\\b"), "")
            .replace(Regex("(?i)\\b(?:pasado\\s+mañana|pasado\\s+manana|hoy|mañana|manana|importante|urgente|prioridad alta|tarea|hábito|habito|rutina|diario|diaria)\\b"), "")
            .replace(Regex("(?i)\\b(?:todos los días|todos los dias|cada día|cada dia|entre semana|fines? de semana|(?:de\\s+)?lunes\\s+a\\s+viernes)\\b"), "")
            .replace(Regex("(?i)\\bcada\\s+(?:dos|tres|cuatro|\\d+)\\s+semanas?\\b"), "")
            .replace(Regex("(?i)\\ben\\s+(?:\\d+|uno|una|dos|tres|cuatro|cinco|seis|siete)\\s+d[ií]as?\\b"), "")
            .replace(Regex("(?i)\\bcada\\s+(?:día|dia|semana|mes)\\b"), "")
            .replace(Regex("(?i)\\b(?:semanal|mensual)\\b"), "")
            .replace(Regex("(?i)\\bexcepto\\s+(?:el\\s+)?(?:lunes|martes|miércoles|miercoles|jueves|viernes|sábado|sabado|domingo)\\b"), "")
            .replace(Regex("(?i)\\b(?:(?:cada|los|el|todos?\\s+los)\\s+)?(?:lunes|lun|martes|mar|miércoles|miercoles|mie|jueves|jue|viernes|vie|sábado|sabado|sab|domingo|dom)\\b"), "")
            .replace(Regex("(?i)\\b(?:a\\s+las?|alas)\\s*\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?\\b"), "")
            .replace(Regex("(?i)\\b(?:a\\s+las?|alas)\\s*(?:una|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce)(?:\\s+y\\s+(?:media|cuarto))?\\b"), "")
            .replace(Regex("(?i)\\b\\d{1,2}:\\d{2}\\s*(?:am|pm)?\\b"), "")
            .replace(Regex("(?i)\\b\\d{1,2}\\s*(?:h|hora(?:s)?)\\s*(?:y\\s*)?(?:\\d{1,2}\\s*(?:m|min(?:uto)?s?))?\\b"), "")
            .replace(Regex("(?i)\\b(?:una\\s+hora|media\\s+hora)\\b"), "")
            .replace(Regex("(?i)\\b\\d{1,5}\\s*(?:pasos?|minutos?|mins?|veces?)\\b"), "")
            .replace(Regex("(?i)\\b(?:el\\s+)?\\d{1,2}\\s+de\\s+(?:${months.keys.joinToString("|")})(?:\\s+de\\s+\\d{4})?\\b"), "")
            .replace(Regex("\\b\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{4})?\\b"), "")
            .replace(Regex("\\s*[,;]+\\s*"), " ")
            .replace(Regex("\\s+"), " ").trim().trim(',', '.', '-', ':')
            .replace(Regex("(?i)(?:[,;]\\s*)?\\by\\b$"), "").trim()

    private fun nextOccurrence(today: LocalDate, day: DayOfWeek, text: String): LocalDate {
        val delta = (day.value - today.dayOfWeek.value + 7) % 7
        val forceNextWeek = Regex("\\b(?:proximo|siguiente)\\s+(?:$weekdayPattern)\\b").containsMatchIn(text)
        return today.plusDays((if (forceNextWeek && delta == 0) 7 else delta).toLong())
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
}
