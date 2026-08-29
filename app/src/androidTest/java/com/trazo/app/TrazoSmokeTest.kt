package com.trazo.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Device-level smoke coverage for the two most important entry points. */
@RunWith(AndroidJUnit4::class)
class TrazoSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensFocusFromToday() {
        rule.onNodeWithText("TU ESTUDIO").assertIsDisplayed()
        rule.onNodeWithText("Enfocar").performClick()
        rule.onNodeWithText("MODO ENFOQUE").assertIsDisplayed()
    }
}
