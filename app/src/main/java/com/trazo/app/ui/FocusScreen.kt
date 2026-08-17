package com.trazo.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.view.WindowManager
import com.trazo.app.R
import com.trazo.app.data.FocusPreferences
import com.trazo.app.data.FocusPreferencesStore
import com.trazo.app.model.Task
import com.trazo.app.notifications.FocusSessionStore
import com.trazo.app.notifications.FocusTimerService
import kotlinx.coroutines.delay

internal enum class FocusPhase { FOCUS, BREAK }
internal enum class FocusArtState { READY, ACTIVE, PAUSED, BREAK }

internal fun nextFocusPhase(phase: FocusPhase): FocusPhase =
    if (phase == FocusPhase.FOCUS) FocusPhase.BREAK else FocusPhase.FOCUS

internal fun focusArtState(
    phase: FocusPhase, running: Boolean, remaining: Int, total: Int
): FocusArtState = when {
    phase == FocusPhase.BREAK -> FocusArtState.BREAK
    running -> FocusArtState.ACTIVE
    remaining in 1 until total -> FocusArtState.PAUSED
    else -> FocusArtState.READY
}

internal fun elapsedTimerProgress(remaining: Int, total: Int): Float {
    if (total <= 0) return 0f
    return (1f - remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

@Composable
internal fun FocusScreen(tasks: List<Task>, padding: PaddingValues, onTaskComplete: (String) -> Unit) {
    val context = LocalContext.current
    val storedPreferences = remember { FocusPreferencesStore.load(context) }
    val pending = tasks.filterNot { it.completed }
    val restoredSession = remember { FocusSessionStore.load(context) }
    var selectedTaskId by rememberSaveable {
        mutableStateOf(tasks.firstOrNull { it.title == restoredSession?.taskTitle }?.id)
    }
    var focusMinutes by rememberSaveable {
        mutableIntStateOf(if (restoredSession?.phase == FocusPhase.FOCUS.name) (restoredSession.totalSeconds / 60).coerceAtLeast(1) else storedPreferences.focusMinutes)
    }
    var breakMinutes by rememberSaveable {
        mutableIntStateOf(if (restoredSession?.phase == FocusPhase.BREAK.name) (restoredSession.totalSeconds / 60).coerceAtLeast(1) else storedPreferences.shortBreakMinutes)
    }
    var phaseName by rememberSaveable { mutableStateOf(restoredSession?.phase ?: FocusPhase.FOCUS.name) }
    var remaining by rememberSaveable {
        mutableIntStateOf(
            restoredSession?.let {
                ((it.endAt - System.currentTimeMillis() + 999L) / 1000L).toInt().coerceAtLeast(1)
            } ?: (storedPreferences.focusMinutes * 60)
        )
    }
    var running by rememberSaveable { mutableStateOf(restoredSession != null) }
    var targetEpoch by rememberSaveable { mutableLongStateOf(restoredSession?.endAt ?: 0L) }
    var sessions by rememberSaveable { mutableIntStateOf(0) }
    var customDialog by rememberSaveable { mutableStateOf(false) }
    var customFocus by rememberSaveable { mutableStateOf("25") }
    var customBreak by rememberSaveable { mutableStateOf("5") }
    var customLongBreak by rememberSaveable { mutableStateOf(storedPreferences.longBreakMinutes.toString()) }
    var customCycles by rememberSaveable { mutableStateOf(storedPreferences.cyclesBeforeLongBreak.toString()) }
    var longBreakMinutes by rememberSaveable { mutableIntStateOf(storedPreferences.longBreakMinutes) }
    var cyclesBeforeLong by rememberSaveable { mutableIntStateOf(storedPreferences.cyclesBeforeLongBreak) }
    var autoAdvance by rememberSaveable { mutableStateOf(storedPreferences.autoAdvance) }
    var ambientMode by rememberSaveable { mutableStateOf(false) }
    val phase = FocusPhase.valueOf(phaseName)
    val total = if (phase == FocusPhase.FOCUS) focusMinutes * 60 else breakMinutes * 60
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }

    fun stopService(resetUi: Boolean = false) = context.startService(
        Intent(context, FocusTimerService::class.java)
            .setAction(FocusTimerService.ACTION_STOP)
            .putExtra(FocusTimerService.EXTRA_RESET_UI, resetUi)
    )
    fun resetToFocus() { running = false; phaseName = FocusPhase.FOCUS.name; remaining = focusMinutes * 60; stopService() }
    val toggleTimer: () -> Unit = {
        if (running) {
            remaining = ((targetEpoch - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
            running = false
            stopService()
        } else {
            targetEpoch = System.currentTimeMillis() + remaining * 1000L
            running = true
            ContextCompat.startForegroundService(context, Intent(context, FocusTimerService::class.java).apply {
                putExtra(FocusTimerService.EXTRA_END_AT, targetEpoch)
                putExtra(FocusTimerService.EXTRA_TASK, selectedTask?.title)
                putExtra(FocusTimerService.EXTRA_PHASE, phase.name)
                putExtra(FocusTimerService.EXTRA_AUTO_ADVANCE, autoAdvance)
                putExtra(FocusTimerService.EXTRA_FOCUS_SECONDS, focusMinutes * 60)
                putExtra(FocusTimerService.EXTRA_SHORT_BREAK_SECONDS, breakMinutes * 60)
                putExtra(FocusTimerService.EXTRA_LONG_BREAK_SECONDS, longBreakMinutes * 60)
                putExtra(FocusTimerService.EXTRA_CYCLES_BEFORE_LONG, cyclesBeforeLong)
                putExtra(FocusTimerService.EXTRA_COMPLETED_SESSIONS, sessions)
            })
        }
    }

    DisposableEffect(context, focusMinutes) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == FocusTimerService.ACTION_STATE_CHANGED) {
                    if (intent.getBooleanExtra(FocusTimerService.EXTRA_RUNNING, false)) {
                        phaseName = intent.getStringExtra(FocusTimerService.EXTRA_PHASE) ?: FocusPhase.FOCUS.name
                        targetEpoch = intent.getLongExtra(FocusTimerService.EXTRA_END_AT, 0L)
                        remaining = intent.getIntExtra(FocusTimerService.EXTRA_TOTAL_SECONDS, focusMinutes * 60)
                        running = true
                        if (phaseName == FocusPhase.BREAK.name) sessions++
                    } else {
                        running = false; phaseName = FocusPhase.FOCUS.name; remaining = focusMinutes * 60
                    }
                }
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(FocusTimerService.ACTION_STATE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(running, targetEpoch) {
        while (running) {
            remaining = ((targetEpoch - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
            if (remaining == 0) {
                running = false
                if (!autoAdvance) {
                    if (phase == FocusPhase.FOCUS) sessions++
                    val next = nextFocusPhase(phase)
                    phaseName = next.name
                    remaining = if (next == FocusPhase.FOCUS) focusMinutes * 60 else breakMinutes * 60
                }
                break
            }
            delay(250)
        }
    }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 88.dp)) {
        item {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
                Text("MODO ENFOQUE", color = Coral, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(if (phase == FocusPhase.FOCUS) "Un trazo a la vez" else "Respira un momento", style = MaterialTheme.typography.headlineMedium)
                Text(if (sessions == 0) "Tu atención también es un hábito." else "$sessions sesiones terminadas", color = MutedInk)
            }
            TomatoTimer(remaining, total, phase, selectedTask?.title, running)
            TimerControls(running, phase, onStartPause = toggleTimer, onReset = { resetToFocus() }, onSkip = {
                running = false; stopService()
                val next = nextFocusPhase(phase)
                phaseName = next.name
                remaining = if (next == FocusPhase.FOCUS) focusMinutes * 60 else breakMinutes * 60
            })
            AmbientModeButton(running) { ambientMode = true }
            if (!running) AutoAdvanceCard(autoAdvance) {
                autoAdvance = it
                FocusPreferencesStore.save(context, FocusPreferences(focusMinutes, breakMinutes, longBreakMinutes, cyclesBeforeLong, autoAdvance))
            }
            if (!running) Presets(focusMinutes, { f, b ->
                focusMinutes = f; breakMinutes = b; phaseName = FocusPhase.FOCUS.name; remaining = f * 60
                FocusPreferencesStore.save(context, FocusPreferences(focusMinutes, breakMinutes, longBreakMinutes, cyclesBeforeLong, autoAdvance))
            }, {
                customFocus = focusMinutes.toString(); customBreak = breakMinutes.toString()
                customLongBreak = longBreakMinutes.toString(); customCycles = cyclesBeforeLong.toString(); customDialog = true
            })
            Text("Tarea para esta sesión", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(24.dp, 14.dp, 24.dp, 8.dp))
        }
        if (pending.isEmpty()) item { Text("No hay tareas pendientes.", color = MutedInk, modifier = Modifier.padding(24.dp)) }
        items(pending.take(8), key = { it.id }) { task ->
            FocusTask(task, task.id == selectedTaskId) { selectedTaskId = if (selectedTaskId == task.id) null else task.id }
        }
        if (selectedTask != null) item {
            TextButton(onClick = { onTaskComplete(selectedTask.id); selectedTaskId = null }, modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
                Text("Marcar tarea como terminada", color = Leaf, fontWeight = FontWeight.Bold)
                TrazoIcon(TrazoIconKind.CHECK, color = Leaf, size = 17.dp, modifier = Modifier.padding(start = 7.dp))
            }
        }
    }

    if (customDialog) AlertDialog(
        onDismissRequest = { customDialog = false }, containerColor = Paper,
        title = { Text("Tu propio ritmo", style = MaterialTheme.typography.titleLarge) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Define el ciclo completo de enfoque y pausas.", color = MutedInk)
            OutlinedTextField(customFocus, { customFocus = it.filter(Char::isDigit).take(3) }, label = { Text("Enfoque (1–180)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(customBreak, { customBreak = it.filter(Char::isDigit).take(2) }, label = { Text("Pausa (1–60)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(customLongBreak, { customLongBreak = it.filter(Char::isDigit).take(2) }, label = { Text("Pausa larga (1–90)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(customCycles, { customCycles = it.filter(Char::isDigit).take(1) }, label = { Text("Pausa larga cada ciclos (2–8)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        } },
        confirmButton = { TextButton(onClick = {
            focusMinutes = customFocus.toIntOrNull()?.coerceIn(1, 180) ?: 25
            breakMinutes = customBreak.toIntOrNull()?.coerceIn(1, 60) ?: 5
            longBreakMinutes = customLongBreak.toIntOrNull()?.coerceIn(1, 90) ?: 15
            cyclesBeforeLong = customCycles.toIntOrNull()?.coerceIn(2, 8) ?: 4
            FocusPreferencesStore.save(context, FocusPreferences(focusMinutes, breakMinutes, longBreakMinutes, cyclesBeforeLong, autoAdvance))
            phaseName = FocusPhase.FOCUS.name; remaining = focusMinutes * 60; customDialog = false
        }) { Text("Usar tiempos", color = Coral) } },
        dismissButton = { TextButton(onClick = { customDialog = false }) { Text("Cancelar") } }
    )

    if (ambientMode) {
        AmbientFocusMode(
            remaining = remaining,
            total = total,
            phase = phase,
            running = running,
            taskTitle = selectedTask?.title,
            onToggle = toggleTimer,
            onClose = { ambientMode = false }
        )
    }
}

@Composable
private fun AutoAdvanceCard(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        color = Leaf.copy(alpha = .09f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ciclo automático", color = Ink, fontWeight = FontWeight.Bold)
                Text("Alterna enfoque, pausa corta y pausa larga", color = MutedInk, fontSize = 11.sp)
            }
            Switch(checked = enabled, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun AmbientModeButton(running: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = running,
        color = if (running) Ink.copy(alpha = .06f) else Ink.copy(alpha = .025f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp).fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrazoIcon(TrazoIconKind.FOCUS, color = if (running) Leaf else MutedInk.copy(alpha = .45f), size = 20.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text("Pantalla siempre activa", fontWeight = FontWeight.Bold, color = if (running) Ink else MutedInk)
                Text(
                    if (running) "Reloj tenue para dejar el teléfono a la vista" else "Disponible al iniciar el contador",
                    color = MutedInk,
                    fontSize = 11.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Abrir", color = if (running) Coral else MutedInk.copy(alpha = .45f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                TrazoIcon(TrazoIconKind.ARROW_RIGHT, color = if (running) Coral else MutedInk.copy(alpha = .45f), size = 14.dp, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

@Composable
private fun AmbientFocusMode(
    remaining: Int,
    total: Int,
    phase: FocusPhase,
    running: Boolean,
    taskTitle: String?,
    onToggle: () -> Unit,
    onClose: () -> Unit
) {
    val minimalMode = LocalMinimalMode.current
    val ambientProgress = if (minimalMode) Color.White.copy(alpha = .82f)
        else if (phase == FocusPhase.FOCUS) Coral.copy(alpha = .62f) else Leaf.copy(alpha = .62f)
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        DisposableEffect(view, running) {
            view.keepScreenOn = running
            val window = (view.parent as? DialogWindowProvider)?.window
            val oldBrightness = window?.attributes?.screenBrightness
            window?.let {
                val attributes = it.attributes
                attributes.screenBrightness = .08f
                it.attributes = attributes
                it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                it.statusBarColor = android.graphics.Color.BLACK
                it.navigationBarColor = android.graphics.Color.BLACK
            }
            onDispose {
                view.keepScreenOn = false
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (oldBrightness != null) {
                    val attributes = window.attributes
                    attributes.screenBrightness = oldBrightness
                    window.attributes = attributes
                }
            }
        }

        Box(
            Modifier.fillMaxSize().background(Color(0xFF070908)).padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (phase == FocusPhase.FOCUS) "TRAZO EN CALMA" else "PAUSA CONSCIENTE",
                    color = if (minimalMode) Color.White.copy(alpha = .82f)
                        else if (phase == FocusPhase.FOCUS) Coral.copy(alpha = .82f) else Leaf.copy(alpha = .82f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.4.sp,
                    fontSize = 12.sp
                )
                if (!minimalMode) AmbientIllustration(phase, running) else Spacer(Modifier.height(42.dp))
                Text(
                    "%02d:%02d".format(remaining / 60, remaining % 60),
                    color = Color(0xFFE5DED1),
                    fontSize = 66.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                )
                Text(
                    if (phase == FocusPhase.BREAK) "Respira. No hay nada que perseguir." else taskTitle ?: "Un trazo a la vez",
                    color = Color(0xFF8F938D),
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp).padding(top = 8.dp)
                )
                val progress by animateFloatAsState(
                    targetValue = elapsedTimerProgress(remaining, total),
                    animationSpec = tween(if (LocalReducedMotion.current) 0 else 350),
                    label = "ambient timer progress"
                )
                Canvas(Modifier.padding(top = 28.dp).width(220.dp).height(8.dp)) {
                    drawLine(Color.White.copy(alpha = .08f), Offset(0f, center.y), Offset(size.width, center.y), 4.dp.toPx(), StrokeCap.Round)
                    drawLine(
                        ambientProgress,
                        Offset(0f, center.y), Offset(size.width * progress, center.y), 4.dp.toPx(), StrokeCap.Round
                    )
                }
                TextButton(onClick = onToggle, modifier = Modifier.padding(top = 26.dp)) {
                    Text(if (running) "Pausar" else "Continuar", color = Color(0xFFB7B2A9), fontWeight = FontWeight.Bold)
                }
            }
            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 12.dp)
            ) {
                Text("Salir", color = Color(0xFF777B76), fontSize = 13.sp)
                TrazoIcon(TrazoIconKind.CLOSE, color = Color(0xFF777B76), size = 14.dp, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun AmbientIllustration(phase: FocusPhase, running: Boolean) {
    val motionEnabled = !LocalReducedMotion.current && !LocalMinimalMode.current
    val motion = rememberInfiniteTransition(label = "ambient illustration")
    val breathe by motion.animateFloat(
        initialValue = .98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ambient breathe"
    )
    Image(
        painter = painterResource(if (phase == FocusPhase.FOCUS) R.drawable.pomodoro_ai_tomato else R.drawable.pomodoro_ai_cup),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        alpha = .76f,
        modifier = Modifier.padding(vertical = 24.dp).size(164.dp)
            .graphicsLayer {
                val scale = if (running && motionEnabled) breathe else 1f
                scaleX = scale
                scaleY = scale
            }
    )
}

@Composable
private fun TomatoTimer(remaining: Int, total: Int, phase: FocusPhase, taskTitle: String?, running: Boolean) {
    if (LocalMinimalMode.current) {
        MinimalTimer(remaining, total, phase, taskTitle, running)
        return
    }
    val motionEnabled = !LocalReducedMotion.current && !LocalMinimalMode.current
    val paused = !running && remaining in 1 until total
    val artState = focusArtState(phase, running, remaining, total)
    val progressTrack = Ink.copy(alpha = .12f)
    val progressColor = if (phase == FocusPhase.FOCUS) Coral else Leaf
    val progress by animateFloatAsState(
        targetValue = elapsedTimerProgress(remaining, total),
        animationSpec = tween(if (motionEnabled) 350 else 0),
        label = "timer progress"
    )
    val pop = remember { Animatable(1f) }
    val tilt = remember { Animatable(0f) }
    val livingMotion = rememberInfiniteTransition(label = "focus illustration")
    val breathe by livingMotion.animateFloat(
        initialValue = .985f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gentle breathing"
    )
    val sway by livingMotion.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hand drawn sway"
    )
    val motionEnergy = ((breathe - .985f) / .06f).coerceIn(0f, 1f)
    val contextMessage = when {
        paused && phase == FocusPhase.FOCUS -> "Pausa · tu progreso está guardado"
        paused -> "Descanso en pausa"
        !running && phase == FocusPhase.FOCUS -> taskTitle ?: "Listo para empezar"
        !running -> "La pausa está lista"
        phase == FocusPhase.BREAK -> "La taza respira contigo"
        progress < .33f -> taskTitle ?: "El tomate toma impulso"
        progress < .75f -> taskTitle ?: "Buen ritmo, sigue con calma"
        else -> taskTitle ?: "Último tramo"
    }
    LaunchedEffect(running, phase) {
        if (running && motionEnabled) {
            pop.snapTo(.94f); tilt.snapTo(-2.5f)
            pop.animateTo(1.06f, spring(dampingRatio = .48f, stiffness = 420f))
            tilt.animateTo(1.5f, spring(dampingRatio = .55f, stiffness = 480f))
            pop.animateTo(1f, spring(dampingRatio = .62f, stiffness = 520f))
            tilt.animateTo(0f, spring(dampingRatio = .65f, stiffness = 520f))
        } else {
            pop.snapTo(1f); tilt.snapTo(0f)
        }
    }
    Box(
        Modifier.fillMaxWidth().padding(vertical = 14.dp)
            .graphicsLayer {
                val livingScale = if (running && motionEnabled) breathe else 1f
                scaleX = pop.value * livingScale
                scaleY = pop.value * livingScale
                rotationZ = tilt.value + if (running && motionEnabled) sway else 0f
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = artState,
            transitionSpec = {
                (fadeIn(tween(if (motionEnabled) 380 else 0)) + scaleIn(tween(if (motionEnabled) 420 else 0), initialScale = .88f)) togetherWith
                    (fadeOut(tween(if (motionEnabled) 220 else 0)) + scaleOut(tween(if (motionEnabled) 250 else 0), targetScale = 1.08f))
            },
            label = "focus break illustration"
        ) { visualState ->
            val illustration = when (visualState) {
                FocusArtState.READY -> R.drawable.pomodoro_ai_tomato
                FocusArtState.ACTIVE -> R.drawable.pomodoro_ai_focus_active
                FocusArtState.PAUSED -> R.drawable.pomodoro_ai_focus_paused
                FocusArtState.BREAK -> R.drawable.pomodoro_ai_cup
            }
            Image(
                painter = painterResource(illustration),
                contentDescription = when (visualState) {
                    FocusArtState.READY -> "Tomate preparado para comenzar"
                    FocusArtState.ACTIVE -> "Tomate concentrado sosteniendo un lápiz"
                    FocusArtState.PAUSED -> "Tomate descansando con los ojos cerrados"
                    FocusArtState.BREAK -> "Taza de descanso ilustrada"
                },
                contentScale = ContentScale.Fit,
                alpha = .90f,
                modifier = Modifier.size(278.dp, 248.dp)
            )
        }
        Canvas(Modifier.size(292.dp, 260.dp)) {
            val lineY = size.height * .84f
            val startX = size.width * .22f
            val endX = startX + size.width * .56f * progress
            drawLine(progressTrack, Offset(startX, lineY), Offset(size.width * .78f, lineY), 7.dp.toPx(), StrokeCap.Round)
            drawLine(progressColor, Offset(startX, lineY), Offset(endX, lineY), 7.dp.toPx(), StrokeCap.Round)
            if (running && motionEnabled && phase == FocusPhase.FOCUS) {
                val accent = progressColor.copy(alpha = .28f + motionEnergy * .38f)
                val reach = (7.dp + 5.dp * motionEnergy).toPx()
                listOf(.31f, .43f, .55f).forEachIndexed { index, yFraction ->
                    val y = size.height * yFraction
                    drawLine(accent, Offset(size.width * .13f, y), Offset(size.width * .13f - reach, y - (index - 1) * 3.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(accent, Offset(size.width * .87f, y), Offset(size.width * .87f + reach, y + (index - 1) * 3.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                }
            }
            if (running && motionEnabled && phase == FocusPhase.BREAK) {
                val steam = progressColor.copy(alpha = .22f + motionEnergy * .34f)
                val lift = (motionEnergy * 7.dp.toPx())
                listOf(.43f, .50f, .57f).forEachIndexed { index, xFraction ->
                    drawArc(
                        color = steam,
                        startAngle = 145f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(size.width * xFraction - 8.dp.toPx(), size.height * .10f - lift - index * 2.dp.toPx()),
                        size = Size(16.dp.toPx(), 30.dp.toPx()),
                        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 22.dp)) {
            Text(
                when { paused -> "PAUSA"; phase == FocusPhase.FOCUS -> "ENFOQUE"; else -> "DESCANSO" },
                color = if (phase == FocusPhase.FOCUS) Coral else Leaf,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Surface(color = Paper.copy(alpha = .94f), shape = RoundedCornerShape(10.dp, 18.dp, 12.dp, 16.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                Text("%02d:%02d".format(remaining/60,remaining%60), fontSize=46.sp, fontWeight=FontWeight.Black, color=Ink, modifier=Modifier.padding(horizontal = 18.dp, vertical = 2.dp))
            }
            Text(contextMessage, color=MutedInk, fontSize=13.sp, maxLines=2, overflow=TextOverflow.Ellipsis, textAlign=androidx.compose.ui.text.style.TextAlign.Center, modifier=Modifier.width(190.dp).padding(top=4.dp))
        }
    }
}

@Composable
private fun MinimalTimer(
    remaining: Int,
    total: Int,
    phase: FocusPhase,
    taskTitle: String?,
    running: Boolean
) {
    val paused = !running && remaining in 1 until total
    val progress = elapsedTimerProgress(remaining, total)
    val progressInk = Ink
    val phaseLabel = when {
        paused -> "PAUSA"
        phase == FocusPhase.FOCUS -> "ENFOQUE"
        else -> "DESCANSO"
    }
    val context = when {
        paused && phase == FocusPhase.FOCUS -> "Tu progreso está guardado"
        paused -> "Descanso en pausa"
        phase == FocusPhase.BREAK -> "Respira y descansa"
        taskTitle != null -> taskTitle
        running -> "Un trazo a la vez"
        else -> "Listo para empezar"
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(phaseLabel, color = Ink, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
        Text(
            "%02d:%02d".format(remaining / 60, remaining % 60),
            color = Ink,
            fontSize = 58.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            context,
            color = MutedInk,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp).padding(top = 8.dp)
        )
        Canvas(Modifier.fillMaxWidth().padding(top = 28.dp).height(4.dp)) {
            drawLine(progressInk.copy(alpha = .14f), Offset(0f, center.y), Offset(size.width, center.y), 2.dp.toPx(), StrokeCap.Square)
            drawLine(progressInk, Offset(0f, center.y), Offset(size.width * progress, center.y), 2.dp.toPx(), StrokeCap.Square)
        }
    }
}

@Composable private fun TimerControls(running:Boolean,phase:FocusPhase,onStartPause:()->Unit,onReset:()->Unit,onSkip:()->Unit){ Row(Modifier.fillMaxWidth().padding(horizontal=24.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){ TextButton(onClick=onReset,modifier=Modifier.weight(1f)){Text("Reiniciar",color=MutedInk)}; Button(onClick=onStartPause,modifier=Modifier.weight(1.5f).height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=if(phase==FocusPhase.FOCUS)Coral else Leaf),shape=RoundedCornerShape(16.dp)){Text(if(running)"Pausar" else "Comenzar",color=Color.White,fontWeight=FontWeight.Bold)}; TextButton(onClick=onSkip,modifier=Modifier.weight(1f)){Text("Saltar",color=MutedInk)} } }

@Composable
private fun Presets(selected: Int, onSelect: (Int, Int) -> Unit, onCustom: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(25 to 5, 50 to 10).forEach { (focus, rest) ->
            Surface(
                onClick = { onSelect(focus, rest) },
                color = if (selected == focus) Mustard.copy(alpha = .28f) else Ink.copy(alpha = .04f),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "$focus / $rest",
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 13.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    color = if (selected == focus) Ink else MutedInk
                )
            }
        }
        Surface(
            onClick = onCustom,
            color = Leaf.copy(alpha = .09f),
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier.weight(1.1f)
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TrazoIcon(TrazoIconKind.EDIT, color = Leaf, size = 16.dp)
                Text("Ajustar", modifier = Modifier.padding(start = 5.dp), fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Bold, color = Leaf)
            }
        }
    }
}

@Composable
private fun FocusTask(task: Task, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        color = if (selected) Coral.copy(alpha = .18f) else PaperRaised,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).fillMaxWidth().clickable(onClick = onSelect)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).background(if (selected) Coral else Ink.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) {
                if (selected) TrazoIcon(TrazoIconKind.CHECK, color = Color.White, size = 13.dp)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(task.title, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                task.dueDate?.let { Text("${it.dayOfMonth}/${it.monthValue}", color = MutedInk, fontSize = 12.sp) }
            }
        }
    }
}
