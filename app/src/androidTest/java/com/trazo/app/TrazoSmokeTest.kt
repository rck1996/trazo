package com.trazo.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Device-level smoke coverage for the two most important entry points. */
@RunWith(AndroidJUnit4::class)
class TrazoSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun dismissOnboardingWhenNeeded() {
        rule.waitForIdle()
        if (rule.onAllNodesWithText("Omitir").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Omitir").performClick()
        }
    }

    @Test
    fun opensFocusFromToday() {
        rule.onNodeWithText("TU ESTUDIO").assertIsDisplayed()
        rule.onNodeWithText("Enfocar").performClick()
        rule.onNodeWithText("MODO ENFOQUE").assertIsDisplayed()
    }

    @Test
    fun calendarAndItsThreeViewsAreDiscoverable() {
        rule.onNodeWithText("Calendario").performClick()
        rule.onNodeWithText("AGENDA VIVA").assertIsDisplayed()
        rule.onNodeWithText("Agenda diaria").assertIsDisplayed()
        rule.onNodeWithText("Semana").assertIsDisplayed()
        rule.onNodeWithText("Mes").assertIsDisplayed()
    }

    @Test
    fun monthShowsLoadMapAndSelectedDaySummary() {
        rule.onNodeWithText("Calendario").performClick()
        rule.onNodeWithText("Mes").performClick()
        rule.onNodeWithText("CARGA POR TIEMPO").assertIsDisplayed()
        rule.onNodeWithText("Abrir →").assertIsDisplayed()
        rule.onNodeWithText("subtareas").assertIsDisplayed()
    }

    @Test
    fun subtaskDependenciesAreEditableFromToday() {
        rule.onNodeWithText("Tareas").performClick()
        rule.onNodeWithContentDescription("Crear elemento").performClick()
        rule.onNodeWithText("Nueva tarea").assertIsDisplayed()
        rule.onNodeWithText("¿Qué quieres hacer?").performTextInput("Preparar informe")
        rule.onNodeWithText("Más opciones ↓").performClick()
        rule.onNodeWithText("Subtareas (una por línea)")
            .performTextInput("Buscar datos\nRedactar resumen")
        rule.onNodeWithText("↳ Sin dependencia").assertExists()
    }
}
