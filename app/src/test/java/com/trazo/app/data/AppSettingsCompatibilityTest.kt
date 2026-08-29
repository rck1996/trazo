package com.trazo.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppSettingsCompatibilityTest {
    @Test
    fun legacyInstallGetsSafeRestructureDefaults() {
        val settings = AppSettings()

        assertEquals(TodayLayout.BALANCED, settings.todayLayout)
        assertFalse(settings.onboardingCompleted)
        assertFalse(settings.seenRestructureTour)
        assertFalse(settings.taskAdvancedExpanded)
        assertFalse(settings.habitAdvancedExpanded)
    }

    @Test
    fun todayLayoutsRemainStableForPersistence() {
        assertEquals(listOf("FOCUS", "BALANCED", "OVERVIEW"), TodayLayout.entries.map { it.name })
    }
}
