package com.trazo.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.trazo.app.data.LocalStore
import com.trazo.app.data.SmartCaptureParser
import com.trazo.app.data.SmartCaptureResult
import com.trazo.app.model.Habit
import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import java.time.DayOfWeek

/** Voice-only entry point used by the launcher widget. No recording is retained. */
class VoiceCaptureActivity : ComponentActivity() {
    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val phrase = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && !phrase.isNullOrBlank()) savePhrase(phrase)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CL")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "¿Qué quieres capturar?")
        }
        runCatching { voiceLauncher.launch(intent) }.onFailure {
            Toast.makeText(this, "No hay reconocimiento de voz disponible", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun savePhrase(phrase: String) {
        val store = LocalStore(this)
        val state = store.load()
        when (val result = SmartCaptureParser.parse(phrase)) {
            is SmartCaptureResult.TaskDraft -> {
                val input = result.input
                store.save(state.copy(tasks = listOf(Task(
                    title = input.title,
                    note = input.note,
                    priority = if (input.important) TaskPriority.IMPORTANT else TaskPriority.CALM,
                    dueDate = input.dueDate,
                    reminderHour = input.reminderHour,
                    reminderMinute = input.reminderMinute,
                    tags = input.tags
                )) + state.tasks))
                Toast.makeText(this, "Tarea guardada: ${input.title}", Toast.LENGTH_LONG).show()
            }
            is SmartCaptureResult.HabitDraft -> {
                val input = result.input
                store.save(state.copy(habits = state.habits + Habit(
                    title = input.title,
                    emoji = input.emoji,
                    category = input.category,
                    activeDays = input.days,
                    target = input.target,
                    unit = input.unit,
                    reminderHour = input.reminderHour,
                    reminderMinute = input.reminderMinute,
                    tags = input.tags
                )))
                val dayLabels = mapOf(
                    DayOfWeek.MONDAY to "L",
                    DayOfWeek.TUESDAY to "M",
                    DayOfWeek.WEDNESDAY to "X",
                    DayOfWeek.THURSDAY to "J",
                    DayOfWeek.FRIDAY to "V",
                    DayOfWeek.SATURDAY to "S",
                    DayOfWeek.SUNDAY to "D"
                )
                val days = input.days.sortedBy { it.value }.joinToString("·") { dayLabels.getValue(it) }
                Toast.makeText(this, "Hábito guardado: ${input.title} · $days", Toast.LENGTH_LONG).show()
            }
        }
    }
}
