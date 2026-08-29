package com.trazo.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.launch

private enum class CalendarMode(val label: String) { DAY("Agenda diaria"), PLANNER("Semana"), MONTH("Mes") }
private val EsLocale = Locale.forLanguageTag("es-CL")

@Composable
internal fun CalendarScreen(
    tasks: List<Task>, habits: List<Habit>, padding: PaddingValues,
    onTaskToggle: (String) -> Unit, onHabitToggle: (String, LocalDate) -> Unit,
    onHabitExceptionToggle: (String, LocalDate) -> Unit,
    onSubtaskToggle: (String, String) -> Unit,
    onTaskReschedule: (String, LocalDate, Int, Int) -> Unit,
    onEditTask: (Task) -> Unit,
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
        CalendarGuide(mode)
        if (mode == CalendarMode.PLANNER) CalendarContextAdd(selectedDate, onAddTask)
        if (mode != CalendarMode.MONTH) {
            PlanningSummary(selectedDate, tasks)
            UnscheduledTray(tasks.filter { !it.completed && it.dueDate == null }, selectedDate, onTaskReschedule, onEditTask)
        }
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = mode,
            transitionSpec = {
                fadeIn(tween(if (reducedMotion) 0 else 260)) togetherWith
                    fadeOut(tween(if (reducedMotion) 0 else 160))
            },
            label = "calendar mode"
        ) { currentMode ->
            when (currentMode) {
                CalendarMode.DAY -> DayAgenda(
                    selectedDate, tasks, habits, padding, onTaskToggle, onHabitToggle,
                    onHabitExceptionToggle, onSubtaskToggle,
                    { id, newDate, hour, minute ->
                        onTaskReschedule(id, newDate, hour, minute)
                        if (newDate != selectedDate) {
                            selectedDate = newDate
                            visibleMonth = YearMonth.from(newDate)
                        }
                    },
                    onEditTask, onAddTask
                )
                CalendarMode.PLANNER -> WeekPlanner(selectedDate, tasks, habits, padding, onTaskToggle, onEditTask) {
                    selectedDate = it
                    visibleMonth = YearMonth.from(it)
                    mode = CalendarMode.DAY
                }
                CalendarMode.MONTH -> MonthGrid(
                    visibleMonth, selectedDate, tasks, habits, padding,
                    onSelect = { selectedDate = it },
                    onOpenDay = { mode = CalendarMode.DAY },
                    onAddTask = { onAddTask(selectedDate) }
                )
            }
        }
    }
}

@Composable
private fun CalendarContextAdd(date: LocalDate, onAddTask: (LocalDate) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(date.format(DateTimeFormatter.ofPattern("EEE d MMM", EsLocale)).replaceFirstChar { it.uppercase() }, color = MutedInk, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Surface(onClick = { onAddTask(date) }, color = Coral.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
            Text("＋ Nueva tarea", color = Coral, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }
}

@Composable
private fun PlanningSummary(date: LocalDate, tasks: List<Task>) {
    val minutes = CalendarInsights.plannedMinutes(tasks, date)
    val conflicts = CalendarInsights.conflictCount(tasks, date)
    val largestFree = CalendarInsights.freeWindows(tasks, date).maxOfOrNull { it.durationMinutes } ?: 0
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PlanningMetric("${minutes / 60}h ${minutes % 60}m", "planificado", Modifier.weight(1f))
        PlanningMetric(if (largestFree == 0) "—" else "${largestFree / 60}h ${largestFree % 60}m", "mayor hueco", Modifier.weight(1f))
        PlanningMetric(conflicts.toString(), if (conflicts == 1) "conflicto" else "conflictos", Modifier.weight(1f), conflicts > 0)
    }
}

@Composable
private fun PlanningMetric(value: String, label: String, modifier: Modifier, warning: Boolean = false) {
    Surface(color = if (warning) Coral.copy(alpha = .13f) else Ink.copy(alpha = .045f), shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = if (warning) Coral else Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(label, color = MutedInk, fontSize = 9.sp)
        }
    }
}

@Composable
private fun UnscheduledTray(
    tasks: List<Task>, date: LocalDate,
    onSchedule: (String, LocalDate, Int, Int) -> Unit,
    onEdit: (Task) -> Unit
) {
    if (tasks.isEmpty()) return
    val suggestedHour = if (date == LocalDate.now()) (LocalTime.now().hour + 1).coerceAtMost(20) else 9
    Column(Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 5.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("SIN PROGRAMAR", color = Coral, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text("${tasks.size} para ubicar", color = MutedInk, fontSize = 10.sp)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tasks.take(8).forEach { task ->
                Surface(color = Mustard.copy(alpha = .16f), shape = RoundedCornerShape(11.dp)) {
                    Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(task.title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.clickable { onEdit(task) }.padding(vertical = 8.dp))
                        TextButton(onClick = { onSchedule(task.id, date, suggestedHour, 0) }) {
                            Text("Ubicar ${"%02d:00".format(suggestedHour)}", color = Coral, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGuide(mode: CalendarMode) {
    val message = when (mode) {
        CalendarMode.DAY -> "Bloques por hora. Mantén pulsada una tarea y arrástrala para cambiar día u hora."
        CalendarMode.PLANNER -> "Tu semana completa. Toca un día para abrir su agenda horaria."
        CalendarMode.MONTH -> "Calendario mensual. Toca una fecha para ver tareas, hábitos y horas."
    }
    Surface(
        color = Sky.copy(alpha = .11f),
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            TrazoIcon(TrazoIconKind.CALENDAR, color = Coral, size = 18.dp)
            Text(message, color = MutedInk, fontSize = 11.sp, modifier = Modifier.padding(start = 9.dp))
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
    onEditTask: (Task) -> Unit,
    onAddTask: (LocalDate) -> Unit
) {
    val dayTasks = TaskSchedule.onDate(tasks, date).sortedBy { it.completed }
    val timedTasks = dayTasks.filter { !it.completed && it.reminderHour != null }
        .sortedWith(compareBy<Task> { it.reminderHour }.thenBy { it.reminderMinute })
    val untimedTasks = dayTasks.filter { it !in timedTasks }
    val dayHabits = habits.filter { HabitProgress.isBaseScheduled(it, date) }
    val conflictIds = CalendarInsights.conflictingTaskIds(tasks, date)
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
            item { TimedAgendaHeader(timedTasks.size, conflictIds.size) }
            items(timedTasks, key = { "timed-${it.id}" }) { task ->
                TimedTaskBlock(task, date, task.id in conflictIds, onTaskToggle, onSubtaskToggle, onEditTask, onTaskReschedule)
            }
            if (untimedTasks.isNotEmpty()) item { AgendaSubLabel("Sin hora asignada") }
        }
        items(untimedTasks, key = { it.id }) { task -> CalendarTaskRow(task, onTaskToggle, onSubtaskToggle, onEditTask) }
        item { AgendaLabel("Hábitos", dayHabits.count { HabitProgress.isComplete(it, date) }, dayHabits.size) }
        if (dayHabits.isEmpty()) item { CalendarEmpty("Sin rituales programados", "Este día puede respirar.") }
        items(dayHabits, key = { it.id }) { habit -> CalendarHabitRow(habit, date, onHabitToggle, onHabitExceptionToggle) }
    }
}

@Composable
private fun TimedAgendaHeader(count: Int, conflictingTasks: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrazoIcon(TrazoIconKind.SCHEDULE, color = Coral, size = 18.dp)
        Text("Agenda horaria", modifier = Modifier.padding(start = 7.dp), fontWeight = FontWeight.Bold, color = Coral)
        Spacer(Modifier.weight(1f))
        if (conflictingTasks > 0) {
            Surface(color = Coral.copy(alpha = .13f), shape = RoundedCornerShape(9.dp)) {
                Text("⚠ $conflictingTasks se cruzan", color = Coral, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        } else Text("$count bloque${if (count == 1) "" else "s"}", color = MutedInk, fontSize = 12.sp)
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
    conflict: Boolean,
    onToggle: (String) -> Unit,
    onSubtaskToggle: (String, String) -> Unit,
    onEdit: (Task) -> Unit,
    onReschedule: (String, LocalDate, Int, Int) -> Unit
) {
    val hour = task.reminderHour ?: return
    val minute = task.reminderMinute
    val start = LocalTime.of(hour, minute)
    val end = start.plusMinutes(task.durationMinutes.toLong())
    val density = LocalDensity.current
    val dragOffset = remember(task.id, date, hour, minute) { mutableStateOf(Offset.Zero) }
    var dragging by remember(task.id) { mutableStateOf(false) }
    var expanded by rememberSaveable(task.id) { mutableStateOf(false) }
    val pixelsPerMinute = with(density) { 1.2.dp.toPx() }
    // A short horizontal gesture is enough to cross one day; the previous
    // card-width threshold made this practically unreachable on phones.
    val pixelsPerDay = with(density) { 96.dp.toPx() }
    val drop = plannerDrop(date, hour, minute, dragOffset.value.x, dragOffset.value.y, pixelsPerDay, pixelsPerMinute)
    val previewStart = LocalTime.of(drop.hour, drop.minute)
    val previewEnd = previewStart.plusMinutes(task.durationMinutes.toLong())
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 5.dp)
            .zIndex(if (dragging) 2f else 0f)
            .graphicsLayer {
                translationX = if (dragging) dragOffset.value.x * .55f else 0f
                translationY = if (dragging) dragOffset.value.y * .55f else 0f
                alpha = if (dragging) .88f else 1f
            }
            .pointerInput(task.id, date, hour, minute, pixelsPerDay, pixelsPerMinute) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragging = true },
                    onDragCancel = { dragging = false; dragOffset.value = Offset.Zero },
                    onDragEnd = {
                        // Calculate from the final gesture here. Capturing `drop`
                        // from composition could use the initial Offset.Zero.
                        val finalDrop = plannerDrop(
                            date, hour, minute,
                            dragOffset.value.x, dragOffset.value.y,
                            pixelsPerDay, pixelsPerMinute
                        )
                        if (finalDrop.date != date || finalDrop.hour != hour || finalDrop.minute != minute) {
                            onReschedule(task.id, finalDrop.date, finalDrop.hour, finalDrop.minute)
                        }
                        dragging = false
                        dragOffset.value = Offset.Zero
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragOffset.value += amount
                    }
                )
            },
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.width(64.dp).padding(top = 13.dp)) {
            val shownStart = if (dragging) previewStart else start
            val shownEnd = if (dragging) previewEnd else end
            Text(shownStart.format(DateTimeFormatter.ofPattern("HH:mm")), color = Coral, fontWeight = FontWeight.Bold)
            Text("hasta", color = MutedInk, fontSize = 9.sp)
            Text(shownEnd.format(DateTimeFormatter.ofPattern("HH:mm")), color = MutedInk, fontSize = 11.sp)
        }
        Column(Modifier.weight(1f)) {
            Surface(
                color = if (dragging) Mustard.copy(alpha = .26f) else PaperRaised,
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth()
                    .then(if (conflict) Modifier.border(1.5.dp, Coral.copy(alpha = .72f), RoundedCornerShape(13.dp)) else Modifier)
                    .heightIn(min = if (expanded) 150.dp else 100.dp)
            ) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(task.completed, { onToggle(task.id) }, colors = CheckboxDefaults.colors(checkedColor = Leaf))
                        Column(Modifier.weight(1f).clickable { onEdit(task) }) {
                            Text(task.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text(
                                if (dragging) {
                                    val dayLabel = drop.date.format(DateTimeFormatter.ofPattern("EEE d", EsLocale))
                                    "$dayLabel · ${previewStart.format(DateTimeFormatter.ofPattern("HH:mm"))}–${previewEnd.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                                } else "${task.durationMinutes} min · mantén pulsado para mover",
                                color = if (dragging) Coral else MutedInk,
                                fontSize = 11.sp,
                                fontWeight = if (dragging) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)) {
                            Text(if (expanded) "Menos" else "Más", color = Coral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (conflict) Text("⚠ Conflicto de horario", color = Coral, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp, top = 2.dp))
                    if (task.subtasks.isNotEmpty() && !expanded) {
                        val done = task.subtasks.count { it.completed }
                        Text("↳ $done/${task.subtasks.size} pasos", color = MutedInk, fontSize = 10.sp, modifier = Modifier.padding(start = 48.dp, top = 3.dp))
                    }
                    if (expanded) {
                        TimedTaskProgress(task, onSubtaskToggle)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                val earlier = start.minusMinutes(15)
                                onReschedule(task.id, date, earlier.hour, earlier.minute)
                            }) { Text("−15", color = MutedInk, fontSize = 11.sp) }
                            TextButton(onClick = {
                                val later = start.plusMinutes(15)
                                onReschedule(task.id, date, later.hour, later.minute)
                            }) { Text("+15", color = Coral, fontSize = 11.sp) }
                            TextButton(onClick = { onReschedule(task.id, date.plusDays(1), hour, minute) }) {
                                Text("Mañana", color = Leaf, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimedTaskProgress(task: Task, onSubtaskToggle: (String, String) -> Unit) {
    if (task.subtasks.isEmpty()) return
    val completed = task.subtasks.count { it.completed }
    val firstPending = task.subtasks.firstOrNull { !it.completed }
    val dependency = firstPending?.dependsOnId?.let { id -> task.subtasks.firstOrNull { it.id == id } }
    val blocked = dependency != null && !dependency.completed
    Column(Modifier.fillMaxWidth().padding(start = 48.dp, top = 3.dp, end = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$completed / ${task.subtasks.size} pasos", color = if (completed == task.subtasks.size) Leaf else MutedInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (firstPending != null) {
                TextButton(
                    onClick = { onSubtaskToggle(task.id, firstPending.id) },
                    enabled = !blocked,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(if (blocked) "Bloqueado" else "Completar paso", color = if (blocked) MutedInk else Coral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (firstPending != null) {
            Text(
                if (blocked) "🔒 ${firstPending.title} · espera «${dependency?.title}»" else "→ ${firstPending.title}",
                color = if (blocked) Coral else Ink,
                fontSize = 11.sp,
                maxLines = 1
            )
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
private fun CalendarTaskRow(task: Task, onToggle: (String) -> Unit, onSubtaskToggle: (String, String) -> Unit, onEdit: (Task) -> Unit) {
    Surface(
        color = if (task.completed) Leaf.copy(alpha = .16f) else PaperRaised,
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp).fillMaxWidth()
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.clickable { onEdit(task) }, verticalAlignment = Alignment.CenterVertically) {
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
    onTaskToggle: (String) -> Unit, onEditTask: (Task) -> Unit, onSelectDay: (LocalDate) -> Unit
) {
    val monday = anchor.minusDays((anchor.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val dates = (0L..6L).map(monday::plusDays)
    val hours = 6..22
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dayWidth = 124
    val dayScroll = rememberScrollState(
        initial = with(density) { ((anchor.dayOfWeek.value - 1).coerceAtLeast(0) * dayWidth).dp.roundToPx() }
    )
    val contextualHour = if (LocalDate.now() in dates) (LocalTime.now().hour - 1).coerceIn(6, 20) else 8
    val timeScroll = rememberScrollState(
        initial = with(density) { ((contextualHour - 6) * 76).dp.roundToPx() }
    )
    val firstConflict = dates.firstNotNullOfOrNull { date ->
        CalendarInsights.firstConflictStart(tasks, date)?.let { date to it }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Desliza horizontalmente para recorrer la semana. Toca un bloque para editarlo.", color = MutedInk, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp))
        if (firstConflict != null) {
            val (conflictDate, conflictMinute) = firstConflict
            Surface(
                onClick = {
                    scope.launch {
                        dayScroll.animateScrollTo(with(density) { (dates.indexOf(conflictDate) * dayWidth).dp.roundToPx() })
                        timeScroll.animateScrollTo(with(density) { (((conflictMinute / 60) - 6).coerceAtLeast(0) * 76).dp.roundToPx() })
                    }
                },
                color = Coral.copy(alpha = .13f), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 3.dp).fillMaxWidth()
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠ Primer conflicto", color = Coral, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${conflictDate.format(DateTimeFormatter.ofPattern("EEE d", EsLocale))} · %02d:%02d  Ver →".format(conflictMinute / 60, conflictMinute % 60), color = Coral, fontSize = 10.sp)
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(timeScroll)
                .padding(bottom = padding.calculateBottomPadding() + 96.dp)
        ) {
            Row(Modifier.horizontalScroll(dayScroll).padding(horizontal = 12.dp)) {
                Column(Modifier.width(48.dp)) {
                    Spacer(Modifier.height(58.dp))
                    hours.forEach { hour ->
                        Text("%02d".format(hour), color = MutedInk, fontSize = 10.sp, modifier = Modifier.height(76.dp).padding(top = 5.dp))
                    }
                }
                dates.forEach { date ->
                    WeekDayColumn(date, tasks, habits, hours, dayWidth, onTaskToggle, onEditTask, onSelectDay)
                }
            }
        }
    }
}

@Composable
private fun WeekDayColumn(
    date: LocalDate, tasks: List<Task>, habits: List<Habit>, hours: IntRange,
    dayWidth: Int,
    onTaskToggle: (String) -> Unit, onEditTask: (Task) -> Unit, onSelectDay: (LocalDate) -> Unit
) {
    val dayTasks = TaskSchedule.onDate(tasks, date).filter { !it.completed }
    val timed = dayTasks.filter { it.reminderHour != null }
    val conflicts = CalendarInsights.conflictCount(tasks, date)
    val conflictIds = CalendarInsights.conflictingTaskIds(tasks, date)
    val gridLine = Ink.copy(alpha = .08f)
    Column(Modifier.width(dayWidth.dp).padding(end = 5.dp)) {
        Surface(
            onClick = { onSelectDay(date) },
            color = if (date == LocalDate.now()) Coral.copy(alpha = .16f) else Ink.copy(alpha = .045f),
            shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Column(Modifier.padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.format(DateTimeFormatter.ofPattern("EEE d", EsLocale)).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("${dayTasks.size} tareas · ${habits.count { HabitProgress.isScheduled(it, date) }} hábitos", color = if (conflicts > 0) Coral else MutedInk, fontSize = 8.sp)
            }
        }
        hours.forEach { hour ->
            val starting = timed.filter { it.reminderHour == hour }.sortedBy { it.reminderMinute }
            Box(
                Modifier.fillMaxWidth().height(76.dp).padding(top = 2.dp)
                    .background(Ink.copy(alpha = .028f), RoundedCornerShape(7.dp))
                    .clickable { onSelectDay(date) }
            ) {
                if (starting.isEmpty()) {
                    Text(
                        "${date.format(DateTimeFormatter.ofPattern("EEE d", EsLocale))} · ${"%02d:00".format(hour)}",
                        color = MutedInk.copy(alpha = .62f), fontSize = 8.sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
                    )
                    Canvas(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)) { drawLine(gridLine, Offset.Zero, Offset(size.width, 0f), 1f) }
                } else Column(Modifier.padding(4.dp)) {
                    starting.take(2).forEach { task ->
                        Surface(
                            onClick = { onEditTask(task) },
                            color = if (task.id in conflictIds) Coral.copy(alpha = .20f) else Sky.copy(alpha = .18f),
                            shape = RoundedCornerShape(7.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                        ) {
                            if (starting.size > 1) {
                                Row(Modifier.padding(horizontal = 4.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(14.dp).border(1.5.dp, MutedInk, RoundedCornerShape(3.dp))
                                            .clickable { onTaskToggle(task.id) }
                                    )
                                    Text(
                                        "%02d:%02d · %s".format(hour, task.reminderMinute, task.title),
                                        modifier = Modifier.padding(start = 4.dp).weight(1f),
                                        fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1
                                    )
                                }
                            } else {
                                Row(Modifier.padding(horizontal = 5.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(task.completed, { onTaskToggle(task.id) }, modifier = Modifier.size(22.dp), colors = CheckboxDefaults.colors(checkedColor = Leaf))
                                    Column(Modifier.padding(start = 3.dp)) {
                                        Text("%02d:%02d · %s".format(hour, task.reminderMinute, task.title), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                                        Text("${task.durationMinutes} min", color = MutedInk, fontSize = 8.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (dayTasks.any { it.reminderHour == null }) Text("+ ${dayTasks.count { it.reminderHour == null }} sin hora", color = Coral, fontSize = 9.sp, modifier = Modifier.padding(5.dp).clickable { onSelectDay(date) })
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth, selected: LocalDate, tasks: List<Task>, habits: List<Habit>,
    padding: PaddingValues, onSelect: (LocalDate) -> Unit, onOpenDay: () -> Unit,
    onAddTask: () -> Unit
) {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    val rows = cells.chunked(7)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = padding.calculateBottomPadding() + 96.dp)) {
        item {
            MonthLegend()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { Text(it, modifier = Modifier.weight(1f), color = MutedInk, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
            Spacer(Modifier.height(8.dp))
        }
        items(rows) { week ->
            Row(Modifier.fillMaxWidth()) {
                (week + List(7 - week.size) { null }).forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f).height(56.dp))
                    else {
                        val dayTasks = TaskSchedule.onDate(tasks, date)
                        val dayHabits = habits.filter { HabitProgress.isScheduled(it, date) }
                        MonthCell(
                            date = date,
                            selected = date == selected,
                            today = date == LocalDate.now(),
                            tasks = dayTasks,
                            habits = dayHabits,
                            modifier = Modifier.weight(1f),
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            MonthDaySummary(selected, tasks, habits, onOpenDay, onAddTask)
        }
    }
}

@Composable
private fun MonthLegend() {
    Row(
        Modifier.fillMaxWidth().background(Ink.copy(alpha = .04f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CARGA POR TIEMPO", color = MutedInk, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
        Spacer(Modifier.weight(1f))
        Text("● Tareas", color = Coral, fontSize = 10.sp)
        Text("● Hábitos", color = Leaf, fontSize = 10.sp)
    }
}

@Composable
private fun MonthCell(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    tasks: List<Task>,
    habits: List<Habit>,
    modifier: Modifier,
    onSelect: (LocalDate) -> Unit
) {
    val workloadMinutes = CalendarInsights.workloadMinutes(tasks, date)
    val loadAlpha = when {
        workloadMinutes >= 240 -> .24f
        workloadMinutes >= 120 -> .17f
        workloadMinutes >= 45 -> .10f
        workloadMinutes > 0 -> .05f
        else -> 0f
    }
    val subtaskTotal = tasks.sumOf { it.subtasks.size }
    val subtaskDone = tasks.sumOf { task -> task.subtasks.count { it.completed } }
    val completedTasks = tasks.count { it.completed }
    val completedHabits = habits.count { HabitProgress.isComplete(it, date) }
    Column(
        modifier.height(56.dp).padding(2.dp).clip(RoundedCornerShape(11.dp))
            .background(if (loadAlpha > 0f) Coral.copy(alpha = loadAlpha) else Color.Transparent)
            .then(if (selected) Modifier.border(2.dp, Coral, RoundedCornerShape(11.dp)) else Modifier)
            .clickable { onSelect(date) }.padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(date.dayOfMonth.toString(), color = if (today) Coral else Ink, fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(1.dp))
        if (tasks.isNotEmpty() || habits.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (tasks.isNotEmpty()) Text("$completedTasks/${tasks.size}", color = if (completedTasks == tasks.size) Leaf else Ink, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                if (habits.isNotEmpty()) Text("●$completedHabits/${habits.size}", color = Leaf, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
            }
            if (subtaskTotal > 0) Text("↳ $subtaskDone/$subtaskTotal", color = MutedInk, fontSize = 8.sp)
        }
    }
}

@Composable
private fun MonthDaySummary(
    date: LocalDate, tasks: List<Task>, habits: List<Habit>,
    onOpenDay: () -> Unit, onAddTask: () -> Unit
) {
    val dayTasks = TaskSchedule.onDate(tasks, date)
    val dayHabits = habits.filter { HabitProgress.isScheduled(it, date) }
    val completedTasks = dayTasks.count { it.completed }
    val completedHabits = dayHabits.count { HabitProgress.isComplete(it, date) }
    val subtaskTotal = dayTasks.sumOf { it.subtasks.size }
    val subtaskDone = dayTasks.sumOf { task -> task.subtasks.count { it.completed } }
    val minutes = dayTasks.filterNot { it.completed }.sumOf { it.durationMinutes.coerceAtLeast(0) }

    Surface(color = PaperRaised, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(date.format(DateTimeFormatter.ofPattern("EEEE d", EsLocale)).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(if (minutes > 0) "${minutes / 60}h ${minutes % 60}m pendientes" else "Sin carga pendiente", color = MutedInk, fontSize = 12.sp)
                }
                TextButton(onClick = onAddTask, contentPadding = PaddingValues(horizontal = 7.dp)) { Text("＋ Tarea", color = Leaf, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                TextButton(onClick = onOpenDay, contentPadding = PaddingValues(horizontal = 7.dp)) { Text("Abrir →", color = Coral, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthSummaryMetric("$completedTasks/${dayTasks.size}", "tareas", Modifier.weight(1f))
                MonthSummaryMetric("$completedHabits/${dayHabits.size}", "hábitos", Modifier.weight(1f))
                MonthSummaryMetric(if (subtaskTotal == 0) "—" else "$subtaskDone/$subtaskTotal", "subtareas", Modifier.weight(1f))
            }
            val previews = dayTasks.filterNot { it.completed }.take(1)
            previews.forEach { task ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("—", color = Coral, fontWeight = FontWeight.Bold)
                    Text(task.title, modifier = Modifier.padding(start = 8.dp).weight(1f), maxLines = 1, fontSize = 12.sp)
                    if (task.subtasks.isNotEmpty()) Text("${task.subtasks.count { it.completed }}/${task.subtasks.size}", color = MutedInk, fontSize = 10.sp)
                }
            }
            if (dayTasks.isEmpty() && dayHabits.isEmpty()) {
                Text("Día libre. Tócalo para planificar con calma.", color = MutedInk, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun MonthSummaryMetric(value: String, label: String, modifier: Modifier) {
    Column(modifier.background(Ink.copy(alpha = .045f), RoundedCornerShape(10.dp)).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(label, color = MutedInk, fontSize = 9.sp)
    }
}

@Composable
private fun CalendarEmpty(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 5.dp).fillMaxWidth().background(Sky.copy(alpha = .10f), RoundedCornerShape(14.dp)).padding(16.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MutedInk, fontSize = 13.sp)
    }
}
