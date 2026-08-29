package com.trazo.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trazo.app.model.Habit
import com.trazo.app.model.HabitProgress
import com.trazo.app.model.Task
import com.trazo.app.model.TaskSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class CalendarMode(val label: String) { DAY("Día"), PLANNER("Planner"), MONTH("Mes") }
private val EsLocale = Locale.forLanguageTag("es-CL")

@Composable
internal fun CalendarScreen(
    tasks: List<Task>, habits: List<Habit>, padding: PaddingValues,
    onTaskToggle: (String) -> Unit, onHabitToggle: (String, LocalDate) -> Unit,
    onHabitExceptionToggle: (String, LocalDate) -> Unit,
    onSubtaskToggle: (String, String) -> Unit,
    onTaskReschedule: (String, LocalDate, Int, Int) -> Unit,
    onAddTask: (LocalDate) -> Unit
) {
    var mode by remember { mutableStateOf(CalendarMode.PLANNER) }
    val reducedMotion = LocalReducedMotion.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        CalendarTitle(selectedDate, mode, visibleMonth,
            onPrevious = {
                when (mode) {
                    CalendarMode.MONTH -> visibleMonth = visibleMonth.minusMonths(1)
                    CalendarMode.PLANNER -> selectedDate = selectedDate.minusWeeks(1)
                    CalendarMode.DAY -> selectedDate = selectedDate.minusDays(1)
                }
            },
            onNext = {
                when (mode) {
                    CalendarMode.MONTH -> visibleMonth = visibleMonth.plusMonths(1)
                    CalendarMode.PLANNER -> selectedDate = selectedDate.plusWeeks(1)
                    CalendarMode.DAY -> selectedDate = selectedDate.plusDays(1)
                }
            }
        )
        ModeSelector(mode) { mode = it }
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                fadeIn(tween(if (reducedMotion) 0 else 260)) togetherWith
                    fadeOut(tween(if (reducedMotion) 0 else 160))
            },
            label = "calendar mode"
        ) { currentMode ->
            when (currentMode) {
                CalendarMode.DAY -> DayAgenda(selectedDate, tasks, habits, padding, onTaskToggle, onHabitToggle, onHabitExceptionToggle, onSubtaskToggle, onTaskReschedule, onAddTask)
                CalendarMode.PLANNER -> WeekPlanner(selectedDate, tasks, habits, padding, onTaskToggle) {
                    selectedDate = it
                    visibleMonth = YearMonth.from(it)
                    mode = CalendarMode.DAY
                }
                CalendarMode.MONTH -> MonthGrid(visibleMonth, selectedDate, tasks, habits, padding) {
                    selectedDate = it
                    mode = CalendarMode.DAY
                }
            }
        }
    }
}

@Composable
private fun CalendarTitle(
    selected: LocalDate, mode: CalendarMode, month: YearMonth,
    onPrevious: () -> Unit, onNext: () -> Unit
) {
    val title = if (mode == CalendarMode.MONTH) month.format(DateTimeFormatter.ofPattern("MMMM yyyy", EsLocale))
    else selected.format(DateTimeFormatter.ofPattern("MMMM yyyy", EsLocale))
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("AGENDA VIVA", color = Coral, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(title.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineMedium)
        }
        MiniArrow(TrazoIconKind.CHEVRON_LEFT, "Anterior", onPrevious)
        MiniArrow(TrazoIconKind.CHEVRON_RIGHT, "Siguiente", onNext)
    }
}

@Composable
private fun MiniArrow(icon: TrazoIconKind, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { TrazoIcon(icon, color = Ink, size = 24.dp, description = description) }
}

@Composable
private fun ModeSelector(selected: CalendarMode, onSelect: (CalendarMode) -> Unit) {
    Row(
        Modifier.padding(horizontal = 24.dp, vertical = 6.dp).fillMaxWidth()
            .background(Ink.copy(alpha = .06f), RoundedCornerShape(18.dp)).padding(4.dp)
    ) {
        CalendarMode.entries.forEach { mode ->
            val active = mode == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (active) PaperRaised else Color.Transparent)
                    .clickable { onSelect(mode) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text(mode.label, color = if (active) Coral else MutedInk, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) }
        }
    }
}

@Composable
private fun DayAgenda(
    date: LocalDate, tasks: List<Task>, habits: List<Habit>, padding: PaddingValues,
    onTaskToggle: (String) -> Unit, onHabitToggle: (String, LocalDate) -> Unit,
    onHabitExceptionToggle: (String, LocalDate) -> Unit,
    onSubtaskToggle: (String, String) -> Unit,
    onTaskReschedule: (String, LocalDate, Int, Int) -> Unit,
    onAddTask: (LocalDate) -> Unit
) {
    val dayTasks = TaskSchedule.onDate(tasks, date).sortedBy { it.completed }
    val timedTasks = dayTasks.filter { !it.completed && it.reminderHour != null }
        .sortedWith(compareBy<Task> { it.reminderHour }.thenBy { it.reminderMinute })
    val untimedTasks = dayTasks.filter { it !in timedTasks }
    val dayHabits = habits.filter { HabitProgress.isBaseScheduled(it, date) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 96.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(date.format(DateTimeFormatter.ofPattern("EEEE", EsLocale)).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge)
                    Text(date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", EsLocale)), color = MutedInk)
                }
                Surface(onClick = { onAddTask(date) }, color = Coral, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        TrazoIcon(TrazoIconKind.ADD, color = Color.White, size = 17.dp)
                        Text("Tarea", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp))
                    }
                }
            }
            AgendaLabel("Tareas", dayTasks.count { it.completed }, dayTasks.size)
        }
        if (dayTasks.isEmpty()) item { CalendarEmpty("La página está libre", "Agrega algo para este día.") }
        if (timedTasks.isNotEmpty()) {
            item { TimedAgendaHeader(timedTasks.size) }
            items(timedTasks, key = { "timed-${it.id}" }) { task ->
                TimedTaskBlock(task, date, onTaskToggle, onSubtaskToggle, onTaskReschedule)
            }
            if (untimedTasks.isNotEmpty()) item { AgendaSubLabel("Sin hora asignada") }
        }
        items(untimedTasks, key = { it.id }) { task -> CalendarTaskRow(task, onTaskToggle, onSubtaskToggle) }
        item { AgendaLabel("Hábitos", dayHabits.count { HabitProgress.isComplete(it, date) }, dayHabits.size) }
        if (dayHabits.isEmpty()) item { CalendarEmpty("Sin rituales programados", "Este día puede respirar.") }
        items(dayHabits, key = { it.id }) { habit -> CalendarHabitRow(habit, date, onHabitToggle, onHabitExceptionToggle) }
    }
}

@Composable
private fun TimedAgendaHeader(count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrazoIcon(TrazoIconKind.SCHEDULE, color = Coral, size = 18.dp)
        Text("Agenda horaria", modifier = Modifier.padding(start = 7.dp), fontWeight = FontWeight.Bold, color = Coral)
        Spacer(Modifier.weight(1f))
        Text("$count bloque${if (count == 1) "" else "s"}", color = MutedInk, fontSize = 12.sp)
    }
}

@Composable
private fun AgendaSubLabel(label: String) {
    Text(label, color = MutedInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 3.dp))
}

@Composable
private fun TimedTaskBlock(
    task: Task,
    date: LocalDate,
    onToggle: (String) -> Unit,
    onSubtaskToggle: (String, String) -> Unit,
    onReschedule: (String, LocalDate, Int, Int) -> Unit
) {
    val hour = task.reminderHour ?: return
    val minute = task.reminderMinute
    val start = LocalTime.of(hour, minute)
    val end = start.plusMinutes(task.durationMinutes.toLong())
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(58.dp).padding(top = 13.dp)) {
            Text(start.format(DateTimeFormatter.ofPattern("HH:mm")), color = Coral, fontWeight = FontWeight.Bold)
            Text(end.format(DateTimeFormatter.ofPattern("HH:mm")), color = MutedInk, fontSize = 11.sp)
        }
        Column(Modifier.weight(1f)) {
            Surface(color = PaperRaised, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Row(Modifier.clickable { onToggle(task.id) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(task.completed, { onToggle(task.id) }, colors = CheckboxDefaults.colors(checkedColor = Leaf))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold)
                            Text("${task.durationMinutes} min · bloque programado", color = MutedInk, fontSize = 11.sp)
                        }
                    }
                    TaskChecklist(task, onSubtaskToggle, compact = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            val earlier = start.minusMinutes(15)
                            onReschedule(task.id, date, earlier.hour, earlier.minute)
                        }) { Text("−15 min", color = MutedInk, fontSize = 11.sp) }
                        TextButton(onClick = {
                            val later = start.plusMinutes(15)
                            onReschedule(task.id, date, later.hour, later.minute)
                        }) { Text("+15 min", color = Coral, fontSize = 11.sp) }
                        TextButton(onClick = { onReschedule(task.id, date.plusDays(1), hour, minute) }) {
                            Text("Mañana", color = Leaf, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaLabel(label: String, done: Int, total: Int) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.weight(1f))
        Text(if (total == 0) "—" else "$done / $total", color = MutedInk)
    }
}

@Composable
private fun CalendarTaskRow(task: Task, onToggle: (String) -> Unit, onSubtaskToggle: (String, String) -> Unit) {
    Surface(
        color = if (task.completed) Leaf.copy(alpha = .16f) else PaperRaised,
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp).fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.clickable { onToggle(task.id) }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(task.completed, { onToggle(task.id) }, colors = CheckboxDefaults.colors(checkedColor = Leaf))
                Column(Modifier.weight(1f)) {
                    Text(task.title, fontWeight = FontWeight.SemiBold, color = if (task.completed) MutedInk else Ink)
                    Text(
                        "≈ ${if (task.durationMinutes >= 60) "${task.durationMinutes / 60} h" else "${task.durationMinutes} min"}",
                        color = MutedInk, fontSize = 11.sp
                    )
                    if (task.note.isNotBlank()) Text(task.note, color = MutedInk, fontSize = 13.sp, maxLines = 1)
                }
            }
            TaskChecklist(task, onSubtaskToggle, compact = true)
        }
    }
}

@Composable
private fun CalendarHabitRow(
    habit: Habit,
    date: LocalDate,
    onToggle: (String, LocalDate) -> Unit,
    onExceptionToggle: (String, LocalDate) -> Unit
) {
    val done = HabitProgress.isComplete(habit, date)
    val skipped = date in habit.skippedDates
    Surface(
        color = if (done) Sky.copy(alpha = .20f) else PaperRaised,
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp).fillMaxWidth()
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(habit.emoji, fontSize = 22.sp)
            Text(habit.title, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { if (skipped) onExceptionToggle(habit.id, date) else onToggle(habit.id, date) }) {
                Text(if (skipped) "Restaurar" else if (done) "Hecho" else "Marcar", color = if (done) Leaf else Coral, fontWeight = FontWeight.Bold)
                if (done) TrazoIcon(TrazoIconKind.CHECK, color = Leaf, size = 15.dp, modifier = Modifier.padding(start = 5.dp))
            }
            TextButton(onClick = { onExceptionToggle(habit.id, date) }) {
                Text(if (skipped) "Omitido" else "Omitir", color = MutedInk, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun WeekPlanner(
    anchor: LocalDate, tasks: List<Task>, habits: List<Habit>, padding: PaddingValues,
    onTaskToggle: (String) -> Unit, onSelectDay: (LocalDate) -> Unit
) {
    val monday = anchor.minusDays((anchor.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val dates = (0L..6L).map(monday::plusDays)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 96.dp)) {
        items(dates, key = { it.toEpochDay() }) { date ->
            val dayTasks = TaskSchedule.onDate(tasks, date)
            val dueHabits = habits.count { HabitProgress.isScheduled(it, date) }
            PlannerDay(date, dayTasks, dueHabits, onTaskToggle) { onSelectDay(date) }
        }
    }
}

@Composable
private fun PlannerDay(
    date: LocalDate, tasks: List<Task>, habitCount: Int, onTaskToggle: (String) -> Unit, onOpen: () -> Unit
) {
    val today = date == LocalDate.now()
    val completeDot = Leaf
    val pendingDot = Coral
    Surface(
        color = if (today) Mustard.copy(alpha = .20f) else PaperRaised,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp).fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(if (today) Coral else Ink.copy(alpha = .07f)), contentAlignment = Alignment.Center) {
                    Text(date.dayOfMonth.toString(), color = if (today) Color.White else Ink, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(date.format(DateTimeFormatter.ofPattern("EEEE", EsLocale)).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                    Text("${tasks.size} tareas · $habitCount hábitos", color = MutedInk, fontSize = 13.sp)
                }
                TrazoIcon(TrazoIconKind.ARROW_RIGHT, color = Coral, size = 20.dp)
            }
            tasks.take(3).forEach { task ->
                Row(Modifier.fillMaxWidth().clickable { onTaskToggle(task.id) }.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(18.dp)) { drawCircle(if (task.completed) completeDot else pendingDot, radius = 4.dp.toPx()) }
                    Text(task.title, modifier = Modifier.padding(start = 8.dp), color = if (task.completed) MutedInk else Ink, maxLines = 1)
                    if (task.subtasks.isNotEmpty()) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${task.subtasks.count { it.completed }}/${task.subtasks.size}",
                            color = if (task.subtasks.all { it.completed }) Leaf else MutedInk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (tasks.size > 3) Text("+ ${tasks.size - 3} más", color = MutedInk, fontSize = 12.sp, modifier = Modifier.padding(start = 26.dp, top = 4.dp))
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth, selected: LocalDate, tasks: List<Task>, habits: List<Habit>,
    padding: PaddingValues, onSelect: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    val rows = cells.chunked(7)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = padding.calculateBottomPadding() + 96.dp)) {
        item {
            Row(Modifier.fillMaxWidth()) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, modifier = Modifier.weight(1f), color = MutedInk, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
            Spacer(Modifier.height(8.dp))
        }
        items(rows) { week ->
            Row(Modifier.fillMaxWidth()) {
                (week + List(7 - week.size) { null }).forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f).height(76.dp))
                    else MonthCell(
                        date, date == selected, date == LocalDate.now(),
                        tasks.count { it.dueDate == date },
                        habits.count { HabitProgress.isScheduled(it, date) },
                        Modifier.weight(1f), onSelect
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCell(date: LocalDate, selected: Boolean, today: Boolean, taskCount: Int, habitCount: Int, modifier: Modifier, onSelect: (LocalDate) -> Unit) {
    val taskDot = Coral
    val habitDot = Leaf
    Column(
        modifier.height(76.dp).padding(2.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) Coral.copy(alpha = .15f) else Color.Transparent)
            .clickable { onSelect(date) }.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(date.dayOfMonth.toString(), color = if (today) Coral else Ink, fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(7.dp))
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val total = (taskCount + habitCount).coerceAtMost(3)
            repeat(total) { index ->
                drawCircle(if (index < taskCount) taskDot else habitDot, radius = 3.dp.toPx(), center = Offset(size.width / 2 + (index - (total - 1) / 2f) * 10.dp.toPx(), center.y))
            }
        }
    }
}

@Composable
private fun CalendarEmpty(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 5.dp).fillMaxWidth().background(Sky.copy(alpha = .10f), RoundedCornerShape(14.dp)).padding(16.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MutedInk, fontSize = 13.sp)
    }
}
