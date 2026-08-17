@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.trazo.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trazo.app.TrazoViewModel
import com.trazo.app.TaskInput
import com.trazo.app.HabitInput
import com.trazo.app.R
import com.trazo.app.model.Habit
import com.trazo.app.model.HabitCategory
import com.trazo.app.model.HabitProgress
import com.trazo.app.model.HabitUnit
import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import com.trazo.app.model.TaskSchedule
import com.trazo.app.notifications.NotificationCenter
import com.trazo.app.notifications.ReminderPreferences
import com.trazo.app.data.AppSettings
import com.trazo.app.data.ReviewInsights
import com.trazo.app.data.ReviewSummary
import com.trazo.app.data.SmartCaptureParser
import com.trazo.app.data.SmartCaptureResult
import com.trazo.app.data.ThemePreference
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Section(val label: String, val icon: TrazoIconKind) {
    TODAY("Hoy", TrazoIconKind.TODAY), TASKS("Tareas", TrazoIconKind.TASK), CALENDAR("Planner", TrazoIconKind.CALENDAR),
    HABITS("Hábitos", TrazoIconKind.HABIT), FOCUS("Enfoque", TrazoIconKind.FOCUS)
}

private enum class Composer { TASK, HABIT }
private enum class TaskFilter(val label: String) { OPEN("Pendientes"), TODAY("Hoy"), DONE("Hechas"), ALL("Todas") }

@Composable
fun TrazoApp(
    viewModel: TrazoViewModel,
    requestedSection: String? = null,
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    val minimalMode = LocalMinimalMode.current
    val hapticsEnabled = LocalTrazoHaptics.current
    val hapticFeedback = LocalHapticFeedback.current
    val toggleTaskWithFeedback: (String) -> Unit = { id ->
        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.toggleTask(id)
    }
    val toggleHabitWithFeedback: (String) -> Unit = { id ->
        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.toggleHabit(id)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        NotificationCenter.createChannels(context)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val state by viewModel.state
    var section by remember { mutableStateOf(Section.TODAY) }
    LaunchedEffect(requestedSection) {
        Section.entries.firstOrNull { it.name == requestedSection }?.let { section = it }
    }
    var composer by remember { mutableStateOf<Composer?>(null) }
    var taskComposerDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var taskDraft by remember { mutableStateOf<TaskInput?>(null) }
    var habitDraft by remember { mutableStateOf<HabitInput?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val activeTasks = state.tasks.filter { !it.archived && it.deletedAt == null }
    val activeHabits = state.habits.filter { !it.archived && it.deletedAt == null }
    val undo = viewModel.undoAction.value
    LaunchedEffect(undo) {
        undo?.let {
            val result = snackbar.showSnackbar(it.message, "Deshacer")
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) viewModel.undoLast()
            else viewModel.consumeUndo()
        }
    }

    Box(Modifier.fillMaxSize().background(Paper)) {
        if (!minimalMode) PaperTexture()
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { SketchNavigation(section) { section = it } },
            floatingActionButton = {
                if (section != Section.FOCUS && section != Section.TODAY) {
                FloatingActionButton(
                    onClick = {
                        taskComposerDate = if (section == Section.CALENDAR) LocalDate.now() else null
                        composer = if (section == Section.HABITS) Composer.HABIT else Composer.TASK
                    },
                    containerColor = Coral,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.then(if (minimalMode) Modifier else Modifier.rotate(-2f))
                        .semantics { contentDescription = "Crear elemento" }
                ) { TrazoIcon(TrazoIconKind.ADD, color = Color.White, size = 25.dp, description = "Crear elemento") }
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = section,
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    if (reducedMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    else (slideInHorizontally(tween(380)) { direction * it / 5 } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(260)) { -direction * it / 8 } + fadeOut(tween(180)))
                },
                label = "section"
            ) { current ->
                when (current) {
                    Section.TODAY -> TodayScreen(
                        tasks = activeTasks,
                        habits = activeHabits,
                        contentPadding = padding,
                        onTaskToggle = toggleTaskWithFeedback,
                        onHabitToggle = toggleHabitWithFeedback,
                        onAddTask = { composer = Composer.TASK },
                        onAddHabit = { composer = Composer.HABIT },
                        onOpenPlanner = { section = Section.CALENDAR },
                        onOpenFocus = { section = Section.FOCUS },
                        onExportBackup = onExportBackup,
                        onImportBackup = onImportBackup,
                        viewModel = viewModel,
                        onSmartTask = { taskDraft = it; composer = Composer.TASK },
                        onSmartHabit = { habitDraft = it; composer = Composer.HABIT }
                    )
                    Section.TASKS -> TasksScreen(
                        activeTasks, padding, toggleTaskWithFeedback, viewModel::deleteTask,
                        viewModel::setTaskDate, { editingTask = it; composer = Composer.TASK }, viewModel::archiveTask
                    )
                    Section.HABITS -> HabitsScreen(
                        activeHabits, padding, toggleHabitWithFeedback, viewModel::deleteHabit,
                        { editingHabit = it; composer = Composer.HABIT }, viewModel::archiveHabit, viewModel::adjustHabitProgress
                    )
                    Section.CALENDAR -> CalendarScreen(
                        activeTasks, activeHabits, padding, toggleTaskWithFeedback,
                        { id, date -> viewModel.toggleHabit(id, date) },
                        { id, date -> viewModel.toggleHabitException(id, date) },
                        { date -> taskComposerDate = date; composer = Composer.TASK }
                    )
                    Section.FOCUS -> FocusScreen(activeTasks, padding, toggleTaskWithFeedback)
                }
            }
        }
    }

    when (composer) {
        Composer.TASK -> TaskComposer(
            task = editingTask,
            draft = taskDraft,
            initialDate = taskComposerDate,
            onDismiss = { composer = null; editingTask = null },
            onSave = { input ->
                editingTask?.let { viewModel.updateTask(it.id, input) }
                    ?: viewModel.addTask(input)
                composer = null
                editingTask = null
                taskDraft = null
            }
        )
        Composer.HABIT -> HabitComposer(
            habit = editingHabit,
            draft = habitDraft,
            onDismiss = { composer = null; editingHabit = null },
            onSave = { input ->
                editingHabit?.let { viewModel.updateHabit(it.id, input) }
                    ?: viewModel.addHabit(input)
                composer = null
                editingHabit = null
                habitDraft = null
            }
        )
        null -> Unit
    }
}

@Composable
private fun PaperTexture() {
    val line = Sky.copy(alpha = .10f)
    val margin = Coral.copy(alpha = .12f)
    Canvas(Modifier.fillMaxSize()) {
        val gap = 32.dp.toPx()
        var y = 96.dp.toPx()
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            y += gap
        }
        drawLine(margin, Offset(38.dp.toPx(), 0f), Offset(38.dp.toPx(), size.height), 1.dp.toPx())
    }
}

@Composable
private fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    onOpenSettings: (() -> Unit)? = null
) {
    val minimalMode = LocalMinimalMode.current
    val reducedMotion = LocalReducedMotion.current
    val hour = remember { LocalTime.now().hour }
    val contextualIllustration = when {
        onOpenSettings == null -> R.drawable.header_ai_notebook
        hour in 5..11 -> R.drawable.context_ai_morning
        hour in 12..19 -> R.drawable.context_ai_afternoon
        else -> R.drawable.context_ai_evening
    }
    val artMotion = rememberInfiniteTransition(label = "contextual header art")
    val artScale by artMotion.animateFloat(
        initialValue = 1f,
        targetValue = if (reducedMotion) 1f else 1.035f,
        animationSpec = infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "contextual art breathing"
    )
    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Column(Modifier.padding(end = if (minimalMode) if (onOpenSettings == null) 0.dp else 48.dp else 72.dp)) {
            Text(eyebrow.uppercase(Locale.forLanguageTag("es-CL")), color = Coral, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(title, style = MaterialTheme.typography.displaySmall)
            Text(subtitle, color = MutedInk)
        }
        if (!minimalMode || onOpenSettings != null) Box(
            Modifier.size(if (minimalMode) 42.dp else 78.dp).align(Alignment.TopEnd)
                .then(
                    if (onOpenSettings != null) Modifier.clip(CircleShape).clickable(onClick = onOpenSettings)
                        .semantics { contentDescription = "Abrir ajustes" }
                    else Modifier
                )
        ) {
            if (!minimalMode) Image(
                painter = painterResource(contextualIllustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(76.dp).align(Alignment.TopCenter).rotate(-4f).scale(artScale)
            )
            if (onOpenSettings != null) {
                Box(
                    Modifier.size(if (minimalMode) 40.dp else 29.dp).align(if (minimalMode) Alignment.TopEnd else Alignment.BottomEnd).clip(CircleShape)
                        .background(if (minimalMode) Color.Transparent else Coral)
                        .border(if (minimalMode) 1.dp else 2.dp, if (minimalMode) Ink else Paper, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    TrazoIcon(
                        TrazoIconKind.SETTINGS,
                        color = if (minimalMode) Ink else Color.White,
                        size = if (minimalMode) 21.dp else 15.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(
    tasks: List<Task>, habits: List<Habit>, contentPadding: PaddingValues,
    onTaskToggle: (String) -> Unit, onHabitToggle: (String) -> Unit,
    onAddTask: () -> Unit, onAddHabit: () -> Unit,
    onOpenPlanner: () -> Unit, onOpenFocus: () -> Unit,
    onExportBackup: () -> Unit, onImportBackup: () -> Unit,
    viewModel: TrazoViewModel,
    onSmartTask: (TaskInput) -> Unit,
    onSmartHabit: (HabitInput) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var showCapture by remember { mutableStateOf(false) }
    var weeklyReview by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val relevantTasks = TaskSchedule.actionable(tasks, today)
    val pending = relevantTasks.filterNot { it.completed }
    val dueHabits = habits.filter { HabitProgress.isScheduled(it, today) }
    val done = relevantTasks.count { it.completed } + dueHabits.count { HabitProgress.isComplete(it, today) }
    val total = relevantTasks.size + dueHabits.size
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 96.dp)
    ) {
        item {
            val date = today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-CL")))
            PageHeader(
                "Tu estudio",
                greeting(),
                date.replaceFirstChar { it.uppercase() },
                onOpenSettings = { showSettings = true }
            )
        }
        item {
            ProgressNote(done, total)
            QuickActions({ showCapture = true }, onOpenPlanner, onOpenFocus)
            val periodFocusMinutes = if (weeklyReview) viewModel.focusStats().weekMinutes else viewModel.focusStats().todayMinutes
            ReviewCard(
                if (weeklyReview) ReviewInsights.weekly(tasks, habits, today, periodFocusMinutes)
                else ReviewInsights.daily(tasks, habits, today, periodFocusMinutes),
                weeklyReview,
                onTogglePeriod = { weeklyReview = !weeklyReview },
                focusMinutes = periodFocusMinutes,
                onOpenPlanner = onOpenPlanner
            )
            SectionTitle("Siguiente trazo", "solo lo que importa ahora")
        }
        if (pending.isEmpty()) item { EmptyNote("Tu lista respira", "Anota una tarea pequeña para empezar.", onAddTask, R.drawable.widget_ai_task) }
        items(pending.take(4), key = { it.id }) { task ->
            Box(Modifier.animateItem()) { TaskCard(task, onTaskToggle, null) }
        }
        item { SectionTitle("Rituales de hoy", "la constancia también cuenta") }
        if (dueHabits.isEmpty()) item { EmptyNote("Sin rituales hoy", "Crea uno que se sienta tuyo.", onAddHabit, R.drawable.widget_ai_ritual) }
        items(dueHabits, key = { it.id }) { habit ->
            Box(Modifier.animateItem()) { HabitCard(habit, onHabitToggle, null) }
        }
    }
    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            onExport = onExportBackup,
            onImport = onImportBackup,
            viewModel = viewModel
        )
    }
    if (showCapture) {
        CaptureSheet(
            onDismiss = { showCapture = false },
            onTask = { showCapture = false; onAddTask() },
            onHabit = { showCapture = false; onAddHabit() },
            onSmart = { result ->
                showCapture = false
                when (result) {
                    is SmartCaptureResult.TaskDraft -> onSmartTask(result.input)
                    is SmartCaptureResult.HabitDraft -> onSmartHabit(result.input)
                }
            }
        )
    }
}

@Composable
private fun ReviewCard(
    summary: ReviewSummary,
    weekly: Boolean,
    onTogglePeriod: () -> Unit,
    focusMinutes: Int,
    onOpenPlanner: () -> Unit
) {
    val minimalMode = LocalMinimalMode.current
    Surface(
        color = Mustard.copy(alpha = .14f),
        shape = if (minimalMode) RoundedCornerShape(12.dp) else RoundedCornerShape(18.dp, 13.dp, 20.dp, 15.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 7.dp).fillMaxWidth()
            .sketchBorder(Ink.copy(alpha = .14f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (weekly) "REVISION DE 7 DÍAS" else "REVISIÓN DE HOY", color = Coral,
                    fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.1.sp,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onTogglePeriod) { Text(if (weekly) "Ver hoy" else "Ver semana", color = Leaf) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewMetric(summary.completedTasks.toString(), "tareas", Modifier.weight(1f))
                ReviewMetric("${summary.habitsDone}/${summary.habitOpportunities}", "rituales", Modifier.weight(1f))
                ReviewMetric("$focusMinutes", "min foco", Modifier.weight(1f))
            }
            if (summary.overdueTasks > 0) {
                Text("${summary.overdueTasks} atrasadas", color = Coral, fontWeight = FontWeight.Bold,
                    fontSize = 12.sp, modifier = Modifier.padding(top = 9.dp))
            }
            Text(summary.suggestion, color = MutedInk, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
            if (summary.overdueTasks > 0) TextButton(onClick = onOpenPlanner, contentPadding = PaddingValues(top = 5.dp)) {
                Text("Revisar agenda", color = Coral, fontWeight = FontWeight.Bold)
                TrazoIcon(TrazoIconKind.ARROW_RIGHT, color = Coral, size = 16.dp, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun ReviewMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(color = Paper.copy(alpha = .72f), shape = RoundedCornerShape(11.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(label, color = MutedInk, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LocalDataCard(onExport: () -> Unit, onImport: () -> Unit) {
    Surface(
        color = Sky.copy(alpha = .10f),
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp).fillMaxWidth()
            .sketchBorder(Ink.copy(alpha = .12f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Tus datos, contigo", fontWeight = FontWeight.Bold, color = Ink)
            Text(
                "Exporta o restaura una copia JSON. Todo sigue guardado solo en el dispositivo.",
                color = MutedInk,
                fontSize = 12.sp
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                    TrazoIcon(TrazoIconKind.EXPORT, color = Leaf, size = 18.dp)
                    Text("Exportar", color = Leaf, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp))
                }
                TextButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                    TrazoIcon(TrazoIconKind.IMPORT, color = Coral, size = 18.dp)
                    Text("Importar", color = Coral, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp))
                }
            }
        }
    }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Buen día"
    in 12..19 -> "Buena tarde"
    else -> "Buena noche"
}

@Composable
private fun QuickActions(onCapture: () -> Unit, onPlanner: () -> Unit, onFocus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        QuickAction(TrazoIconKind.ADD, "Capturar", Coral.copy(alpha = .13f), onCapture)
        QuickAction(TrazoIconKind.CALENDAR, "Planear", Sky.copy(alpha = .17f), onPlanner)
        QuickAction(TrazoIconKind.FOCUS, "Enfocar", Leaf.copy(alpha = .14f), onFocus)
    }
}

@Composable
private fun CaptureSheet(
    onDismiss: () -> Unit,
    onTask: () -> Unit,
    onHabit: () -> Unit,
    onSmart: (SmartCaptureResult) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { text = it }
        }
    }
    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CL")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla tu tarea o hábito")
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Paper) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("¿Qué quieres capturar?", style = MaterialTheme.typography.headlineMedium)
            Text("Prueba: “Spinning lunes miércoles viernes”, “Leer 30 min fines de semana” o “Informe viernes a las 16”.", color = MutedInk)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Captura rápida") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            )
            Surface(
                onClick = {
                    runCatching { voiceLauncher.launch(voiceIntent) }
                        .onFailure { Toast.makeText(context, "No hay reconocimiento de voz disponible", Toast.LENGTH_LONG).show() }
                },
                color = Sky.copy(alpha = .16f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { contentDescription = "Dictar una nota de voz" }
            ) {
                Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    TrazoIcon(TrazoIconKind.MICROPHONE, color = Coral, size = 19.dp)
                    Text("Dictar nota de voz", color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 9.dp))
                }
            }
            Button(
                onClick = { onSmart(SmartCaptureParser.parse(text)) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Coral),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Interpretar y revisar") }
            Text("O elige manualmente", color = MutedInk, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp))
            Surface(
                onClick = onTask,
                color = Coral.copy(alpha = .11f),
                shape = RoundedCornerShape(17.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).sketchBorder(Coral.copy(alpha = .45f))
            ) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    TrazoIcon(TrazoIconKind.TASK, color = Coral, size = 27.dp)
                    Column(Modifier.padding(start = 15.dp)) {
                        Text("Tarea", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Algo que quieres hacer una vez", color = MutedInk, fontSize = 13.sp)
                    }
                }
            }
            Surface(
                onClick = onHabit,
                color = Leaf.copy(alpha = .11f),
                shape = RoundedCornerShape(17.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 11.dp).sketchBorder(Leaf.copy(alpha = .45f))
            ) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    TrazoIcon(TrazoIconKind.HABIT, color = Leaf, size = 27.dp)
                    Column(Modifier.padding(start = 15.dp)) {
                        Text("Hábito", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Algo que quieres repetir con constancia", color = MutedInk, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    viewModel: TrazoViewModel
) {
    val state by viewModel.state
    val settings by viewModel.settings
    val stats = viewModel.focusStats()
    val today = LocalDate.now()
    val weekStart = today.minusDays(6)
    val weeklyTasks = state.tasks.count { task -> task.completedAt?.let { Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() >= weekStart } == true }
    val activeHabits = state.habits.filter { !it.archived && it.deletedAt == null }
    val habitChecks = activeHabits.sumOf { habit -> (0L..6L).count { offset -> HabitProgress.isComplete(habit, today.minusDays(offset)) } }
    val habitDue = activeHabits.sumOf { habit -> (0L..6L).count { offset -> HabitProgress.isScheduled(habit, today.minusDays(offset)) } }
    val archivedTasks = state.tasks.filter { it.archived && it.deletedAt == null }
    val archivedHabits = state.habits.filter { it.archived && it.deletedAt == null }
    val deletedTasks = state.tasks.filter { it.deletedAt != null }
    val deletedHabits = state.habits.filter { it.deletedAt != null }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Paper) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 30.dp)
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Personaliza tu experiencia, revisa tu avance y cuida tus datos.",
                    color = MutedInk,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                "NOTIFICACIONES",
                color = Coral,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp)
            )
            ReminderCard()
            Text("APARIENCIA Y ACCESIBILIDAD", color = Lavender, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, fontSize = 12.sp, modifier = Modifier.padding(start = 24.dp, top = 12.dp))
            Surface(color = Lavender.copy(alpha = .10f), shape = RoundedCornerShape(15.dp), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Tema", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        ThemePreference.entries.forEach { option ->
                            val selected = settings.theme == option
                            TextButton(onClick = { viewModel.updateSettings(settings.copy(theme = option)) }, modifier = Modifier.weight(1f)) {
                                Text(when(option) { ThemePreference.SYSTEM -> "Sistema"; ThemePreference.LIGHT -> "Claro"; ThemePreference.DARK -> "Oscuro" }, color = if (selected) Coral else MutedInk, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    SettingsToggle("Texto más grande", settings.largeText) { viewModel.updateSettings(settings.copy(largeText = it)) }
                    SettingsToggle("Minimalista blanco y negro", settings.minimalMode) { viewModel.updateSettings(settings.copy(minimalMode = it)) }
                    SettingsToggle("Reducir animaciones", settings.reducedMotion) { viewModel.updateSettings(settings.copy(reducedMotion = it)) }
                    SettingsToggle("Respuesta háptica", settings.haptics) { viewModel.updateSettings(settings.copy(haptics = it)) }
                }
            }
            Text("ESTADÍSTICAS · ÚLTIMOS 7 DÍAS", color = Mustard, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, fontSize = 12.sp, modifier = Modifier.padding(start = 24.dp, top = 12.dp))
            Surface(color = Mustard.copy(alpha = .12f), shape = RoundedCornerShape(15.dp), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    StatCell(weeklyTasks.toString(), "tareas")
                    StatCell(if (habitDue == 0) "—" else "${habitChecks * 100 / habitDue}%", "hábitos")
                    StatCell(stats.sessions.toString(), "enfoques")
                    StatCell(stats.minutes.toString(), "minutos")
                }
            }
            Text("ARCHIVO Y PAPELERA", color = Sky, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, fontSize = 12.sp, modifier = Modifier.padding(start = 24.dp, top = 12.dp))
            Surface(color = Sky.copy(alpha = .10f), shape = RoundedCornerShape(15.dp), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    if (archivedTasks.isEmpty() && archivedHabits.isEmpty() && deletedTasks.isEmpty() && deletedHabits.isEmpty()) Text("Archivo y papelera vacíos", color = MutedInk)
                    (archivedTasks.map { Triple(it.id, it.title, "task") } + archivedHabits.map { Triple(it.id, it.title, "habit") }).forEach { (id, title, kind) ->
                        LibraryRow("Archivado · $title", "Restaurar") { if (kind == "task") viewModel.restoreTask(id) else viewModel.restoreHabit(id) }
                    }
                    (deletedTasks.map { Triple(it.id, it.title, "task") } + deletedHabits.map { Triple(it.id, it.title, "habit") }).forEach { (id, title, kind) ->
                        LibraryRow("Papelera · $title", "Restaurar") { if (kind == "task") viewModel.restoreTask(id) else viewModel.restoreHabit(id) }
                        TextButton(onClick = { if (kind == "task") viewModel.permanentlyDeleteTask(id) else viewModel.permanentlyDeleteHabit(id) }) { Text("Eliminar definitivamente", color = Coral, fontSize = 12.sp) }
                    }
                }
            }
            Text(
                "DATOS LOCALES",
                color = Leaf,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 24.dp, top = 12.dp)
            )
            LocalDataCard(onExport, onImport)
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RowScope.StatCell(value: String, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, color = Ink, fontSize = 18.sp)
        Text(label, color = MutedInk, fontSize = 10.sp)
    }
}

@Composable
private fun LibraryRow(label: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), maxLines = 1)
        TextButton(onClick = onClick) { Text(action, color = Leaf) }
    }
}

@Composable
private fun RowScope.QuickAction(icon: TrazoIconKind, label: String, color: Color, onClick: () -> Unit) {
    val minimalMode = LocalMinimalMode.current
    Surface(
        onClick = onClick,
        color = color,
        shape = if (minimalMode) RoundedCornerShape(12.dp) else RoundedCornerShape(18.dp, 12.dp, 20.dp, 14.dp),
        modifier = Modifier.weight(1f).sketchBorder(Ink.copy(alpha = .18f))
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TrazoIcon(icon, color = Ink, size = 22.dp)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedInk)
        }
    }
}

@Composable
private fun ReminderCard() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(ReminderPreferences.enabled(context)) }
    var hour by remember { mutableIntStateOf(ReminderPreferences.hour(context)) }
    Surface(
        color = Leaf.copy(alpha = .10f), shape = RoundedCornerShape(15.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp).fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrazoIcon(TrazoIconKind.NOTIFICATION, color = Coral, size = 21.dp)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text("Resumen diario", fontWeight = FontWeight.Bold)
                    Text(if (enabled) "Te avisaré a las %02d:00".format(hour) else "Recordatorios desactivados", color = MutedInk, fontSize = 13.sp)
                }
                TextButton(onClick = {
                    enabled = !enabled
                    ReminderPreferences.set(context, enabled, hour)
                }) { Text(if (enabled) "Desactivar" else "Activar", color = if (enabled) MutedInk else Leaf) }
            }
            if (enabled) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                listOf(8, 12, 18).forEach { option ->
                    Surface(
                        onClick = { hour = option; ReminderPreferences.set(context, true, option) },
                        color = if (hour == option) Leaf else Color.Transparent, shape = RoundedCornerShape(10.dp)
                    ) { Text("%02d:00".format(option), color = if (hour == option) Color.White else MutedInk, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TasksScreen(
    tasks: List<Task>, padding: PaddingValues, onToggle: (String) -> Unit,
    onDelete: (String) -> Unit, onDateChange: (String, LocalDate?) -> Unit,
    onEdit: (Task) -> Unit, onArchive: (String) -> Unit
) {
    var filter by remember { mutableStateOf(TaskFilter.OPEN) }
    var query by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val visible = tasks.filter {
        (query.isBlank() || it.title.contains(query, true) || it.note.contains(query, true) || it.tags.any { tag -> tag.contains(query, true) }) &&
        when (filter) {
            TaskFilter.OPEN -> !it.completed
            TaskFilter.TODAY -> !it.completed && (it.dueDate == null || !it.dueDate.isAfter(today))
            TaskFilter.DONE -> it.completed
            TaskFilter.ALL -> true
        }
    }.sortedWith(compareBy<Task> { it.completed }.thenBy { it.dueDate ?: LocalDate.MAX }.thenByDescending { it.priority })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 96.dp)) {
        item { PageHeader("Lista flexible", "Tus tareas", "Ordena sin perder la calma.") }
        item { SearchField(query, { query = it }, "Buscar tareas o #etiquetas") }
        item { TaskFilters(filter) { filter = it } }
        if (visible.isEmpty()) item {
            EmptyNote(
                if (tasks.isEmpty()) "La hoja está limpia" else "Nada por aquí",
                if (tasks.isEmpty()) "Usa el botón + para capturar una idea." else "Prueba otra vista o disfruta el espacio.",
                null,
                if (tasks.isEmpty()) R.drawable.widget_ai_task else null
            )
        }
        items(visible, key = { it.id }) {
            Box(Modifier.animateItem()) {
                TaskCard(it, onToggle, onDelete, { date -> onDateChange(it.id, date) }, { onEdit(it) }, { onArchive(it.id) })
            }
        }
    }
}

@Composable
private fun TaskFilters(selected: TaskFilter, onSelect: (TaskFilter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        TaskFilter.entries.forEach { filter ->
            Surface(
                onClick = { onSelect(filter) },
                color = if (filter == selected) Ink else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    filter.label,
                    color = if (filter == selected) Paper else MutedInk,
                    fontSize = 12.sp,
                    fontWeight = if (filter == selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun HabitsScreen(
    habits: List<Habit>, padding: PaddingValues, onToggle: (String) -> Unit,
    onDelete: (String) -> Unit, onEdit: (Habit) -> Unit,
    onArchive: (String) -> Unit, onAdjust: (String, Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val visible = habits.filter { query.isBlank() || it.title.contains(query, true) || it.category.label.contains(query, true) || it.tags.any { tag -> tag.contains(query, true) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 96.dp)) {
        item { PageHeader("Pequeños trazos", "Tus hábitos", "No busques perfección; vuelve mañana.") }
        item { SearchField(query, { query = it }, "Buscar hábitos o #etiquetas") }
        if (visible.isNotEmpty()) item { HabitSummary(visible) }
        if (visible.isEmpty()) item { EmptyNote("Aún sin rituales", "Agrega un hábito amable y sostenible.", null, R.drawable.widget_ai_ritual) }
        items(visible, key = { it.id }) {
            Box(Modifier.animateItem()) { HabitCard(it, onToggle, onDelete, { onEdit(it) }, { delta -> onAdjust(it.id, delta) }, { onArchive(it.id) }) }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp))
}

@Composable
private fun HabitSummary(habits: List<Habit>) {
    val minimalMode = LocalMinimalMode.current
    val today = LocalDate.now()
    val due = habits.filter { HabitProgress.isScheduled(it, today) }
    val done = due.count { HabitProgress.isComplete(it, today) }
    val best = habits.maxOfOrNull { HabitProgress.streak(it, today) } ?: 0
    Surface(
        color = Sky.copy(alpha = .12f),
        shape = if (minimalMode) RoundedCornerShape(12.dp) else RoundedCornerShape(22.dp, 12.dp, 20.dp, 10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp).sketchBorder(Sky.copy(alpha = .55f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ritmo de hoy", fontWeight = FontWeight.Bold, color = Ink)
                Text("$done de ${due.size} rituales", color = MutedInk, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 10.dp)) {
                Text(if (best == 1) "1 día" else "$best días", style = MaterialTheme.typography.titleLarge, color = Leaf, maxLines = 1)
                Text("mejor racha", color = MutedInk, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ProgressNote(done: Int, total: Int) {
    val minimalMode = LocalMinimalMode.current
    val reducedMotion = LocalReducedMotion.current
    val progress = if (total == 0) 0f else done.toFloat() / total
    val animatedProgress by animateFloatAsState(progress, tween(if (reducedMotion) 0 else 850), label = "daily progress")
    val faintInk = Ink.copy(alpha = .08f)
    val leafColor = Leaf
    val mustardColor = Mustard.copy(alpha = .5f)
    val barBackground = Ink.copy(alpha = .10f)
    val barColor = Coral
    Surface(
        color = PaperRaised,
        shape = if (minimalMode) RoundedCornerShape(12.dp) else RoundedCornerShape(11.dp, 28.dp, 16.dp, 24.dp),
        shadowElevation = if (minimalMode) 0.dp else 3.dp,
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().sketchBorder(Mustard.copy(alpha = .78f))
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(faintInk, -90f, 360f, false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(leafColor, -90f, animatedProgress * 360f, false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(mustardColor, -88f, animatedProgress * 355f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }
                Text(if (total == 0) "✦" else "${(animatedProgress * 100).toInt()}%", fontWeight = FontWeight.Black, color = if (total == 0) Mustard else Ink)
            }
            Column(Modifier.weight(1f).padding(start = 18.dp)) {
                Text(if (total == 0) "Tu día está abierto" else "$done de $total trazos", style = MaterialTheme.typography.titleLarge)
                Text(
                    when {
                        total == 0 -> "Elige algo pequeño y empieza sin presión."
                        done == total -> "Todo lo previsto está en calma."
                        done == 0 -> "Un primer paso cambia el ritmo del día."
                        else -> "Ya avanzaste. Sigue con suavidad."
                    },
                    color = MutedInk,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(10.dp))
                Canvas(Modifier.fillMaxWidth().height(7.dp)) {
                    drawLine(barBackground, Offset(0f, center.y), Offset(size.width, center.y), 6.dp.toPx(), StrokeCap.Round)
                    drawLine(barColor, Offset(0f, center.y), Offset(size.width * animatedProgress, center.y), 6.dp.toPx(), StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, note: String) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(10.dp))
        Text(note, color = MutedInk, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
private fun TaskCard(
    task: Task, onToggle: (String) -> Unit, onDelete: ((String) -> Unit)?,
    onDateChange: ((LocalDate?) -> Unit)? = null, onEdit: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null
) {
    val reducedMotion = LocalReducedMotion.current
    val minimalMode = LocalMinimalMode.current
    val cardColor by animateColorAsState(
        if (task.completed) Leaf.copy(alpha = .16f) else PaperRaised,
        tween(if (reducedMotion) 0 else 220),
        label = "task color"
    )
    Surface(
        color = cardColor,
        shape = if (minimalMode) RoundedCornerShape(12.dp) else RoundedCornerShape(14.dp, 9.dp, 16.dp, 11.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp).fillMaxWidth()
            .then(if (reducedMotion) Modifier else Modifier.animateContentSize(spring(stiffness = 420f)))
            .sketchBorder(Ink.copy(alpha = .42f))
    ) {
        Column {
            Row(Modifier.fillMaxWidth().clickable { onToggle(task.id) }.padding(start = 8.dp, top = 10.dp, bottom = 8.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { onToggle(task.id) },
                    colors = CheckboxDefaults.colors(checkedColor = Leaf, uncheckedColor = Ink)
                )
                Column(Modifier.weight(1f).padding(vertical = 4.dp, horizontal = 4.dp)) {
                    Text(
                        task.title, fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                        color = if (task.completed) MutedInk else Ink,
                        maxLines = 3
                    )
                    if (task.note.isNotBlank()) Text(task.note, color = MutedInk, fontSize = 13.sp, maxLines = 2)
                    if (task.tags.isNotEmpty()) Text(task.tags.joinToString("  ") { "#$it" }, color = Lavender, fontSize = 11.sp, maxLines = 1)
                    task.dueDate?.let { date ->
                        val today = LocalDate.now()
                        val label = when (date) {
                            today -> "Hoy"
                            today.plusDays(1) -> "Mañana"
                            else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-CL")))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TrazoIcon(TrazoIconKind.SCHEDULE, color = if (!task.completed && date.isBefore(today)) Coral else MutedInk, size = 14.dp)
                            Text(label, color = if (!task.completed && date.isBefore(today)) Coral else MutedInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 5.dp))
                        }
                    }
                }
                if (task.priority == TaskPriority.IMPORTANT && !task.completed) Text("!", color = Coral, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
            }
            if (onEdit != null || onDateChange != null || onArchive != null || onDelete != null) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 56.dp, end = 8.dp, bottom = 7.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEdit != null) EditButton("tarea", onEdit)
                    if (onDateChange != null) TaskDateButton(task.dueDate, onDateChange)
                    if (onArchive != null) ArchiveButton("tarea", onArchive)
                    if (onDelete != null) DeleteButton("tarea") { onDelete(task.id) }
                }
            }
        }
    }
}

@Composable
private fun TaskDateButton(current: LocalDate?, onChange: (LocalDate?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    TextButton(
        onClick = { showPicker = true }, contentPadding = PaddingValues(6.dp),
        modifier = Modifier.semantics { contentDescription = "Cambiar fecha" }
    ) { TrazoIcon(TrazoIconKind.SCHEDULE, color = if (current == null) MutedInk else Leaf, size = 20.dp) }
    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (current ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() })
                    showPicker = false
                }) { Text("Reprogramar") }
            },
            dismissButton = {
                Row {
                    if (current != null) TextButton(onClick = { onChange(null); showPicker = false }) { Text("Quitar fecha", color = Coral) }
                    TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
                }
            }
        ) { DatePicker(state) }
    }
}

@Composable
private fun HabitCard(
    habit: Habit, onToggle: (String) -> Unit, onDelete: ((String) -> Unit)?,
    onEdit: (() -> Unit)? = null, onAdjust: ((Int) -> Unit)? = null,
    onArchive: (() -> Unit)? = null
) {
    val today = LocalDate.now()
    val checked = HabitProgress.isComplete(habit, today)
    val amount = HabitProgress.amount(habit, today)
    val streak = HabitProgress.streak(habit, today)
    val reducedMotion = LocalReducedMotion.current
    val minimalMode = LocalMinimalMode.current
    val cardColor by animateColorAsState(
        if (checked) Sky.copy(alpha = .24f) else PaperRaised,
        tween(if (reducedMotion) 0 else 320),
        label = "habit completion color"
    )
    Surface(
        color = cardColor,
        shape = if (minimalMode) RoundedCornerShape(12.dp) else RoundedCornerShape(18.dp, 10.dp, 16.dp, 8.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 5.dp).fillMaxWidth().sketchBorder(Sky)
    ) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(enabled = HabitProgress.isScheduled(habit, today)) { onToggle(habit.id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(CircleShape)
                        .background(if (minimalMode) Color.Transparent else Mustard.copy(alpha = .28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (minimalMode) "○" else habit.emoji, color = Ink, fontSize = if (minimalMode) 18.sp else 22.sp)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(habit.title, fontWeight = FontWeight.Bold, maxLines = 3)
                    Text(
                        "${habit.category.label} · " + if (streak == 0) "Empieza tu racha" else if (streak == 1) "1 día seguido" else "$streak días seguidos",
                        color = MutedInk,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                    if (habit.target > 1) Text("$amount / ${habit.target} ${habit.unit.shortLabel}", color = Leaf, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (habit.tags.isNotEmpty()) Text(habit.tags.joinToString("  ") { "#$it" }, color = Lavender, fontSize = 11.sp, maxLines = 1)
                    HabitWeek(habit, today)
                }
                if (habit.target <= 1 || onAdjust == null) SketchCheck(checked) { onToggle(habit.id) }
            }
            if (onAdjust != null || onEdit != null || onArchive != null || onDelete != null) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 58.dp, end = 8.dp, bottom = 7.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (habit.target > 1 && onAdjust != null) {
                        TextButton(onClick = { onAdjust(-1) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("−", color = MutedInk, fontSize = 20.sp) }
                        Text("$amount/${habit.target}", color = Leaf, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp))
                        TextButton(onClick = { onAdjust(1) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("+", color = Leaf, fontSize = 20.sp) }
                    }
                    if (onEdit != null) EditButton("hábito", onEdit)
                    if (onArchive != null) ArchiveButton("hábito", onArchive)
                    if (onDelete != null) DeleteButton("hábito") { onDelete(habit.id) }
                }
            }
        }
    }
}

@Composable
private fun HabitWeek(habit: Habit, today: LocalDate) {
    val start = today.minusDays(6)
    Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(7) { index ->
            val date = start.plusDays(index.toLong())
            val scheduled = HabitProgress.isScheduled(habit, date)
            val completed = HabitProgress.isComplete(habit, date)
            Box(
                Modifier.size(if (date == today) 10.dp else 8.dp)
                    .background(
                        when {
                            completed -> Leaf
                            scheduled -> Ink.copy(alpha = .14f)
                            else -> Color.Transparent
                        },
                        CircleShape
                    )
                    .then(if (!scheduled) Modifier.border(1.dp, Ink.copy(alpha = .12f), CircleShape) else Modifier)
            )
        }
    }
}

@Composable
private fun EditButton(label: String, onEdit: () -> Unit) {
    TextButton(onClick = onEdit, contentPadding = PaddingValues(7.dp), modifier = Modifier.semantics { contentDescription = "Editar $label" }) {
        TrazoIcon(TrazoIconKind.EDIT, color = Leaf, size = 20.dp)
    }
}

@Composable
private fun ArchiveButton(label: String, onArchive: () -> Unit) {
    TextButton(onClick = onArchive, contentPadding = PaddingValues(7.dp), modifier = Modifier.semantics { contentDescription = "Archivar $label" }) {
        TrazoIcon(TrazoIconKind.ARCHIVE, color = Sky, size = 21.dp)
    }
}

@Composable
private fun SketchCheck(checked: Boolean, onClick: () -> Unit) {
    val checkColor = Leaf
    val emptyColor = Ink.copy(alpha = .45f)
    val reducedMotion = LocalReducedMotion.current
    val minimalMode = LocalMinimalMode.current
    val reveal by animateFloatAsState(
        if (checked) 1f else 0f,
        if (reducedMotion) tween(0) else spring(dampingRatio = .48f, stiffness = 520f),
        label = "ritual check reveal"
    )
    Box(
        Modifier.size(48.dp).scale(if (minimalMode) 1f else 1f + reveal * .10f).clip(CircleShape).clickable(onClick = onClick).semantics {
            role = Role.Checkbox
            contentDescription = if (checked) "Marcar como no realizado" else "Marcar como realizado"
        }, contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(30.dp)) {
            drawCircle(if (reveal > 0f) checkColor else emptyColor, style = Stroke(2.dp.toPx()))
            if (reveal > 0f) {
                val firstStart = Offset(size.width * .22f, size.height * .52f)
                val firstEnd = Offset(size.width * .22f + size.width * .21f * reveal, size.height * .52f + size.height * .20f * reveal)
                val secondStart = Offset(size.width * .43f, size.height * .72f)
                val secondEnd = Offset(size.width * .43f + size.width * .36f * reveal, size.height * .72f - size.height * .44f * reveal)
                drawLine(checkColor, firstStart, firstEnd, 3.dp.toPx(), StrokeCap.Round)
                drawLine(checkColor, secondStart, secondEnd, 3.dp.toPx(), StrokeCap.Round)
                if (!minimalMode) {
                    drawLine(checkColor.copy(alpha = reveal * .55f), Offset(size.width * .08f, size.height * .20f), Offset(size.width * .02f, size.height * .09f), 1.5.dp.toPx(), StrokeCap.Round)
                    drawLine(checkColor.copy(alpha = reveal * .55f), Offset(size.width * .83f, size.height * .17f), Offset(size.width * .91f, size.height * .07f), 1.5.dp.toPx(), StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun DeleteButton(label: String, onDelete: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    TextButton(onClick = { confirming = true }, contentPadding = PaddingValues(8.dp), modifier = Modifier.semantics { contentDescription = "Eliminar $label" }) {
        TrazoIcon(TrazoIconKind.DELETE, color = MutedInk, size = 20.dp)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("¿Borrar $label?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Se moverá a la papelera y podrás deshacerlo.") },
            confirmButton = {
                TextButton(onClick = { confirming = false; onDelete() }) { Text("Borrar", color = Coral) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Conservar") }
            },
            containerColor = Paper
        )
    }
}

@Composable
private fun EmptyNote(title: String, subtitle: String, action: (() -> Unit)?, illustration: Int? = null) {
    val minimalMode = LocalMinimalMode.current
    Row(
        Modifier.padding(horizontal = 24.dp, vertical = 6.dp).fillMaxWidth()
            .background(Sky.copy(alpha = .10f), RoundedCornerShape(12.dp)).sketchBorder(Sky.copy(alpha = .7f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (illustration != null && !minimalMode) Image(
            painterResource(illustration), contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.size(66.dp).padding(end = 10.dp)
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrazoIcon(TrazoIconKind.TASK, color = Ink, size = 17.dp)
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp))
            }
            Text(subtitle, color = MutedInk, fontSize = 14.sp)
            if (action != null) TextButton(onClick = action, contentPadding = PaddingValues(top = 8.dp)) {
                Text("Crear ahora", color = Coral)
                TrazoIcon(TrazoIconKind.ARROW_RIGHT, color = Coral, size = 15.dp, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun SketchNavigation(selected: Section, onSelect: (Section) -> Unit) {
    val minimalMode = LocalMinimalMode.current
    val reducedMotion = LocalReducedMotion.current
    val indicator = Coral
    Surface(color = PaperRaised.copy(alpha = .98f), shadowElevation = if (minimalMode) 0.dp else 12.dp, shape = RoundedCornerShape(topStart = if (minimalMode) 0.dp else 24.dp, topEnd = if (minimalMode) 0.dp else 24.dp)) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Section.entries.forEach { section ->
                val active = section == selected
                val tint by animateColorAsState(if (active) Coral else MutedInk, tween(if (reducedMotion) 0 else 220), label = "nav tint")
                Column(
                    Modifier.weight(1f).scale(if (minimalMode || active) 1f else .96f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) Coral.copy(alpha = .10f) else Color.Transparent)
                        .clickable { onSelect(section) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TrazoIcon(section.icon, color = tint, size = 21.dp)
                    Text(section.label, color = if (active) Ink else MutedInk, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    Canvas(Modifier.width(28.dp).height(3.dp)) {
                        if (active) drawLine(indicator, Offset(4.dp.toPx(), center.y), Offset(size.width - 2.dp.toPx(), center.y), 2.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskComposer(task: Task?, draft: TaskInput?, initialDate: LocalDate?, onDismiss: () -> Unit, onSave: (TaskInput) -> Unit) {
    var title by remember(task, draft) { mutableStateOf(task?.title ?: draft?.title.orEmpty()) }
    var note by remember(task, draft) { mutableStateOf(task?.note ?: draft?.note.orEmpty()) }
    var important by remember(task, draft) { mutableStateOf(task?.priority == TaskPriority.IMPORTANT || draft?.important == true) }
    var dueDate by remember(task, draft, initialDate) { mutableStateOf(task?.dueDate ?: draft?.dueDate ?: initialDate) }
    var reminderHour by remember(task, draft) { mutableStateOf(task?.reminderHour ?: draft?.reminderHour) }
    var reminderMinute by remember(task, draft) { mutableIntStateOf(task?.reminderMinute ?: draft?.reminderMinute ?: 0) }
    var reminderText by remember(task, draft) { mutableStateOf((task?.reminderHour ?: draft?.reminderHour)?.let { "%02d:%02d".format(it, task?.reminderMinute ?: draft?.reminderMinute ?: 0) }.orEmpty()) }
    var tags by remember(task, draft) { mutableStateOf((task?.tags ?: draft?.tags.orEmpty()).joinToString(", ")) }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Paper) {
        ComposerLayout(if (task == null) "Nueva tarea" else "Editar tarea", if (task == null) "Sácala de tu cabeza y déjala aquí." else "Ajusta lo que necesites.") {
            OutlinedTextField(
                value = title, onValueChange = { title = it }, label = { Text("¿Qué quieres hacer?") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note, onValueChange = { note = it }, label = { Text("Una nota (opcional)") },
                modifier = Modifier.fillMaxWidth(), maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { important = !important }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(important, { important = it }, colors = CheckboxDefaults.colors(checkedColor = Coral))
                Text("Es importante para hoy")
            }
            Text("Fecha", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DateChoice("Sin fecha", dueDate == null) { dueDate = null }
                DateChoice("Hoy", dueDate == LocalDate.now()) { dueDate = LocalDate.now() }
                DateChoice("Mañana", dueDate == LocalDate.now().plusDays(1)) { dueDate = LocalDate.now().plusDays(1) }
            }
            TextButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(dueDate?.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-CL")))?.replaceFirstChar { it.uppercase() } ?: "Elegir otra fecha…", color = Leaf)
            }
            Text("Recordatorio", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(null, 8, 12, 18, 21).forEach { hour ->
                    TimeChoice(if (hour == null) "No" else "%02d:00".format(hour), reminderHour == hour && reminderMinute == 0) { reminderHour = hour; reminderMinute = 0; reminderText = hour?.let { "%02d:00".format(it) }.orEmpty() }
                }
            }
            OutlinedTextField(reminderText, { value -> reminderText = value.take(5); parseReminder(value)?.let { (h, m) -> reminderHour = h; reminderMinute = m } }, label = { Text("Hora personalizada (HH:MM)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 7.dp))
            OutlinedTextField(tags, { tags = it }, label = { Text("Etiquetas separadas por coma") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            SaveButton(if (task == null) "Guardar tarea" else "Guardar cambios", title.isNotBlank()) {
                onSave(TaskInput(title, note, important, dueDate, reminderHour, reminderMinute, parseTags(tags)))
            }
        }
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (dueDate ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = pickerState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDatePicker = false
                }) { Text("Elegir") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun RowScope.TimeChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = if (selected) Leaf else Ink.copy(alpha = .05f), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
        Text(label, color = if (selected) Color.White else MutedInk, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun RowScope.DateChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, color = if (selected) Mustard.copy(alpha = .30f) else Ink.copy(alpha = .05f),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
    ) { Text(label, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(vertical = 9.dp), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
}

@Composable
private fun HabitComposer(
    habit: Habit?,
    draft: HabitInput?,
    onDismiss: () -> Unit,
    onSave: (HabitInput) -> Unit
) {
    var title by remember(habit, draft) { mutableStateOf(habit?.title ?: draft?.title.orEmpty()) }
    var emoji by remember(habit, draft) { mutableStateOf(habit?.emoji ?: draft?.emoji ?: "✦") }
    var category by remember(habit, draft) { mutableStateOf(habit?.category ?: draft?.category ?: HabitCategory.GENERAL) }
    var days by remember(habit, draft) { mutableStateOf(habit?.activeDays ?: draft?.days ?: DayOfWeek.entries.toSet()) }
    var repeatEveryWeeks by remember(habit, draft) { mutableIntStateOf(habit?.repeatEveryWeeks ?: draft?.repeatEveryWeeks ?: 1) }
    var skippedDates by remember(habit, draft) { mutableStateOf(habit?.skippedDates ?: draft?.skippedDates.orEmpty()) }
    var showExceptionPicker by remember { mutableStateOf(false) }
    var target by remember(habit, draft) { mutableIntStateOf(habit?.target ?: draft?.target ?: 1) }
    var unit by remember(habit, draft) { mutableStateOf(habit?.unit ?: draft?.unit ?: HabitUnit.CHECK) }
    var reminderHour by remember(habit, draft) { mutableStateOf(habit?.reminderHour ?: draft?.reminderHour) }
    var reminderMinute by remember(habit, draft) { mutableIntStateOf(habit?.reminderMinute ?: draft?.reminderMinute ?: 0) }
    var reminderText by remember(habit, draft) { mutableStateOf((habit?.reminderHour ?: draft?.reminderHour)?.let { "%02d:%02d".format(it, habit?.reminderMinute ?: draft?.reminderMinute ?: 0) }.orEmpty()) }
    var tags by remember(habit, draft) { mutableStateOf((habit?.tags ?: draft?.tags.orEmpty()).joinToString(", ")) }
    val labels = mapOf(
        DayOfWeek.MONDAY to "L", DayOfWeek.TUESDAY to "M", DayOfWeek.WEDNESDAY to "X",
        DayOfWeek.THURSDAY to "J", DayOfWeek.FRIDAY to "V", DayOfWeek.SATURDAY to "S", DayOfWeek.SUNDAY to "D"
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Paper) {
        ComposerLayout(if (habit == null) "Nuevo hábito" else "Editar hábito", if (habit == null) "Hazlo tan pequeño que dé gusto volver." else "Haz que siga encajando en tu vida.") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(emoji, { emoji = it.take(2) }, label = { Text("Símbolo") }, singleLine = true, modifier = Modifier.width(94.dp))
                OutlinedTextField(title, { title = it }, label = { Text("Nombre del hábito") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Text("Categoría", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
            HabitCategory.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { option ->
                        val selected = option == category
                        Surface(
                            onClick = {
                                category = option
                                emoji = option.symbol
                            },
                            color = if (selected) Leaf else Sky.copy(alpha = .10f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "${option.symbol} ${option.label}",
                                color = if (selected) Color.White else MutedInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
            }
            Text("Meta", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple("Hecho", HabitUnit.CHECK, 1), Triple("5 veces", HabitUnit.TIMES, 5),
                    Triple("20 min", HabitUnit.MINUTES, 20), Triple("8000 pasos", HabitUnit.STEPS, 8000)
                ).forEach { (label, optionUnit, optionTarget) ->
                    Surface(onClick = { unit = optionUnit; target = optionTarget }, color = if (unit == optionUnit) Leaf else Ink.copy(alpha = .05f), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                        Text(label, color = if (unit == optionUnit) Color.White else MutedInk, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(vertical = 9.dp))
                    }
                }
            }
            if (unit != HabitUnit.CHECK) OutlinedTextField(target.toString(), { target = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 99999) ?: 1 }, label = { Text("Meta personalizada (${unit.label})") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Text("¿Qué días?", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DayOfWeek.entries.forEach { day ->
                    val active = day in days
                    Box(
                        Modifier.size(42.dp).clip(CircleShape)
                            .background(if (active) Leaf else Color.Transparent)
                            .border(1.dp, if (active) Leaf else Ink.copy(alpha = .4f), CircleShape)
                            .clickable { days = if (active) days - day else days + day }
                            .semantics { contentDescription = "${day.name}, ${if (active) "seleccionado" else "no seleccionado"}" },
                        contentAlignment = Alignment.Center
                    ) { Text(labels.getValue(day), color = if (active) Color.White else Ink, fontWeight = FontWeight.Bold) }
                }
            }
            Text("Frecuencia", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 7.dp))
            Surface(color = Sky.copy(alpha = .10f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { repeatEveryWeeks = (repeatEveryWeeks - 1).coerceAtLeast(1) }) { Text("−", fontSize = 20.sp, color = MutedInk) }
                    Text(
                        if (repeatEveryWeeks == 1) "Cada semana" else "Cada $repeatEveryWeeks semanas",
                        modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold, color = Ink
                    )
                    TextButton(onClick = { repeatEveryWeeks = (repeatEveryWeeks + 1).coerceAtMost(12) }) { Text("+", fontSize = 20.sp, color = Leaf) }
                }
            }
            Text("Excepciones", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 3.dp))
            Text("Omite días concretos sin romper la recurrencia.", color = MutedInk, fontSize = 12.sp)
            if (skippedDates.isNotEmpty()) Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                skippedDates.sorted().forEach { date ->
                    Surface(onClick = { skippedDates = skippedDates - date }, color = Lavender.copy(alpha = .16f), shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(date.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-CL"))), color = Ink, fontSize = 12.sp)
                            TrazoIcon(TrazoIconKind.CLOSE, color = Ink, size = 13.dp, modifier = Modifier.padding(start = 7.dp))
                        }
                    }
                }
            }
            TextButton(onClick = { showExceptionPicker = true }, contentPadding = PaddingValues(top = 5.dp)) {
                Text("+ Omitir una fecha", color = Coral, fontWeight = FontWeight.Bold)
            }
            Text("Recordatorio", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(null, 8, 12, 18, 21).forEach { hour ->
                    TimeChoice(if (hour == null) "No" else "%02d:00".format(hour), reminderHour == hour && reminderMinute == 0) { reminderHour = hour; reminderMinute = 0; reminderText = hour?.let { "%02d:00".format(it) }.orEmpty() }
                }
            }
            OutlinedTextField(reminderText, { value -> reminderText = value.take(5); parseReminder(value)?.let { (h, m) -> reminderHour = h; reminderMinute = m } }, label = { Text("Hora personalizada (HH:MM)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 7.dp))
            OutlinedTextField(tags, { tags = it }, label = { Text("Etiquetas separadas por coma") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            Spacer(Modifier.height(22.dp))
            SaveButton(if (habit == null) "Crear hábito" else "Guardar cambios", title.isNotBlank() && days.isNotEmpty()) {
                onSave(HabitInput(
                    title = title, emoji = emoji, category = category, days = days,
                    repeatEveryWeeks = repeatEveryWeeks,
                    skippedDates = skippedDates,
                    target = target, unit = unit,
                    reminderHour = reminderHour, reminderMinute = reminderMinute,
                    tags = parseTags(tags)
                ))
            }
        }
    }
    if (showExceptionPicker) {
        val exceptionPickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showExceptionPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    exceptionPickerState.selectedDateMillis?.let {
                        skippedDates = (skippedDates + Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()).take(32).toSet()
                    }
                    showExceptionPicker = false
                }) { Text("Omitir fecha", color = Coral) }
            },
            dismissButton = { TextButton(onClick = { showExceptionPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(exceptionPickerState) }
    }
}

private fun parseTags(value: String): Set<String> = value.split(',', ' ').mapNotNull { it.trim().removePrefix("#").lowercase().takeIf(String::isNotBlank) }.take(8).toSet()

private fun parseReminder(value: String): Pair<Int, Int>? {
    val parts = value.split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour to minute else null
}

@Composable
private fun ComposerLayout(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, color = MutedInk, modifier = Modifier.padding(bottom = 18.dp))
        content()
    }
}

@Composable
private fun SaveButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val minimalMode = LocalMinimalMode.current
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = if (minimalMode) RoundedCornerShape(12.dp) else RoundedCornerShape(14.dp, 20.dp, 12.dp, 18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Coral)
    ) { Text(label, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Modifier.sketchBorder(color: Color): Modifier {
    val minimalMode = LocalMinimalMode.current
    val minimalInk = Ink.copy(alpha = .14f)
    return if (minimalMode) border(1.dp, minimalInk, RoundedCornerShape(12.dp)) else drawBehind {
        val stroke = 1.25.dp.toPx()
        drawRoundRect(color, style = Stroke(stroke, pathEffect = PathEffect.cornerPathEffect(5.dp.toPx())), cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()))
        drawLine(color.copy(alpha = .22f), Offset(12.dp.toPx(), size.height + 1.dp.toPx()), Offset(size.width - 8.dp.toPx(), size.height - 1.dp.toPx()), stroke, StrokeCap.Round)
    }
}
