package com.trazo.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trazo.app.ui.Coral
import com.trazo.app.ui.Ink
import com.trazo.app.ui.Leaf
import com.trazo.app.ui.MutedInk
import com.trazo.app.ui.Paper
import com.trazo.app.ui.Sky
import com.trazo.app.ui.TrazoTheme

class WidgetConfigurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val initial = WidgetPreferences.load(this, widgetId)
        setContent {
            TrazoTheme {
                WidgetConfigurationScreen(initial) { config ->
                    WidgetPreferences.save(this, widgetId, config)
                    TrazoWidget.updateOne(this, widgetId)
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    )
                    finish()
                }
            }
        }
    }
}

@Composable
private fun WidgetConfigurationScreen(initial: WidgetConfig, onSave: (WidgetConfig) -> Unit) {
    var first by remember { mutableStateOf(initial.firstSection) }
    var showTasks by remember { mutableStateOf(initial.showTasks) }
    var showHabits by remember { mutableStateOf(initial.showHabits) }
    var focusMinutes by remember { mutableStateOf(initial.focusMinutes) }
    var palette by remember { mutableStateOf(initial.palette) }
    var maxItems by remember { mutableStateOf(initial.maxItems) }

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState())
            .navigationBarsPadding().padding(horizontal = 22.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Tu widget, a tu manera", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
        Text(
            "Trazo se adapta automáticamente al espacio. Aquí eliges qué debe priorizar.",
            color = MutedInk
        )
        ConfigCard("Secciones visibles") {
            CheckLine("Tareas", showTasks) {
                if (!it && !showHabits) showHabits = true
                showTasks = it
            }
            CheckLine("Hábitos", showHabits) {
                if (!it && !showTasks) showTasks = true
                showHabits = it
            }
        }
        ConfigCard("Primero en el espacio compacto") {
            ChoiceRow(
                listOf(WidgetSection.TASKS to "Tareas", WidgetSection.HABITS to "Hábitos"),
                first,
                onSelect = { first = it }
            )
        }
        ConfigCard("Pomodoro del widget") {
            ChoiceRow(listOf(15 to "15", 25 to "25", 45 to "45", 60 to "60"), focusMinutes) {
                focusMinutes = it
            }
        }
        ConfigCard("Cantidad máxima") {
            ChoiceRow(listOf(2 to "2", 4 to "4", 6 to "6"), maxItems) { maxItems = it }
        }
        ConfigCard("Tinta principal") {
            ChoiceRow(
                listOf(
                    WidgetPalette.CORAL to "Coral",
                    WidgetPalette.BOTANICAL to "Hoja",
                    WidgetPalette.INK to "Tinta"
                ),
                palette,
                onSelect = { palette = it }
            )
        }
        Spacer(Modifier.height(2.dp))
        Button(
            onClick = {
                onSave(WidgetConfig(first, showTasks, showHabits, focusMinutes, palette, maxItems))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Coral),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text("Guardar widget", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ConfigCard(title: String, content: @Composable () -> Unit) {
    Surface(color = Color.White.copy(alpha = .64f), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Ink)
            content()
        }
    }
}

@Composable
private fun CheckLine(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, color = Ink)
    }
}

@Composable
private fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { (value, label) ->
            Surface(
                onClick = { onSelect(value) },
                color = if (selected == value) when (value) {
                    WidgetPalette.BOTANICAL -> Leaf
                    WidgetPalette.INK -> Ink
                    else -> Coral
                } else Sky.copy(alpha = .10f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    label,
                    color = if (selected == value) Color.White else MutedInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
